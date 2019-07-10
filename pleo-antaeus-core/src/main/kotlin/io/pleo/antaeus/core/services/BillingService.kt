package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.apache.logging.log4j.kotlin.Logging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
) : Logging {


    // TODO - billing and invoice status saving should be transactionnal.
    fun charge(i: Invoice): Invoice {
        logger.info { "Start charging of invoice ${i}" } // TODO data in this object might be sensitive ond be obfuscated from the logs ?
        var invoice = i
        var customer = Customer(id = i.customerId, currency = i.amount.currency) // initialized in case the fetch later fails.

        try {
            invoice = invoiceService.fetch(i.id) // make sure the invoice actually exists
            customer = customerService.fetch(i.customerId) // make sur ethe customer exists

            if (customer.currency != invoice.amount.currency) throw CurrencyMismatchException(invoice.id, customer.id) // make sure the currencies aren't mismatched

            // TODO In the meentime, save a "inprogress status" any stuck invoices in progress should raise alarms
            invoice.status = InvoiceStatus.PROCESSING
            invoiceService.save(invoice) // if this fails then at least we wont bill the customer.

            //  actually process the charging
            if (paymentProvider.charge(invoice)) {
                invoice.status = InvoiceStatus.PAID
            } else {
                invoice.status = InvoiceStatus.INSUFICIENT_FUNDS_ERROR
            }

            // TODO end TX
        } catch (e: CustomerNotFoundException) {
            logger.error { "Customer ${invoice.customerId} of invoice #${i.id} is not found." }
            invoice.status = InvoiceStatus.INVALID_CUSTOMER
        } catch (e: CurrencyMismatchException) {
            logger.error { "Currency(${customer.currency}) of customer ${i.customerId} does not match the currency(${invoice.amount.currency}) of invoice #${i.id}" }
            invoice.status = InvoiceStatus.UNSUPPORTED_CURRENCY_ERROR
        } catch (e: InvoiceNotFoundException) {
            logger.error { "Invoice #${i.id} is not found." }
            invoice.status = InvoiceStatus.INVOICE_NOT_FOUND
        } catch (e: NetworkException) {
            logger.error { "A network error occured while processing Invoice #${i.id}." }
            invoice.status = InvoiceStatus.SYSTEM_ERROR
        } finally {
            try {
                invoiceService.save(invoice)
            } catch (e: Exception) {
                // TODO a programmers worst nightmare! Client might have been billed, but the database failed!
                logger.error { "Exception occured saving the invoice state for ${invoice.toString()}" }
                if (invoice.status == InvoiceStatus.PAID) {
                    // TODO some log monitoring will trigger a support email (datadog?)
                    logger.fatal { "RAISE HELL! A customer was billed but we didn't save the status for ${invoice.toString()}" }
                }

                throw e;
            }
        }

        return invoice;
    }
}