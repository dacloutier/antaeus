
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = randomCurrency(),
            nextBillingStartDate = randomBillingDate()
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = randomIncorrectCurrency(customer.currency)
                ),
                customer = customer,
                status = InvoiceStatus.values().random()
            )
        }
    }
}

internal fun randomBillingDate(): DateTime {
 return DateTime.now()
         .withHourOfDay(0)
         .withMinuteOfHour(1)
         .withSecondOfMinute(0)
         .withDayOfMonth(1)
         .plusMonths(Random.nextInt(0,1))
         .withZoneRetainFields(getRandomZone())
}

internal fun getRandomZone(): DateTimeZone {
    return DateTimeZone.forID(DateTimeZone.getAvailableIDs().random())

}

internal fun randomCurrency():Currency {
    return Currency.values()[Random.nextInt(0, Currency.values().size)];
}

/**
 * 1 in 20 is always a botched roll!
 */
internal fun randomIncorrectCurrency(customerCurrency: Currency): Currency {

    if (Random.nextInt(1, 20) == 1) return randomCurrency()

    return customerCurrency
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}