//
//  WebSocketClientDelegate.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import Foundation

protocol WebSocketClientDelegate {
    func onConnect()
    func onMessageReceived(message: String)
}
