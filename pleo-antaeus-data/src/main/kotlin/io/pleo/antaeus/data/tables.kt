/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status")
    val scheduledPayment = datetime("scheduled_payment").nullable()
    val lastPaymentAttempt = datetime("last_payment_attempt").nullable()
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val nextBillingStartDate = datetime("next_billing_start") // ASSUMPTION: The entry system will make sure the customers local date time for the next requested billing is entered un UTC.
}
