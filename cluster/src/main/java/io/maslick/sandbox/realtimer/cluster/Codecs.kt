package io.maslick.sandbox.realtimer.cluster

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.maslick.sandbox.realtimer.data.Data
import io.maslick.sandbox.realtimer.data.Event
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.eventbus.impl.codecs.StringMessageCodec
import io.vertx.core.json.Json

open class DataMessageCodec : MessageCodec<Data, Data> {

    init {
        Json.mapper.registerModule(KotlinModule())
    }

    private val stringCodec = StringMessageCodec()

    override fun decodeFromWire(pos: Int, buffer: Buffer?): Data {
        return Json.decodeValue(stringCodec.decodeFromWire(pos, buffer), Data::class.java)
    }

    override fun encodeToWire(buffer: Buffer?, s: Data?) {
        stringCodec.encodeToWire(buffer, Json.encode(s))
    }

    override fun transform(s: Data?) = s!!
    override fun name() = "mydata"
    override fun systemCodecID(): Byte = -1

}

open class EventMessageCodec : MessageCodec<Event, Event> {

    init {
        Json.mapper.registerModule(KotlinModule())
    }

    private val stringCodec = StringMessageCodec()

    override fun decodeFromWire(pos: Int, buffer: Buffer?): Event {
        return Json.decodeValue(stringCodec.decodeFromWire(pos, buffer), Event::class.java)
    }

    override fun encodeToWire(buffer: Buffer?, s: Event?) {
        stringCodec.encodeToWire(buffer, Json.encode(s))
    }

    override fun transform(s: Event?) = s!!
    override fun name() = "myevent"
    override fun systemCodecID(): Byte = -1
}