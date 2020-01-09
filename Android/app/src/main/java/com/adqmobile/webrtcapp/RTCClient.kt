package com.adqmobile.webrtcapp

import android.content.Context
import org.webrtc.*
import java.util.*


class RTCClient(
    private val context: Context,
    private val observer: RTCClientInterface)
{

    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory by lazy {
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
    private val peerConnection by lazy {
        val iceServer = listOf(
            PeerConnection.IceServer
                .builder("stun:stun.l.google.com:19302")
                .createIceServer()
        )
        //val remoteAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        //val remoteAudioTrack = peerConnectionFactory.createAudioTrack("5555", remoteAudioSource)

        //Local audio track
        val localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val localAudioTrack = peerConnectionFactory.createAudioTrack("TestAudio", localAudioSource)
        localAudioTrack.setEnabled(true) //Enable audio

        val peerConnection = peerConnectionFactory.createPeerConnection(iceServer, object: PeerConnection.Observer  {
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
        })
        val mediaStreamLabels: List<String> = Collections.singletonList("StreamAudio")
        peerConnection?.addTrack(localAudioTrack, mediaStreamLabels)
        peerConnection?.setAudioRecording(true)
        peerConnection?.setAudioPlayout(true)

        return@lazy peerConnection
    }

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
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
        val localVideoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)

        videoCapturer.startCapture(1920, 1080, 60)

        val localVideoTrack = peerConnectionFactory.createVideoTrack("TestVideo", localVideoSource)
        localVideoTrack.addSink(renderer)

        val localStream = peerConnectionFactory.createLocalMediaStream("StreamTest")
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)

        peerConnection?.addTrack(localVideoTrack, mutableListOf("StreamVideo"))
    }

    fun configureRenderer(renderer: SurfaceViewRenderer) {
        renderer.setMirror(true)
        renderer.setEnableHardwareScaler(true)
        renderer.init(rootEglBase.eglBaseContext, null)
    }

    fun startCall() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
                print(p0)
            }

            override fun onSetSuccess() {

            }

            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        print(p0)
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                        print(p0)
                    }
                }, description)
                observer.onCreateSuccess(description)
            }

            override fun onCreateFailure(p0: String?) {
                print(p0)
            }

        }, constraints)
    }

    fun answerCall() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
            }

            override fun onSetSuccess() {
            }

            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                    }
                }, description)
                observer.onCreateSuccess(description)
            }

            override fun onCreateFailure(p0: String?) {
            }
        }, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
            }

            override fun onSetSuccess() {
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onCreateFailure(p0: String?) {
            }
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    interface RTCClientInterface {
        fun onIceCandidate(iceCandidate: IceCandidate?)
        fun onCreateSuccess(sessionDescription: SessionDescription?)
        fun onAddStream(mediaStream: MediaStream?)
    }
}