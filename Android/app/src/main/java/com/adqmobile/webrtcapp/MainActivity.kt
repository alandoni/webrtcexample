package com.adqmobile.webrtcapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.*

class MainActivity : AppCompatActivity(), RTCClient.RTCClientInterface, WebSocketClient.WebSocketListener, PermissionHandler.PermissionHandlerListener {

    companion object {
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

        PermissionHandler(PermissionHandler.PermissionType.CAMERA, this, this).checkPermission()
    }

    private fun initVideo() {
        rtcClient = RTCClient(this, this)
        rtcClient.configureRenderer(rtc_other_renderer)
        rtcClient.configureRenderer(rtc_my_renderer)
        rtcClient.initLocalVideo(rtc_my_renderer)

        webSocketClient = WebSocketClient()
        webSocketClient.listener = this
        webSocketClient.connect()
    }

    override fun onDestroy() {
        webSocketClient.destroy()
        super.onDestroy()
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        webSocketClient.send(iceCandidate)
        rtcClient.addIceCandidate(iceCandidate)
    }

    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        if (sessionDescription != null) {
            webSocketClient.send(sessionDescription)
        } else {
            print("Null session description")
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        if (mediaStream?.videoTracks != null && mediaStream.videoTracks.size > 0) {
            mediaStream.videoTracks[0]?.addSink(rtc_other_renderer)
        }
    }

    override fun onConnect() {
        call_button.isEnabled = true
        call_button.imageAlpha = 255
    }

    override fun onMessageReceived(message: String) {
        print(message)
        val data = webSocketClient.gson.fromJson(message, JsonObject::class.java)

        if (data.has(TYPE_KEY)) {
            val sessionDescription = webSocketClient.gson.fromJson(data, SessionDescription::class.java)
            if (data.get(TYPE_KEY).asString == OFFER) {
                rtcClient.answerCall(sessionDescription)
            } else if (data.get(TYPE_KEY).asString == ANSWER) {
                rtcClient.onRemoteSessionReceived(sessionDescription)
            }
            rtc_other_renderer.visibility = View.VISIBLE
        } else {
            val candidate = webSocketClient.gson.fromJson(data, IceCandidate::class.java)
            rtcClient.addIceCandidate(candidate)
        }
    }

    override fun onPermissionDenied(permissionType: PermissionHandler.PermissionType) {

    }

    override fun onPermissionGranted(permissionType: PermissionHandler.PermissionType) {
        if (permissionType == PermissionHandler.PermissionType.CAMERA) {
            PermissionHandler(PermissionHandler.PermissionType.AUDIO, this@MainActivity, this).checkPermission()
        } else {
            initVideo()
        }
    }
}
