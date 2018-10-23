package io.maslick.sandbox.realtimer.cli

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.hazelcast.config.Config
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.Json
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

fun main(args: Array<String>) {
    println("Cli app")
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
            vertx.eventBus().consumer<String>("/propagator") { message ->
                val event = Json.decodeValue(message.body(), Event::class.java)
                println("/propagator: ${event.data} -> ${event.timestamp}")
            }
        }
        else println("Error creating a Vertx cluster")
    }

    Thread.sleep(1000000)
}