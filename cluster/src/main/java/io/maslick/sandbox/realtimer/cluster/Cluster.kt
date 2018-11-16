package io.maslick.sandbox.realtimer.cluster

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.ignite.IgniteClusterManager
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder
import java.net.Inet4Address
import java.net.NetworkInterface

class Cluster(val verticles: List<Verticle>) {

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

    fun deploy(vert: Vertx) {
        vert.eventBus()
                .registerDefaultCodec(Data::class.java, DataMessageCodec())
                .registerDefaultCodec(Event::class.java, EventMessageCodec())
        verticles.forEach(vert::deployVerticle)
    }

    fun run() {
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
}