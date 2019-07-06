package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BillingServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(1) } returns Customer(id = 1, currency = Currency.DKK)
        every { fetchInvoice(1) } returns Invoice(id = 1, customerId = 1, amount = Money(value=1.toBigDecimal(), currency = Currency.DKK), status = InvoiceStatus.PENDING)
    }

    private val customerService = CustomerService(dal = dal)
    private val invoiceService = InvoiceService(dal = dal)
    private val billingService = BillingService(paymentProvider = getPaymentProvider(), customerService = customerService, invoiceService = invoiceService)

    @Test
    fun `invoice status will be PAID`() {

        val invoice = Invoice(id = 1, customerId = 1, amount = Money(value = 1.toBigDecimal(), currency = Currency.DKK), status = InvoiceStatus.PENDING)
        Assertions.assertEquals(InvoiceStatus.PAID, billingService.charge(invoice).status) {
            "Invoice is not paid"
        }
    }

    private fun getPaymentProvider(): PaymentProvider {
        return object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.amount.value.toInt() < 100
            }
        }
    }
}