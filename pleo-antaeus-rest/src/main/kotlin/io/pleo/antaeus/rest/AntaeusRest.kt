/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.apache.logging.log4j.kotlin.Logging
import org.joda.time.DateTime
import java.io.Serializable
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.fixedRateTimer;

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable, Logging {


    class BillingScheduler (private val name: String, private val billingService:BillingService, private val invoiceService: InvoiceService ) {
        private var schedulerActive:Boolean = false
        private var interval = 1000*60*60L // TODO: Externalize. (although every hour seems good.)
        private var billingTimer = start()

        private fun start(): Timer {
            stopScheduler() // just in case.... we would'nt want to timers running.
            // TODO: calculate next running time:
            val nextStart:Date = Date()

            billingTimer = fixedRateTimer(name    = name,
                                          daemon  = true,
                                          startAt = nextStart,
                                          period  = interval) {
                logger.info("billingScheduler - It's ALIVE! ")

                // this
                runBillingCycle()

                logger.info("billingScheduler - Going back to bed. ")
            }

            schedulerActive = true
            return billingTimer;
        }

        /**
         *
         */
        private fun runBillingCycle() {
            // step 1: collect underpants

            // fetch by UTC billing date has passed.
            val invoices = invoiceService.fetchBillable(DateTime())

            // reporting purposes
            val processedInvoices = mutableListOf<Invoice>()

            // step 2: ???
            invoices.forEach {
                val i = billingService.charge(it)
                processedInvoices.add(i)
            }

            // step 3: profit..... lets see how well we did.
            logger.info{ "Invoices to process by initial status: ${invoices.groupingBy { it.status }.eachCount()}. Total: ${invoices.size}" }
            logger.info{ "Processed invoices by status: ${processedInvoices.groupingBy { it.status }.eachCount()}. Total: ${processedInvoices.size}" }

            val aggregatedBefore = invoices.groupingBy { it.amount.currency.toString() }
                    .aggregate { key, accumulator: BigDecimal?, invoice:Invoice, first:Boolean ->
                        if(first) invoice.amount.value
                        else accumulator!!.add(invoice.amount.value)
                    }

            val aggregatedAfter = processedInvoices
                    .filter { it.status == InvoiceStatus.PAID }
                    .groupingBy { it.amount.currency.toString() }
                    .aggregate { key, accumulator: BigDecimal?, invoice:Invoice, first:Boolean ->
                        if(first) invoice.amount.value
                        else accumulator!!.add(invoice.amount.value)
                    }


            logger.info{ "Total accounts receivable: ${aggregatedBefore}"}
            logger.info{ "Total paid:                ${aggregatedAfter}"}

            val leftover = mutableMapOf<String, BigDecimal>()
            aggregatedBefore.forEach{
                leftover[it.key] = it.value?.minus((aggregatedAfter[it.key] ?: 0.toBigDecimal())) ?: 0.toBigDecimal()
            }

            logger.info{ "Left unpaid:               ${leftover}"}
        }


        fun startScheduler() {
            start();
        }


        fun stopScheduler() {
            schedulerActive = false
            billingTimer?.cancel()
        }

        fun toMap():Map<String,Serializable> {
            return mapOf(
                    "name" to name,
                    "interval" to interval,
                    "active" to schedulerActive
            )
        }

    }

    private var billingScheduler = BillingScheduler("billingScheduler", billingService, invoiceService)

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }



    init {  // TODO these endpoints must be secured, this might be externalized using something like https://istio.io sidecar
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("scheduler") {
                       // URL: /rest/v1/scheduler
                       get {
                       }

                       path("activate") {
                           // URL: /rest/v1/scheduler/activate
                           get {
                               billingScheduler.stopScheduler()
                               billingScheduler.startScheduler()
                               it.json(billingScheduler.toMap())
                           }
                       }

                       path("deactivate") {
                           // URL: /rest/v1/scheduler/deactivate
                           get {
                               billingScheduler.stopScheduler()
                               it.json(billingScheduler.toMap())
                           }
                       }
                   }

               }
           }
        }
    }
}
