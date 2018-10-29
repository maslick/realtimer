package io.maslick.sandbox.realtimer.tracker

import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.DataMessageCodec
import io.maslick.sandbox.realtimer.data.Event
import io.maslick.sandbox.realtimer.data.EventMessageCodec
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class IntegrationTest {

    lateinit var vertx: Vertx
    val port = 8080

    @Before
    fun setUp(context: TestContext) {
        vertx = Vertx.vertx()
        vertx.deployVerticle(HttpServerVert(), context.asyncAssertSuccess())
        vertx.deployVerticle(RouterVert(FakeRepo(), EventBusPropagator(vertx.eventBus())), context.asyncAssertSuccess())
        vertx.eventBus()
                .registerDefaultCodec(Data::class.java, DataMessageCodec())
                .registerDefaultCodec(Event::class.java, EventMessageCodec())
    }

    @After
    fun tearDown(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testMyHttp(context: TestContext) {
        val async = context.async()

        vertx.createHttpClient().getNow(port, "localhost", "/maslick?data=hello") { response ->
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                context.assertTrue(body.toString().contains("ok"))
                context.assertFalse(body.toString().contains("false"))
            }

            vertx.eventBus().consumer<Event>("/propagator") { message ->
                val id = message.body().accountId
                val data = message.body().data
                println("id: $id, data: $data")
                context.assertEquals("maslick", id)
                context.assertEquals("hello", data)
                async.complete()
            }
        }

        async.awaitSuccess()
    }
}