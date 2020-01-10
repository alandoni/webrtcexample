//
//  WebSocketClient.swift
//  WebRTCApp
//
//  Created by Alan Donizete Quintiliano on 08/01/20.
//  Copyright © 2020 Alan Donizete Quintiliano. All rights reserved.
//

import Foundation
import Starscream

class WebSocketClient: NSObject {

    static let HOST = "192.168.2.1"

    let webSocket: WebSocket = {
        return WebSocket(url: URL(string: "ws://\(WebSocketClient.HOST):8080/connect")!)
    }()
    var listener: WebSocketClientDelegate? = nil

    func connect() {
        webSocket.onConnect = {
            [unowned self] in
            self.listener?.onConnect()
        }
        webSocket.onText = {
            [unowned self] text in
            self.listener?.onMessageReceived(message: text)
        }
        self.webSocket.connect()
    }

    func send<T: Codable>(object: T) {
        do {
            let json = try JSONEncoder().encode(object)
            let encoded = String(data: json, encoding: .utf8)!
            self.webSocket.write(string: encoded)
        } catch {
            
        }
    }
}
