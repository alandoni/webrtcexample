import React from 'react';
import './App.css';
import RTCClient from './RTCClient'
import WebSocketClient from './WebSocket'

export default class App extends React.Component {
    constructor() {
        super()
        this.state = {
            isConnected: false
        }
        this.localVideo = React.createRef()
        this.remoteVideo = React.createRef()
        this.rtcClient = new RTCClient();
        this.webSocketClient = new WebSocketClient();
    }

    componentDidMount() {
        this.rtcClient.onIceCandidate = this.onIceCandidate
        this.rtcClient.onCreateSuccess = this.onCreateSuccess
        this.rtcClient.onAddStream = this.onAddStream

        this.webSocketClient.onConnect = this.onConnect
        this.webSocketClient.onMessage = this.onMessage
        this.webSocketClient.connect()

        this.rtcClient.startLocalVideo(this.localVideo.current)
    }

    render() {
        return <div>
            <video ref={this.localVideo} />
            <video ref={this.remoteVideo} />
            <input type="button" enabled={this.state.isConnected ? "enabled" : "disabled"} value="Call" onClick={this.startCall}/>
        </div>
    }

    startCall = () => {
        this.rtcClient.startCall()
    }

    onMessage = async(message) => {
        console.log(`Received message: ${message}`)
        let obj = JSON.parse(message);
        if (obj.type) {
            if (obj.description) { 
                obj.sdp = obj.description.normalize()
                obj.type = obj.type.toLowerCase()
                delete obj.description
            }
            if (obj.type === 'offer') {
                await this.rtcClient.answerCall(obj);
            } else if (obj.type === 'answer') {
                await this.rtcClient.onReceiveRemoteDescription(obj);
            } 
        } else {
            await this.rtcClient.addIceCandidate(obj);
        }
    };

    onConnect = () => {
        console.log(`Websocket connected`)
        this.setState({isConnected: true});
    }

    onAddStream = (streams) => {
        let video = this.remoteVideo.current
        streams[0].muted = false;
        video.srcObject = streams[0];
        video.play()
    }

    onCreateSuccess = (description) => {
        let newDesc = {
            type: description.type.toUpperCase(),
            description: description.sdp
        }
        this.webSocketClient.sendMessage(JSON.stringify(newDesc));
    }

    onIceCandidate = (candidate) => {
        let newCandidate = {
            sdpMid: candidate.sdpMid,
            sdpMLineIndex: candidate.sdpMLineIndex,
            sdp: candidate.candidate,
            serverUrl: ""
        }
        this.webSocketClient.sendMessage(JSON.stringify(newCandidate));
        this.rtcClient.addIceCandidate(candidate);
    }
}