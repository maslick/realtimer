package io.maslick.sandbox.realtimer.tracker

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.DataMessageCodec
import io.maslick.sandbox.realtimer.data.Event
import io.maslick.sandbox.realtimer.data.EventMessageCodec
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class IntegrationTest {

    lateinit var vertx: Vertx
    val httpPort = 8080
    val wsPort = 8081

    @Before
    fun setUp(context: TestContext) {
        vertx = Vertx.vertx()
        vertx.deployVerticle(HttpServerVert(), context.asyncAssertSuccess())
        vertx.deployVerticle(RouterVert(FakeRepo(), EventBusPropagator(vertx.eventBus())), context.asyncAssertSuccess())
        vertx.deployVerticle(WebsocketVert(), context.asyncAssertSuccess())
        vertx.eventBus()
                .registerDefaultCodec(Data::class.java, DataMessageCodec())
                .registerDefaultCodec(Event::class.java, EventMessageCodec())
    }

    @After
    fun tearDown(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testEventBusClient(context: TestContext) {
        val async = context.async()

        // create event bus client
        vertx.eventBus().consumer<Event>("/propagator") { message ->
            val id = message.body().accountId
            val data = message.body().data
            println("id: $id, data: $data")
            context.assertEquals("maslick", id)
            context.assertEquals("hello", data)
            async.complete()
        }

        // fire a dummy http get request
        vertx.createHttpClient().getNow(httpPort, "localhost", "/maslick?data=hello") { response ->
            context.assertEquals(200, response.statusCode())
            context.assertNotEquals(500, response.statusCode())
            response.bodyHandler { body ->
                context.assertTrue(body.toString().contains("ok"))
                context.assertFalse(body.toString().contains("false"))
            }
        }

        async.awaitSuccess()
    }

    @Test
    fun testWebSocketClient(context: TestContext) {
        val async = context.async()

        // create ws client
        vertx.createHttpClient().websocket(wsPort, "localhost", "/ws") { response ->
            println("connected: ${response.remoteAddress()}")

            response.textMessageHandler { body ->
                val data = Json.decodeValue(body, Event::class.java)
                context.assertEquals("fakeUser", data.accountId)
                context.assertEquals("helloworld", data.data)
                async.complete()
            }
        }

        // fire a dummy http get request
        vertx.createHttpClient().getNow(httpPort, "localhost", "/fakeUser?data=helloworld") { response ->
            context.assertEquals(200, response.statusCode())
            context.assertNotEquals(500, response.statusCode())
            response.bodyHandler { body ->
                context.assertTrue(body.toString().contains("ok"))
                context.assertFalse(body.toString().contains("false"))
            }
        }

        async.awaitSuccess()
    }
}