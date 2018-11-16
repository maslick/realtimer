package io.maslick.sandbox.realtimer.tracker

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler


class HttpServerVert : AbstractVerticle() {
    override fun start() {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.get("/:id")
                .handler { context ->
                    val id = context.request().getParam("id")
                    val data = context.queryParam("data")
                    println("{user: $id, data: $data, ts: ${System.currentTimeMillis()}}")
                    vertx.eventBus().send("/router", Data(id, data[0]))
                    context.response().end("ok")
                }
        server.requestHandler(router::accept)
        server.listen(8080)
    }
}

class RouterVert : AbstractVerticle() {
    override fun start() {
        vertx.eventBus().consumer<Data>("/router") { message ->
            val data = message.body()
            vertx.eventBus().send<Boolean>("/isUserValid", data.accountId, DeliveryOptions().setSendTimeout(5000)) {
                if (it.succeeded() && it.result().body())
                    vertx.eventBus().publish("/propagator", Event(data.accountId, data.data, System.currentTimeMillis()))
            }
        }
    }
}