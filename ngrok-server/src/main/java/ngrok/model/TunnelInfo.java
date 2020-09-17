package ngrok.model;

import java.net.ServerSocket;

import ngrok.socket.SocketHelper;

public class TunnelInfo {

    private String clientId;
    private ServerSocket tcpServerSocket;

    public TunnelInfo(String clientId, ServerSocket tcpServerSocket) {
        this.clientId = clientId;
        this.tcpServerSocket = tcpServerSocket;
    }

    public String getClientId() {
        return clientId;
    }

    public ServerSocket getTcpServerSocket() {
        return tcpServerSocket;
    }

    public void close() {
        SocketHelper.safeClose(tcpServerSocket);
    }
}
