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
    let description: String
    let candidate: Candidate?
}

struct Candidate: Codable {
    let sdp: String
    let sdpMLineIndex: Int32
    let sdpMid: String?
    let serverUrl: String?
}
