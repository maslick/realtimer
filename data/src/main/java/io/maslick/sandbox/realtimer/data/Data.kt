package io.maslick.sandbox.realtimer.data

data class Data(val accountId: String, val data: String)
data class Event(val accountId: String, val data: String, val timestamp: Long)