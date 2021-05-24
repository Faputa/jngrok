package ngrok.server.model;

import java.net.ServerSocket;
import java.util.concurrent.CopyOnWriteArrayList;

import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class Tunnel {

    private String clientId;
    private ServerSocket tcpServerSocket;
    private CopyOnWriteArrayList<Exitable> outerHandlers = new CopyOnWriteArrayList<>();

    public Tunnel(String clientId, ServerSocket tcpServerSocket) {
        this.clientId = clientId;
        this.tcpServerSocket = tcpServerSocket;
    }

    public String getClientId() {
        return clientId;
    }

    public ServerSocket getTcpServerSocket() {
        return tcpServerSocket;
    }

    public void addOuterHandler(Exitable handler) {
        outerHandlers.add(handler);
    }

    public void removeOuterHandler(Exitable handler) {
        outerHandlers.remove(handler);
    }

    public void close() {
        SocketHelper.safeClose(tcpServerSocket);
        for (Exitable handler : outerHandlers) {
            handler.exit();
        }
    }
}
