package ngrok.server.model;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class Client {

    private Socket controlSocket;
    private long lastPingTime;
    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    private List<Exitable> proxyHandlers = Collections.synchronizedList(new ArrayList<>());

    public Client(Socket controlSocket) {
        this.lastPingTime = System.currentTimeMillis();
        this.controlSocket = controlSocket;
    }

    public Socket getControlSocket() {
        return controlSocket;
    }

    public long getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    public void putRequest(Request request) throws InterruptedException {
        this.requestQueue.put(request);
    }

    public Request pollRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return requestQueue.poll(timeout, unit);
    }

    public void close() {
        SocketHelper.safeClose(controlSocket);
    }

    public synchronized void addProxyHandler(Exitable handler) {
        proxyHandlers.add(handler);
    }

    public synchronized void removeProxyHandler(Exitable handler) {
        proxyHandlers.remove(handler);
    }

    public synchronized void clean() {
        for (Exitable handler : proxyHandlers) {
            handler.exit();
        }
    }
}
