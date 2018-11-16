package io.maslick.sandbox.realtimer.mongo

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

class MongoVerticle : AbstractVerticle() {
    private lateinit var client: MongoClient


    fun initDb() {
        val user1 = JsonObject()
                .put("accountId", "maslick")
                .put("accountName", "Pavel Maslov")
                .put("isActive", true)

        val user2 = JsonObject()
                .put("accountId", "testUserId")
                .put("accountName", "Test User")
                .put("isActive", false)

        client.insert("account", user1) { println("mongo populated with user1") }
        client.insert("account", user2) { println("mongo populated with user2") }
    }

    override fun start() {
        println("Starting mongodb verticle..")
        val uri = "mongodb://mongo:27017"
        client = MongoClient.createShared(vertx, JsonObject()
                .put("connection_string", uri)
                .put("db_name", "myDb"))

        initDb()

        vertx.eventBus().consumer<String>("/isUserValid") { message ->
            client.find("account", JsonObject().put("accountId", message.body())) {
                println("user ${message.body()} is in db: ${it.result().size > 0}")
                message.reply(it.result().size > 0)
            }
        }
    }
}