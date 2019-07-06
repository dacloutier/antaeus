package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.apache.logging.log4j.kotlin.Logging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
) : Logging {

    fun charge(i: Invoice): Invoice {
        logger.info {"Start charging of invoice ${i}"} // TODO data in this object might be sensitive ond be obfuscated from the logs ?

        var invoice = i
        var customer = Customer(id=i.customerId, currency = i.amount.currency) // initialized in case the fetch later fails.

        try {
            invoice = invoiceService.fetch(i.id) // make sure the invoice actually exists
            customer = customerService.fetch(i.customerId) // make sur ethe customer exists

            // TODO - billing and invoice status saving should be transactionnal.
            if (customer.currency != invoice.amount.currency) throw CurrencyMismatchException(invoice.id, customer.id) // make sure the currencies aren't mismatched

            //  actually process the charging
            if (paymentProvider.charge(invoice)) {
                invoice.status = InvoiceStatus.PAID
            } else {
                invoice.status = InvoiceStatus.INSUFICIENT_FUNDS_ERROR
            }
        } catch (e: CustomerNotFoundException) {
            logger.error {"Customer ${invoice.customerId} of invoice #${i.id} is not found." }
            invoice.status = InvoiceStatus.INVALID_CUSTOMER
        } catch (e: CurrencyMismatchException) {
            logger.error {"Currency(${customer.currency}) of customer ${i.customerId} does not match the currency(${invoice.amount.currency}) of invoice #${i.id}" }
            invoice.status = InvoiceStatus.UNSUPPORTED_CURRENCY_ERROR
        }  catch (e: InvoiceNotFoundException) {
            logger.error {"Invoice #${i.id} is not found." }
            invoice.status = InvoiceStatus.SYSTEM_ERROR // TODO - each exception here should have it's own status.
        } catch (e: NetworkException) {
            logger.error {"A network error occured while processing Invoice #${i.id}." }
            invoice.status = InvoiceStatus.SYSTEM_ERROR// TODO - each exception here should have it's own status.
        }


        return invoice; // TODO: invoiceService.save(invoice);

    }
}