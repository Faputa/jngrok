package ngrok.server.model;

import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class Client {

    private Socket controlSocket;
    private long lastPingTime;
    private LinkedBlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    private CopyOnWriteArrayList<Exitable> proxyHandlers = new CopyOnWriteArrayList<>();

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

    public void addProxyHandler(Exitable handler) {
        proxyHandlers.add(handler);
    }

    public void removeProxyHandler(Exitable handler) {
        proxyHandlers.remove(handler);
    }

    public void clean() {
        for (Exitable handler : proxyHandlers) {
            handler.exit();
        }
    }
}
