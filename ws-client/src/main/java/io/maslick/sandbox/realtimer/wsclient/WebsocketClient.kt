package io.maslick.sandbox.realtimer.wsclient

import com.google.gson.Gson
import io.maslick.sandbox.realtimer.data.Event
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


fun main(args: Array<String>) {
    val addr = System.getProperty("address", "ws://localhost:8081")
    val userId = System.getProperty("userId", null)

    if (userId == null) {
        println("Error: userId unset!..")
        return
    }

    wsObservable("$addr/$userId")
            .subscribe {
                println("${it.timestamp.formatDate()} message from ${it.accountId} : ${it.data}")
            }
}

fun wsObservable(serverUri: String): Flowable<Event> {
    return Flowable.create<Event>({ emitter ->
        val ws = object : WebSocketClient(URI.create(serverUri)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("connected to server: $serverUri")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Connection closed!!! Reconnecting in 5sec...")
                Thread.sleep(5000)
                Thread { this.reconnectBlocking() }.start()
            }

            override fun onMessage(message: String?) {
                message?.let {
                    emitter.onNext(Gson().fromJson(message, Event::class.java))
                }
            }

            override fun onError(ex: java.lang.Exception?) {
                println("error occurred :(")
            }
        }
        ws.connect()
    }, BackpressureStrategy.BUFFER)
}

fun Long.formatDate(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS")
    return formatter.format(Date(this))
}