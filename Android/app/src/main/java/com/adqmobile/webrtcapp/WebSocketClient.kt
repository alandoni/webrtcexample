package com.adqmobile.webrtcapp;

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.CoroutineContext

class WebSocketClient : CoroutineScope {
    companion object {
        private const val HOST_ADDRESS = "192.168.2.1"
    }

    val gson = Gson()
    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val sendChannel = ConflatedBroadcastChannel<String>()
    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    var listener: WebSocketListener? = null

    fun connect() = launch {
        client.ws(HttpMethod.Get, HOST_ADDRESS, 8080, "/connect") {
            withContext(Dispatchers.Main) {
                listener?.onConnect()
            }
            val sendData = sendChannel.openSubscription()
            try {
                while (true) {
                    sendMessage(sendData)
                    receiveData()
                }
            } catch (exception: Throwable) {
                exception.printStackTrace()
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveData() {
        val receivedMessage = incoming.poll()
        if (receivedMessage != null) {
            if (receivedMessage is Frame.Text) {
                val message = receivedMessage.readText()
                withContext(Dispatchers.Main) {
                    listener?.onMessageReceived(message)
                }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(sendData: ReceiveChannel<String>) {
        val message = sendData.poll()
        if (message != null) {
            outgoing.send(Frame.Text(message))
        }
    }

    fun send(data: Any?) = runBlocking {
        sendChannel.send(gson.toJson(data))
    }

    fun destroy() {
        client.close()
        sendChannel.close()
    }

    interface WebSocketListener {
        fun onConnect()

        fun onMessageReceived(message: String)
    }
}
