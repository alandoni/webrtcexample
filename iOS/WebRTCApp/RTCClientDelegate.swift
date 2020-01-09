//
//  RTCClientDelegate.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import Foundation
import WebRTC

protocol RTCClientDelegate {
    func onIceCandidate(iceCandidate: RTCIceCandidate?)
    func onCreateSuccess(sessionDescription: RTCSessionDescription?)
    func onAddStream(mediaStream: RTCMediaStream?)
}
