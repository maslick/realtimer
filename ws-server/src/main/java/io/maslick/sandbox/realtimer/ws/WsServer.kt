package io.maslick.sandbox.realtimer.ws

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.Json
import io.vertx.spi.cluster.ignite.IgniteClusterManager
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder
import java.net.Inet4Address
import java.net.NetworkInterface

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