package com.adqmobile.webrtcapp

import android.content.Context
import org.webrtc.*
import java.util.*


class RTCClient(
    private val context: Context,
    private val observer: RTCClientInterface) : PeerConnection.Observer
{

    private var localVideoTrack: VideoTrack? = null
    private var localVideoSource: VideoSource? = null
    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        return@lazy PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }
    private val peerConnection: PeerConnection by lazy {
        val iceServer = listOf(
            PeerConnection.IceServer
                .builder("stun:stun.l.google.com:19302")
                .createIceServer()
        )
        //val remoteAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        //val remoteAudioTrack = peerConnectionFactory.createAudioTrack("5555", remoteAudioSource)

        val mediaConstraints = MediaConstraints().apply {
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
        val configurations = PeerConnection.RTCConfiguration(iceServer)
        val peerConnection = peerConnectionFactory.createPeerConnection(configurations, mediaConstraints, this)!!

        peerConnection.setAudioRecording(true)
        peerConnection.setAudioPlayout(true)

        return@lazy peerConnection
    }
    private val sdpObserver = object: SdpObserver {
        override fun onSetFailure(p0: String?) {
            print(p0)
        }

        override fun onSetSuccess() {

        }

        override fun onCreateFailure(p0: String?) {
            print(p0)
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
        }
    }

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val localStream = peerConnectionFactory.createLocalMediaStream("StreamTest")
        val audioTrack = createLocalAudioTrack()
        val videoTrack = createLocalVideoTrack()
        localStream.addTrack(audioTrack)
        localStream.addTrack(videoTrack)
        peerConnection.addStream(localStream)
    }

    private fun audioVideoMediaConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    private fun getFrontVideoCapturer() = Camera2Enumerator(context).run {
        if (deviceNames.isEmpty()) {
            throw IllegalStateException("No cameras found")
        }

        var camera = deviceNames.find {
            isFrontFacing(it)
        }
        if (camera == null) {
            camera = deviceNames.get(0)
        }

        createCapturer(camera, null)
    }

    fun initLocalVideo(renderer: SurfaceViewRenderer) {
        val videoCapturer = getFrontVideoCapturer()
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)

        videoCapturer.startCapture(1920, 1080, 60)
        localVideoTrack?.addSink(renderer)
        localVideoTrack?.setEnabled(true)
    }

    private fun createLocalVideoTrack() : VideoTrack {
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        localVideoTrack =
            peerConnectionFactory.createVideoTrack("TestVideo", localVideoSource)
        return localVideoTrack!!
    }

    private fun createLocalAudioTrack() : AudioTrack {
        //Local audio track
        val localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val localAudioTrack = peerConnectionFactory.createAudioTrack("TestAudio", localAudioSource)
        localAudioTrack.setEnabled(true) //Enable audio
        return localAudioTrack
    }

    fun configureRenderer(renderer: SurfaceViewRenderer) {
        renderer.setMirror(true)
        renderer.setEnableHardwareScaler(true)
        renderer.init(rootEglBase.eglBaseContext, null)
    }

    fun startCall() {
        val constraints = audioVideoMediaConstraints()

        peerConnection.createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection.setLocalDescription(sdpObserver, description)
                observer.onCreateSuccess(description)
            }

        }, constraints)
    }

    fun answerCall(sessionDescription: SessionDescription) {
        onRemoteSessionReceived(sessionDescription)
        val constraints = audioVideoMediaConstraints()

        peerConnection.createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection.setLocalDescription(sdpObserver, description)
                observer.onCreateSuccess(description)
            }
        }, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(sdpObserver, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection.addIceCandidate(iceCandidate)
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        observer.onIceCandidate(iceCandidate)
    }

    override fun onDataChannel(p0: DataChannel?) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        observer.onAddStream(mediaStream)
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onRemoveStream(p0: MediaStream?) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }

    interface RTCClientInterface {
        fun onIceCandidate(iceCandidate: IceCandidate?)
        fun onCreateSuccess(sessionDescription: SessionDescription?)
        fun onAddStream(mediaStream: MediaStream?)
    }
}

