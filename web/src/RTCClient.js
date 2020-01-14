export default class RTCClient {

    constructor() {
        this.constraints = { audio: true, video: true };
        this.descriptionConstraints = { offerToReceiveAudio: true, offerToReceiveVideo: true };
        let configuration = { iceServers: [{ urls: 'stun:stun.example.org' }] };
        this.peerConnection = new RTCPeerConnection(configuration);
        this.peerConnection.onicecandidate = ({ candidate }) => {
            console.log(`adding new candidate: ${JSON.stringify(candidate)}`)
            if (candidate) {
                this.onIceCandidate(candidate);
            }
        }
        this.peerConnection.onnegotiationneeded = async() => {
            console.log('Negotiation needed');
        }
        this.peerConnection.ontrack = (event) => this.onAddStream(event.streams);
    }

    async startLocalVideo(view) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia(this.constraints);
            stream.getTracks().forEach((track) => {
                this.peerConnection.addTrack(track, stream);
                view.srcObject = stream;
            });
            view.play()
            view.muted = true;
        } catch (error) {
            console.log(error)
        }
    }

    async addIceCandidate(candidate) {
        await this.peerConnection.addIceCandidate(candidate)
    }

    async startCall() {
        console.log("Creating offer")
        await this.peerConnection.setLocalDescription(await this.peerConnection.createOffer(this.descriptionConstraints));
        this.onCreateSuccess(this.peerConnection.localDescription);
    }

    async answerCall(description) {
        await this.peerConnection.setRemoteDescription(description)
        console.log("Creating answer")
        await this.peerConnection.setLocalDescription(await this.peerConnection.createAnswer(this.descriptionConstraints));
        this.onCreateSuccess(this.peerConnection.localDescription);
    }

    async onReceiveRemoteDescription(description) {
        await this.peerConnection.setRemoteDescription(description)
        this.onCreateSuccess(this.peerConnection.remoteDescription);
    }
}