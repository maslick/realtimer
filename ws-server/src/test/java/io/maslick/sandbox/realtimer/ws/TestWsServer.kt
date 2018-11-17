package io.maslick.sandbox.realtimer.ws

import io.maslick.sandbox.realtimer.cluster.DataMessageCodec
import io.maslick.sandbox.realtimer.cluster.EventMessageCodec
import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class TestWsServer {

    lateinit var vertx: Vertx
    val wsPort = 8081

    @Before
    fun setUp(context: TestContext) {
        vertx = Vertx.vertx()

        vertx.eventBus()
                .registerDefaultCodec(Data::class.java, DataMessageCodec())
                .registerDefaultCodec(Event::class.java, EventMessageCodec())

        vertx.deployVerticle(WebsocketVert(), context.asyncAssertSuccess())
    }

    @After
    fun tearDown(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testWebSocketVerticle(context: TestContext) {
        val async = context.async()

        // create ws client
        vertx.createHttpClient().websocket(wsPort, "localhost", "/ws") { response ->
            println("connected: ${response.remoteAddress()}")

            // mock tracker (receive an http call, go to mongo and check if user is there)
            vertx.eventBus().publish("/propagator", Event("fakeUser", "helloworld", System.currentTimeMillis()))

            response.textMessageHandler { body ->
                val data = Json.decodeValue(body, Event::class.java)
                context.assertEquals("fakeUser", data.accountId)
                context.assertEquals("helloworld", data.data)
                async.complete()
            }
        }

        async.awaitSuccess()
    }
}