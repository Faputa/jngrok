package ngrok.server.model;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class Tunnel {

    private String clientId;
    private ServerSocket tcpServerSocket;
    private List<Exitable> outerHandlers = Collections.synchronizedList(new ArrayList<>());

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

    public synchronized void addOuterHandler(Exitable handler) {
        outerHandlers.add(handler);
    }

    public synchronized void removeOuterHandler(Exitable handler) {
        outerHandlers.remove(handler);
    }

    public synchronized void close() {
        SocketHelper.safeClose(tcpServerSocket);
        for (Exitable handler : outerHandlers) {
            handler.exit();
        }
    }
}
