package io.maslick.sandbox.realtimer.tracker

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.spi.cluster.ignite.IgniteClusterManager
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder
import java.net.Inet4Address
import java.net.NetworkInterface
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
        Thread.sleep((100 to 2000).random())
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
                    println("{user: $id, data: $data}")
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

fun getIgniteConfig(): IgniteConfiguration? {
    val spi = TcpDiscoverySpi()
    val ipFinder = TcpDiscoveryKubernetesIpFinder()
    ipFinder.setServiceName("ignite-service")
    spi.ipFinder = ipFinder
    return IgniteConfiguration().setDiscoverySpi(spi)
}

fun getMyIp(): String {
    val localAddresses = arrayListOf<Inet4Address>()

    NetworkInterface.getNetworkInterfaces().toList().forEach { i: NetworkInterface ->
        i.inetAddresses.toList().forEach { addr ->
            if (!addr.isLinkLocalAddress && addr is Inet4Address)
                localAddresses.add(addr)
        }
    }

    val publicClusterHost = localAddresses.map(Inet4Address::getHostAddress).first()
    println("publicClusterHost: $publicClusterHost")

    return publicClusterHost
}

fun deploy(vertx: Vertx) {
    vertx.deployVerticle(RouterVert(FakeRepo(), EventBusPropagator(vertx.eventBus())))
    vertx.deployVerticle(HttpServerVert())
    vertx.deployVerticle(WebsocketVert())

    vertx.eventBus()
            .registerDefaultCodec(Data::class.java, DataMessageCodec())
            .registerDefaultCodec(Event::class.java, EventMessageCodec())
}

fun setupCluster() {
    val options = VertxOptions()
            .setClustered(true)
            .setClusterManager(IgniteClusterManager(getIgniteConfig()))
            .setClusterHost(getMyIp())


    Vertx.clusteredVertx(options) {
        if (it.succeeded())
            deploy(it.result())
        else {
            println("Error creating a Vertx cluster!")
            System.exit(1);
        }

    }
}

fun main(args: Array<String>) {
    println("Start app")
    setupCluster()
}