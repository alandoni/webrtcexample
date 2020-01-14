export default class WebSocketClient {
    connect() {
        let host = "192.168.2.1"
        this.webSocket = new WebSocket(`ws://${host}:8080/connect`);
        this.webSocket.onopen = () => this.onConnect();
        this.webSocket.onmessage = async(messageEvent) => this.onMessage(messageEvent.data);
    }

    sendMessage(message) {
        console.log(`Sending message: ${message}`)
        this.webSocket.send(message)
    }
}