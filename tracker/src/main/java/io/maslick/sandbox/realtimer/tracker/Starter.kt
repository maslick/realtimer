package io.maslick.sandbox.realtimer.tracker

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.hazelcast.config.Config
import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
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
        eventBus.publish("/propagator", Json.encode(Event(message.accountId, message.data, System.currentTimeMillis())))
    }
}

fun main(args: Array<String>) {
    println("Start app")
    Json.mapper.registerModule(KotlinModule())

    val hazelcastConfig = Config()
    hazelcastConfig.networkConfig.join.multicastConfig.isEnabled = false
    hazelcastConfig.networkConfig.join.tcpIpConfig.isEnabled = true
    hazelcastConfig.networkConfig.join.tcpIpConfig.addMember("127.0.0.1")

    val options = VertxOptions()
            .setClustered(true)
            .setClusterManager(HazelcastClusterManager(hazelcastConfig))

    Vertx.clusteredVertx(options) {
        if (it.succeeded()) {
            val vertx = it.result()
            vertx.deployVerticle(FakeDbVert(FakeRepo(), EventBusPropagator(vertx.eventBus())))
            vertx.deployVerticle(RouterVert())
            vertx.deployVerticle(HttpServerVert())
        }
        else println("Error creating a Vertx cluster")
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
                    vertx.eventBus().send("/router", Json.encode(Data(id, data[0])))
                    context.response().end("ok")
                }
        server.requestHandler(router::accept)
        server.listen(8080)
    }
}

class RouterVert : AbstractVerticle() {
    override fun start() {
        vertx.eventBus().consumer<String>("/router") { message ->
            val data = Json.decodeValue(message.body(), Data::class.java)
            vertx.eventBus().send("/db", message.body())
        }
    }
}

class FakeDbVert(val repo: Repo, val propagator: Propagator) : AbstractVerticle() {
    override fun start() {
        vertx.eventBus().consumer<String>("/db") { message ->
            val data = Json.decodeValue(message.body(), Data::class.java)
            vertx.executeBlocking<Boolean>({ it.complete(repo.accountIdIsValid(data.accountId)) }, false) { result ->
                if (result.succeeded()) propagator.propagate(data)
            }
        }
    }
}