//
//  File.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import Foundation

struct SessionDescription: Codable {
    let type: String
    let sessionDescription: SDP
    let candidate: Candidate?
}

struct SDP: Codable {
    let sdp: String
}

struct Candidate: Codable {
    let sdp: String
    let sdpMLineInedx: Int32
    let sdpMid: String?
}
