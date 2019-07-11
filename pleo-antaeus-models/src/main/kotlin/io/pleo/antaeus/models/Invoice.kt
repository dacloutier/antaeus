package io.pleo.antaeus.models

import org.joda.time.DateTime

data class Invoice(
        val id: Int,
        val customerId: Int,
        val amount: Money,
        var status: InvoiceStatus,
        var scheduledPayment: DateTime?,
        var lastPaymentAttempt: DateTime?
)
