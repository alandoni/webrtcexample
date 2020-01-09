package com.adqmobile.webrtcapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SERVER_URL_KEY = "serverUrl"
        private const val TYPE_KEY = "type"
        private const val OFFER = "OFFER"
        private const val ANSWER = "ANSWER"
    }

    private lateinit var rtcClient: RTCClient
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        call_button.isEnabled = false
        call_button.imageAlpha = 60
        call_button.setOnClickListener {
            rtcClient.startCall()
        }

        PermissionHandler(PermissionHandler.PermissionType.CAMERA, this, object : PermissionHandler.PermissionHandlerListener {
            override fun onPermissionDenied() {

            }

            override fun onPermissionGranted() {
                PermissionHandler(PermissionHandler.PermissionType.AUDIO, this@MainActivity, object : PermissionHandler.PermissionHandlerListener {
                    override fun onPermissionDenied() {

                    }

                    override fun onPermissionGranted() {
                        initVideo()
                    }
                }).checkPermission()
            }
        }).checkPermission()
    }

    private fun initVideo() {
        rtcClient = RTCClient(this, object: RTCClient.RTCClientInterface {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                webSocketClient.send(iceCandidate)
                rtcClient.addIceCandidate(iceCandidate)
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                webSocketClient.send(sessionDescription)
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                mediaStream?.videoTracks?.get(0)?.addSink(rtc_other_renderer)
            }
        })
        rtcClient.configureRenderer(rtc_other_renderer)
        rtcClient.configureRenderer(rtc_my_renderer)
        rtcClient.initLocalVideo(rtc_my_renderer)

        webSocketClient = WebSocketClient()
        webSocketClient.listener = object : WebSocketClient.WebSocketListener {
            override fun onConnect() {
                call_button.isEnabled = true
                call_button.imageAlpha = 255
            }

            override fun onMessageReceived(message: String) {
                val data = webSocketClient.gson.fromJson(message, JsonObject::class.java)
                if (data.has(SERVER_URL_KEY)) {
                    val candidate = webSocketClient.gson.fromJson(data, IceCandidate::class.java)
                    rtcClient.addIceCandidate(candidate)
                } else if (data.has(TYPE_KEY)) {
                    val sessionDescription = webSocketClient.gson.fromJson(data, SessionDescription::class.java)
                    if (data.get(TYPE_KEY).asString == OFFER) {
                        rtcClient.onRemoteSessionReceived(sessionDescription)
                        rtcClient.answerCall()
                        rtc_other_renderer.visibility = View.VISIBLE
                    } else if (data.get(TYPE_KEY).asString == ANSWER) {
                        rtcClient.onRemoteSessionReceived(sessionDescription)
                    }
                }
            }
        }

        try {
            webSocketClient.connect()
        } catch (exception: Throwable) {
            exception.printStackTrace()
        }
    }

    override fun onDestroy() {
        webSocketClient.destroy()
        super.onDestroy()
    }
}
