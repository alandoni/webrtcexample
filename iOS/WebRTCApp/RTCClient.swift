//
//  RTCClient.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import Foundation
import WebRTC

class RTCClient: NSObject {

    let observer: RTCClientDelegate

    lazy var connectionFactory: RTCPeerConnectionFactory = {
        [unowned self] in
        let connectionFactory = RTCPeerConnectionFactory(
            encoderFactory: RTCDefaultVideoEncoderFactory(),
            decoderFactory: RTCDefaultVideoDecoderFactory())
        let rtcOptions = RTCPeerConnectionFactoryOptions()
        rtcOptions.disableEncryption = true
        rtcOptions.disableNetworkMonitor = true
        connectionFactory.setOptions(rtcOptions)
        return connectionFactory
    }()

    lazy var peerConnection: RTCPeerConnection = {
        [unowned self] in
        let configuration = RTCConfiguration()
        configuration.iceServers = [RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302"])]
        let defaultConstraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: /*["DtlsSrtpKeyAgreement": "true"]*/nil)

        return self.connectionFactory.peerConnection(with: configuration,
                                                                    constraints: defaultConstraints,
                                                                    delegate: self)
    }()

    lazy var localStream: RTCMediaStream? = nil

    lazy var remoteStream: RTCMediaStream? = nil

    init(observer: RTCClientDelegate) {
        self.observer = observer
        super.init()

        RTCPeerConnectionFactory.initialize()

        let audioTrack = self.audioTrack()
        let videoTrack = self.videoTrack()
        self.localStream = self.connectionFactory.mediaStream(withStreamId: "StreamTest1")
        self.localStream!.addVideoTrack(videoTrack)
        self.localStream!.addAudioTrack(audioTrack)
        self.peerConnection.add(self.localStream!)
    }

    func audioTrack() -> RTCAudioTrack {
        let audioConstrains = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: nil)
        let localAudioSource = self.connectionFactory.audioSource(with: audioConstrains)
        let localAudioTrack = self.connectionFactory.audioTrack(with: localAudioSource, trackId: "TestAudio1")
        localAudioTrack.isEnabled = true
        return localAudioTrack
    }

    func getFrontVideoCapturer() -> AVCaptureDevice? {
        let devices = RTCCameraVideoCapturer.captureDevices()

        if (devices.isEmpty) {
            return nil
        }

        let camera = RTCCameraVideoCapturer.captureDevices().filter { (device: AVCaptureDevice) -> Bool in
            device.position == AVCaptureDevice.Position.front
        }
        if (camera.count > 0) {
            return camera[0]
        } else {
            return devices[0]
        }
    }

    func videoTrack() -> RTCVideoTrack {
        let localVideoSource = self.connectionFactory.videoSource()
        let localVideoTrack = self.connectionFactory.videoTrack(with: localVideoSource, trackId: "TestVideo1")
        return localVideoTrack
    }

    func initLocalVideo(renderer: RTCEAGLVideoView) {
        let device = getFrontVideoCapturer()
        let videoSource = self.localStream!.videoTracks.first!.source
        if (device != nil) {
            let capturer = RTCCameraVideoCapturer(delegate: videoSource)
            capturer.startCapture(with: device!, format: RTCCameraVideoCapturer.supportedFormats(for: device!).last!, fps: 60)
        } else {
            let capturer = RTCFileVideoCapturer(delegate: videoSource)
            if let _ = Bundle.main.url(forResource: "sample", withExtension: "mp4") {
                capturer.startCapturing(fromFileNamed: "sample.mp4") { (err) in
                    print(err)
                }
            } else {
                print("File not found")
            }
        }

        self.localStream!.videoTracks.first!.add(renderer)
    }

    func createAudioVideoConstraints() -> RTCMediaConstraints {
        return RTCMediaConstraints(mandatoryConstraints: nil/*[
            "OfferToReceiveVideo": "true",
            "OfferToReceiveAudio": "true"
        ]*/, optionalConstraints: nil)
    }

    func startCall() {
        let constraints = createAudioVideoConstraints()
        self.peerConnection.offer(for: constraints, completionHandler: { (description: RTCSessionDescription?, error: Error?) in
            if (error != nil) {
                print("Error making offer: \(error.debugDescription)")
            } else {
                self.peerConnection.setLocalDescription(description!, completionHandler: { (error: Error?) in
                    if (error != nil) {
                        print("Error making offer local: \(error.debugDescription)")
                    }
                })
                self.observer.onCreateSuccess(sessionDescription: description)
            }
        })
    }

    func answerCall(sessionDescription: RTCSessionDescription) {
        didReceiveRemoteSession(sessionDescription: sessionDescription)
        let constraints = self.createAudioVideoConstraints()
        self.peerConnection.answer(for: constraints, completionHandler: { (description: RTCSessionDescription?, error: Error?) in
            if (error != nil) {
                print("Error answering: \(error.debugDescription)")
            } else {
                self.peerConnection.setLocalDescription(description!, completionHandler: { (error: Error?) in
                    if (error != nil) {
                        print("Error answering local: \(error.debugDescription)")
                    }
                })
                self.observer.onCreateSuccess(sessionDescription: description)
            }
        })
    }

    func didReceiveRemoteSession(sessionDescription: RTCSessionDescription) {
        self.peerConnection.setRemoteDescription(sessionDescription, completionHandler: { (error: Error?) in
            if (error != nil) {
                print("Error answering: \(error.debugDescription)")
            }
        })
    }

    func addIceCandidate(iceCandidate: RTCIceCandidate) {
        self.peerConnection.add(iceCandidate)
    }
}

extension RTCClient: RTCPeerConnectionDelegate {
    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {

    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {
        var state: String? = nil
        switch (stateChanged) {
        case .closed:
            state = "closed"
            break
        case .stable:
            state = "stable"
            break
        case .haveLocalOffer:
            state = "haveLocalOffer"
            break
        case .haveLocalPrAnswer:
            state = "haveLocalPrAnswer"
            break
        case .haveRemoteOffer:
            state = "haveRemoteOffer"
            break
        case .haveRemotePrAnswer:
            state = "haveRemotePrAnswer"
            break
        default:
            state = ""
            break
        }
        print("Signaling state: \(state!)")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        self.remoteStream = stream
        observer.onAddStream(mediaStream: stream)
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {

    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        var state: String? = nil
        switch (newState) {
        case .checking:
            state = "checking"
            break
        case .closed:
            state = "closed"
            break
        case .completed:
            state = "completed"
            break
        case .connected:
            state = "connected"
            break
        case .count:
            state = "count"
            break
        case .disconnected:
            state = "disconnected"
            break
        case .failed:
            state = "failed"
            break
        case .new:
            state = "new"
            break
        default:
            state = ""
            break
        }
        print("ICE state: \(state!)")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {
        var state: String? = nil
        switch (newState) {
        case .complete:
            state = "checking"
            break
        case .gathering:
            state = "closed"
            break
        case .new:
            state = "new"
            break
        default:
            state = "haveRemotePrAnswer"
            break
        }
        print("ICE gather state: \(state!)")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        observer.onIceCandidate(iceCandidate: candidate)
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {

    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {

    }
}
