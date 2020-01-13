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

class ViewController: UIViewController, RTCClientDelegate, WebSocketClientDelegate {

    static let SERVER_URL_KEY = "serverUrl"
    static let TYPE_KEY = "type"
    static let OFFER = "OFFER"
    static let ANSWER = "ANSWER"

    @IBOutlet weak var rtcOtherRenderer: RTCEAGLVideoView!
    @IBOutlet weak var rtcMyRenderer: RTCEAGLVideoView!
    @IBOutlet weak var callButton: UIButton!

    lazy var rtcClient: RTCClient? = nil
    lazy var webSocket: WebSocketClient? = nil

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

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
            let candidate = Candidate(sdp: iceCandidate!.sdp, sdpMLineIndex: iceCandidate!.sdpMLineIndex, sdpMid: iceCandidate!.sdpMid, serverUrl: iceCandidate?.serverUrl ?? "")
            self.webSocket?.send(object: candidate)
            self.rtcClient?.addIceCandidate(iceCandidate: iceCandidate!)
        }
    }

    func onCreateSuccess(sessionDescription: RTCSessionDescription?) {
        var type = ViewController.OFFER
        if (sessionDescription?.type == .answer) {
            type = ViewController.ANSWER
        }
        let description = SessionDescription(type: type, description: sessionDescription!.sdp, candidate: nil)
        self.webSocket?.send(object: description)
    }

    func onAddStream(mediaStream: RTCMediaStream?) {
        Timer.scheduledTimer(withTimeInterval: 2.0, repeats: false) { [unowned self] (Timer) in
            let videoTrack = mediaStream?.videoTracks.first
            videoTrack?.isEnabled = true
            videoTrack?.add(self.rtcOtherRenderer)
        }.fire()
    }

    func onConnect() {
        self.callButton.isEnabled = true
    }

    func onMessageReceived(message: String) {
        do {
            let messageData = message.data(using: .utf8)!
            let data = try JSONSerialization.jsonObject(with: messageData, options: []) as! [String: Any?]
            if (data[ViewController.SERVER_URL_KEY] != nil) {
                let candidate = try JSONDecoder().decode(Candidate.self, from: messageData)
                let iceCandidate = RTCIceCandidate(sdp: candidate.sdp, sdpMLineIndex: candidate.sdpMLineIndex, sdpMid: candidate.sdpMid)
                self.rtcClient?.addIceCandidate(iceCandidate: iceCandidate)
            } else if (data[ViewController.TYPE_KEY] != nil) {
                let description = try JSONDecoder().decode(SessionDescription.self, from: messageData)
                let type = data[ViewController.TYPE_KEY] as! String
                if (type == ViewController.OFFER) {
                    let sessionDescription = RTCSessionDescription(type: .offer, sdp: description.description)
                    self.rtcClient?.answerCall(sessionDescription: sessionDescription)
                } else {
                    let sessionDescription = RTCSessionDescription(type: .answer, sdp: description.description)
                    self.rtcClient?.didReceiveRemoteSession(sessionDescription: sessionDescription)
                }
                self.rtcOtherRenderer.isHidden = false
            }
        } catch {
            print(error)
        }
    }
}
