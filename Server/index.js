const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 8080 }, () => {
    console.log("Signaling server is now listening on port 8080")
});

// Broadcast to all.
wss.broadcast = (ws, data) => {
    wss.clients.forEach((client) => {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
            client.send(data);
        }
    });
};

wss.on('connection', (ws, req) => {
    const ip = req.connection.remoteAddress;
    ws.ip = ip;
    ws.id = wss.clients.size;
    console.log(`Client with ip ${ws.ip} and the id ${ws.id} connected. Total connected clients: ${wss.clients.size}`)

    ws.onmessage = (message) => {
        const obj = JSON.parse(message.data);

        if (obj.type) {
            console.log(`Client with ID ${ws.id} sending session of type: ${obj.type}`);
        } else {
            console.log(`Client with ID ${ws.id} sending candidate with type: ${obj.sdpMid}`);
        }
        wss.broadcast(ws, message.data);
    }

    ws.onclose = () => {
        console.log(`Client with ip ${ws.ip} and the id ${ws.id} disconnected. Total connected clients: ${wss.clients.size}`)
    }
});