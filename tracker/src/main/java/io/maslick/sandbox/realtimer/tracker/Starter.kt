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
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

interface Repo {
    fun accountIdIsValid(accountId: String): Boolean
}

interface Propagator {
    fun propagate(message: Data)
}

class FakeRepo : Repo {
    override fun accountIdIsValid(accountId: String): Boolean {
        println("blocking call")
        Thread.sleep(1200)
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


fun main(args: Array<String>) {
    println("Start app")

    val hazelcastConfig = Config()
    hazelcastConfig.networkConfig.join.multicastConfig.isEnabled = false
    hazelcastConfig.networkConfig.join.tcpIpConfig.isEnabled = true
    hazelcastConfig.networkConfig.join.tcpIpConfig.addMember("127.0.0.1")

    val options = VertxOptions()
            .setClustered(true)
            .setClusterManager(HazelcastClusterManager(hazelcastConfig))
            .setWorkerPoolSize(200)

    Vertx.clusteredVertx(options) {
        if (it.succeeded()) {
            val vertx = it.result()
            vertx.deployVerticle(RouterVert(FakeRepo(), EventBusPropagator(vertx.eventBus())))
            vertx.deployVerticle(HttpServerVert())

            vertx.eventBus()
                    .registerDefaultCodec(Data::class.java, DataMessageCodec())
                    .registerDefaultCodec(Event::class.java, EventMessageCodec())
        }

        else println("Error creating a Vertx cluster")
    }
}