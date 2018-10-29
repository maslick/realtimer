package io.maslick.sandbox.realtimer.tracker

import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class DummyTest {

    @Test
    @Ignore
    fun testHttp(context: TestContext) {
        val async = context.async()

        Vertx.vertx().createHttpClient().getNow(8080, "localhost", "/maslick?data=hello") { response ->
            context.assertEquals(200, response.statusCode())
            context.assertNotEquals(500, response.statusCode())
            response.bodyHandler { body ->
                context.assertTrue(body.toString().contains("ok"))
                context.assertFalse(body.toString().contains("false"))
                async.complete()
            }
        }

        async.awaitSuccess()
    }
}