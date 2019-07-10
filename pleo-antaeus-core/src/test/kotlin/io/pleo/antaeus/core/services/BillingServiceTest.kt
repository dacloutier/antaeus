package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true) {
        every { fetchCustomer(1) } returns Customer(id = 1, currency = Currency.DKK)
        every { fetchInvoice(1) } returns Invoice(id = 1, customerId = 1, amount = Money(value=1.toBigDecimal(), currency = Currency.DKK), status = InvoiceStatus.PENDING)
        every { fetchInvoice(2) } returns Invoice(id = 2, customerId = 1, amount = Money(value=100000.toBigDecimal(), currency = Currency.DKK), status = InvoiceStatus.PENDING)
        every { fetchInvoice(-1) } returns Invoice(id = -1, customerId = 1, amount = Money(value=100000.toBigDecimal(), currency = Currency.DKK), status = InvoiceStatus.PENDING)
    }

    private val customerService = CustomerService(dal = dal)
    private val invoiceService = InvoiceService(dal = dal)
    private val billingService = BillingService(paymentProvider = getPaymentProvider(), customerService = customerService, invoiceService = invoiceService)

    @Test
    fun `invoice status will be PAID`() {

        val invoice = dal.fetchInvoice(1)!!
        Assertions.assertEquals(InvoiceStatus.PAID, billingService.charge(invoice)?.status) {
            "Invoice is not paid"
        }
    }

    @Test
    fun `invoice will have insuficient funds`(){
        val invoice = dal.fetchInvoice(2)!!
        Assertions.assertEquals(InvoiceStatus.INSUFICIENT_FUNDS_ERROR, billingService.charge(invoice)?.status) {
            "Invoice was paid!!!"
        }
    }

    @Test
    fun `invoice will have system error`(){
        val invoice = dal.fetchInvoice(-1)!!
        Assertions.assertEquals(InvoiceStatus.SYSTEM_ERROR, billingService.charge(invoice)?.status) {
            "Wait what? Should have received a system error"
        }
    }

    private fun getPaymentProvider(): PaymentProvider {
        return object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {

                if (invoice.id == -1) {
                    throw NetworkException()
                }

                return invoice.amount.value.toInt() < 100
            }
        }
    }
}