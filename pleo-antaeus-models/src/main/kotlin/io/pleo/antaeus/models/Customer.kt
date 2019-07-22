package io.pleo.antaeus.models

import org.joda.time.DateTime

data class Customer(
    val id: Int,
    val currency: Currency,
    var nextBillingStartDate: DateTime
)
