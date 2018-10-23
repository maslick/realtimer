package io.maslick.sandbox.realtimer.tracker

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class Integration {

    lateinit var vertx: Vertx
    var port = 8080

    @Before
    fun setUp(context: TestContext) {
        vertx = Vertx.vertx()
        val options = DeploymentOptions().setConfig(JsonObject().put("http.port", port))
        vertx.deployVerticle(HttpServerVert::class.java.name, options, context.asyncAssertSuccess())
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
                async.complete()
            }
        }
        async.awaitSuccess();
    }
}