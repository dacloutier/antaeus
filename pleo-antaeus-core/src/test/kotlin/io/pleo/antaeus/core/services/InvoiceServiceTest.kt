package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoiceByStatus( InvoiceStatus.SYSTEM_ERROR, InvoiceStatus.PENDING) } returns listOf(
                                                                                        Invoice(id = 1, status = InvoiceStatus.SYSTEM_ERROR, customerId = 1, amount = Money(1.toBigDecimal(), Currency.DKK)),
                                                                                        Invoice(id = 2, status = InvoiceStatus.PENDING, customerId = 2, amount = Money(2.toBigDecimal(), Currency.EUR) )
                                                                                    )
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return error or pending invoices only`() {

        val invoices:List<Invoice> = invoiceService.fetchByStatus(InvoiceStatus.SYSTEM_ERROR, InvoiceStatus.PENDING)
        Assertions.assertTrue(invoices.isNotEmpty()) {"List is empty"}

        invoices.forEach {
            Assertions.assertTrue(it.status == InvoiceStatus.SYSTEM_ERROR || it.status == InvoiceStatus.PENDING) {"Invalid status returned."}
        }

    }
}