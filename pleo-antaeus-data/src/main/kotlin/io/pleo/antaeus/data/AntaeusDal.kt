/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime


class AntaeusDal(private val db: Database) {

    // new methods
    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = customer.id
                        it[this.scheduledPayment] = customer.nextBillingStartDate
                        // when inserting an invoice, it shouldnt have a "lastPaymentAttempt" so its null.
                    } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    /**
     * Function to update an invoice on the DB.
     * Only the attributes value, currency and status are updated, as the attributes id and customerId should never change
     */
    fun updateInvoice(invoice: Invoice){
        transaction(db){
            InvoiceTable.update({InvoiceTable.id eq invoice.id}){
                it[this.value] = invoice.amount.value
                it[this.currency] = invoice.amount.currency.toString()
                it[this.status] = invoice.status.toString()
                it[this.scheduledPayment] = invoice.scheduledPayment
                it[this.lastPaymentAttempt] = invoice.lastPaymentAttempt

            }
        }
    }


    fun fetchInvoiceByStatus(vararg statuses:InvoiceStatus):List<Invoice>{
        return transaction(db) {
            InvoiceTable.select{
                InvoiceTable.status.inList(statuses.map{it.toString()})
            }
                    .map { it.toInvoice() }
        }
    }


    fun fetchBillableInvoices():List<Invoice>{
        return transaction(db) {
            InvoiceTable.select{
                            InvoiceTable.status.eq(InvoiceStatus.SYSTEM_ERROR.toString()) or
                            InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) or
                            InvoiceTable.status.eq(InvoiceStatus.INSUFICIENT_FUNDS_ERROR.toString())
                    }
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency, nextBillingStartDate:DateTime): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
                it[this.nextBillingStartDate] = nextBillingStartDate
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }

    /**
     * Function to update a customer on the DB.
     */
    fun updateCustomer(customer: Customer ){
        transaction(db){
            CustomerTable.update({CustomerTable.id eq customer.id}){
                it[this.currency] = customer.currency.toString()
                it[this.nextBillingStartDate] = customer.nextBillingStartDate
            }
        }
    }
}
