package io.maslick.sandbox.realtimer.tracker

import com.hazelcast.config.Config
import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.DataMessageCodec
import io.maslick.sandbox.realtimer.data.Event
import io.maslick.sandbox.realtimer.data.EventMessageCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.util.*

interface Repo {
    fun accountIdIsValid(accountId: String): Boolean
}

interface Propagator {
    fun propagate(message: Data)
}

class FakeRepo : Repo {
    private fun Pair<Int, Int>.random(): Long {
        return this.first + Random().nextInt(this.second - this.first + 1).toLong()
    }

    override fun accountIdIsValid(accountId: String): Boolean {
        println("blocking call")
        Thread.sleep((400 to 500).random())
        return true
    }
}

class EventBusPropagator(private val eventBus: EventBus) : Propagator {
    override fun propagate(message: Data) {
        eventBus.publish("/propagator", Event(message.accountId, message.data, System.currentTimeMillis()))
    }
}

class HttpServerVert : AbstractVerticle() {
    override fun start() {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.get("/:id")
                .handler { context ->
                    val id = context.request().getParam("id")
                    val data = context.queryParam("data")
                    vertx.eventBus().send("/router", Data(id, data[0]))
                    context.response().end("ok")
                }
        server.requestHandler(router::accept)
        server.listen(8080)
    }
}

class RouterVert(val repo: Repo, val propagator: Propagator) : AbstractVerticle() {
    override fun start() {
        vertx.eventBus().consumer<Data>("/router") { message ->
            val data = message.body()
            vertx.executeBlocking<Boolean>({ it.complete(repo.accountIdIsValid(data.accountId)) }, false) { result ->
                if (result.succeeded()) propagator.propagate(data)
            }
        }
    }
}

class WebsocketVert : AbstractVerticle() {
    override fun start() {
        vertx.createHttpServer().websocketHandler { wsServer ->
            println("new ws socket connected: ${wsServer.path()}")

            vertx.eventBus().consumer<Event>("/propagator") { message ->
                if (message.body().accountId == wsServer.path().split("/")[1]) {
                    wsServer.writeFinalTextFrame(Json.encode(message.body()))
                    message.reply("ok")
                }
            }

            wsServer.endHandler {
                println("ws socket closed: ${wsServer.path()}")
            }
        }.listen(8081)
    }
}

fun main(args: Array<String>) {
    println("Start app")

    val options = VertxOptions()
            .setClustered(true)
            .setClusterManager(HazelcastClusterManager(Config()))
            .setWorkerPoolSize(200)

    Vertx.clusteredVertx(options) {
        if (it.succeeded()) {
            val vertx = it.result()
            vertx.deployVerticle(RouterVert(FakeRepo(), EventBusPropagator(vertx.eventBus())))
            vertx.deployVerticle(HttpServerVert())
            vertx.deployVerticle(WebsocketVert())

            vertx.eventBus()
                    .registerDefaultCodec(Data::class.java, DataMessageCodec())
                    .registerDefaultCodec(Event::class.java, EventMessageCodec())
        }

        else println("Error creating a Vertx cluster")
    }
}