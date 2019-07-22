/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchByStatus(vararg statuses:InvoiceStatus): List<Invoice> {
        return dal.fetchInvoiceByStatus(*statuses)
    }

    fun fetchBillable(scheduledBillingDate : DateTime?): List<Invoice> {
        return dal.fetchBillableInvoices(scheduledBillingDate);
    }

    fun save(invoice: Invoice){
        dal.updateInvoice(invoice)
    }
}
