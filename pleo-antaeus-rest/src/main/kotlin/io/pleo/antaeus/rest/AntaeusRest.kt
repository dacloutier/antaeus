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
import mu.KotlinLogging
import org.apache.logging.log4j.kotlin.Logging
import java.io.Serializable
import java.util.*
import kotlin.concurrent.fixedRateTimer;

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable, Logging {


    class BillingScheduler (private val name: String) {
        private var schedulerActive:Boolean = false
        private var interval = 1000*60*60L
        private var billingTimer = start()

        private fun start(): Timer {
            schedulerActive = true
            billingTimer = fixedRateTimer(name = name,
                    daemon = true,
                    startAt = Date(),
                    period = interval) {
                logger.info("billingScheduler - woohoo! ")
                // TODO Call real transactional service here!


                // TODO
            }
            return billingTimer;
        }

        fun isActive():Boolean {return schedulerActive }

        fun startScheduler() {
            start();
        }


        fun stopScheduler() {
            schedulerActive = false
            billingTimer.cancel()
        }

        fun toMap():Map<String,Serializable> {
            return mapOf(
                    "name" to name,
                    "interval" to interval,
                    "active" to schedulerActive
            )
        }

    }

    private var billingScheduler = BillingScheduler("billingScheduler")

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



    init {  // TODO these endpoints must be secured
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
                           it.json(billingScheduler.toMap())
                       }

                       path("activate") {
                           // URL: /rest/v1/scheduler/activate
                           get {
                               // TODO this should be a post but it's easier to test like this. // TODO these endpoints must be secured
                               billingScheduler.stopScheduler()
                               billingScheduler.startScheduler()
                               it.json(billingScheduler.toMap())
                           }
                       }

                       path("deactivate") {
                           // URL: /rest/v1/scheduler/deactivate
                           get {
                               // TODO this should be a post but it's easier to test like this. // TODO these endpoints must be secured
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
