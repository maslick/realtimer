package io.maslick.sandbox.realtimer.ws

import io.maslick.sandbox.realtimer.cluster.Cluster
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json

class WebsocketVert : AbstractVerticle() {
    override fun start() {
        vertx.createHttpServer().websocketHandler { wsServer ->
            println("new ws socket connected: ${wsServer.path()}")

            vertx.eventBus().consumer<Event>("/propagator") { message ->
                if ("/ws" == wsServer.path()) {
                    wsServer.writeFinalTextFrame(Json.encode(message.body()))
                    message.reply("ok")
                }
            }

            wsServer.endHandler {
                println("ws socket closed: ${wsServer.path()}")
            }
        }.listen(8081) {
            println("web socket server started: ${it.result().actualPort()}")
        }
    }
}

fun main(args: Array<String>) {
    Cluster(listOf(WebsocketVert())).run()
}