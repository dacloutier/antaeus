package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    INVALID_CUSTOMER,
    INSUFICIENT_FUNDS_ERROR,
    UNSUPPORTED_CURRENCY_ERROR,
    SYSTEM_ERROR,
    PAID,
    PROCESSING,
    INVOICE_NOT_FOUND
}
