//
//  ViewController.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import UIKit
import WebRTC
import Foundation

class ViewController: UIViewController, RTCVideoViewDelegate, RTCClientDelegate, WebSocketClientDelegate {

    static let SERVER_URL_KEY = "serverUrl"
    static let TYPE_KEY = "type"
    static let OFFER = "OFFER"
    static let ANSWER = "ANSWER"

    @IBOutlet weak var rtcOtherRenderer: RTCEAGLVideoView!
    @IBOutlet weak var rtcMyRenderer: RTCEAGLVideoView!
    @IBOutlet weak var callButton: UIButton!

    lazy var rtcClient: RTCClient? = nil
    lazy var webSocket: WebSocketClient? = nil

    func videoView(_ videoView: RTCVideoRenderer, didChangeVideoSize size: CGSize) {

    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

        self.rtcMyRenderer.delegate = self
        self.rtcOtherRenderer.delegate = self

        self.rtcClient = RTCClient(observer: self)
        self.rtcClient?.initLocalVideo(renderer: self.rtcMyRenderer)

        self.webSocket = WebSocketClient()
        self.webSocket?.listener = self
        self.webSocket?.connect()
    }

    @IBAction func didClickCallButton(_ sender: Any) {
        self.rtcClient?.startCall()
    }

    func onIceCandidate(iceCandidate: RTCIceCandidate?) {
        if (iceCandidate != nil) {
            let candidate = Candidate(sdp: iceCandidate!.sdp, sdpMLineInedx: iceCandidate!.sdpMLineIndex, sdpMid: iceCandidate!.sdpMid)
            self.webSocket?.send(object: candidate)
            self.rtcClient?.addIceCandidate(iceCandidate: iceCandidate!)
        }
    }

    func onCreateSuccess(sessionDescription: RTCSessionDescription?) {
        var type = ViewController.OFFER
        if (sessionDescription?.type == .answer) {
            type = ViewController.ANSWER
        }
        let description = SessionDescription(type: type, sessionDescription: SDP(sdp: sessionDescription!.sdp), candidate: nil)
        self.webSocket?.send(object: description)
    }

    func onAddStream(mediaStream: RTCMediaStream?) {
        mediaStream?.videoTracks.first?.add(self.rtcOtherRenderer)
    }

    func onConnect() {
        self.callButton.isEnabled = true
    }

    func onMessageReceived(message: String) {
        do {
            let messageData = message.data(using: .utf8)!
            let data = try JSONDecoder().decode(Dictionary<String, String>.self, from: messageData)
            if (data[ViewController.SERVER_URL_KEY] != nil) {
                let candidate = try JSONDecoder().decode(Candidate.self, from: messageData)
                let iceCandidate = RTCIceCandidate(sdp: candidate.sdp, sdpMLineIndex: candidate.sdpMLineInedx, sdpMid: candidate.sdpMid)
                self.rtcClient?.addIceCandidate(iceCandidate: iceCandidate)
            } else if (data[ViewController.TYPE_KEY] != nil) {
                let description = try JSONDecoder().decode(SessionDescription.self, from: messageData)
                if (data[ViewController.TYPE_KEY] == ViewController.OFFER) {
                    let sessionDescription = RTCSessionDescription(type: .offer, sdp: description.sessionDescription.sdp)
                    self.rtcClient?.didReceiveRemoteSession(sessionDescription: sessionDescription)
                    self.rtcClient?.answerCall()
                    self.rtcOtherRenderer.isHidden = false
                } else {
                    let sessionDescription = RTCSessionDescription(type: .answer, sdp: description.sessionDescription.sdp)
                    self.rtcClient?.didReceiveRemoteSession(sessionDescription: sessionDescription)
                }
            }
        } catch {

        }
    }
}
