package ngrok.model;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.socket.SocketHelper;

public class ClientInfo {

    private Socket controlSocket;
    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    private List<Socket> proxySockets = Collections.synchronizedList(new ArrayList<>());
    private List<Socket> outerSockets = Collections.synchronizedList(new ArrayList<>());
    private long lastPingTime;

    public ClientInfo(Socket controlSocket) {
        this.lastPingTime = System.currentTimeMillis();
        this.controlSocket = controlSocket;
    }

    public Socket getControlSocket() {
        return controlSocket;
    }

    public void setControlSocket(Socket controlSocket) {
        this.controlSocket = controlSocket;
    }

    public BlockingQueue<Request> getRequestQueue() {
        return requestQueue;
    }

    public void addProxySocket(Socket socket) {
        proxySockets.add(socket);
    }

    public void removeProxySocket(Socket socket) {
        proxySockets.remove(socket);
    }

    public void addOuterSocket(Socket socket) {
        outerSockets.add(socket);
    }

    public void removeOuterSocket(Socket socket) {
        outerSockets.remove(socket);
    }

    public long getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    public void close() {
        SocketHelper.safeClose(controlSocket);
    }

    public void clean() {
        try {
            requestQueue.put(new Request());// 毒丸
        } catch (InterruptedException e) {
            // ignore
        }
        for (Socket socket : proxySockets) {
            SocketHelper.safeClose(socket);
        }
        for (Socket socket : outerSockets) {
            SocketHelper.safeClose(socket);
        }
    }
}
