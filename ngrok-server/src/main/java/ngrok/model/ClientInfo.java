package ngrok.model;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ngrok.socket.SocketHelper;

public class ClientInfo {

    private Socket controlSocket;
    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    private List<Thread> proxyThreads = Collections.synchronizedList(new ArrayList<>());
    private List<Thread> outerThreads = Collections.synchronizedList(new ArrayList<>());
    private long lastPingTime;

    public ClientInfo(Socket controlSocket) {
        this.lastPingTime = System.currentTimeMillis();
        this.controlSocket = controlSocket;
    }

    public Socket getControlSocket() {
        return controlSocket;
    }

    public void putRequest(Request request) throws InterruptedException {
        this.requestQueue.put(request);
    }

    public Request pollRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return requestQueue.poll(timeout, unit);
    }

    public synchronized void addProxyThread(Thread thread) {
        proxyThreads.add(thread);
    }

    public synchronized void removeProxyThread(Thread thread) {
        proxyThreads.remove(thread);
    }

    public synchronized void addOuterThread(Thread thread) {
        outerThreads.add(thread);
    }

    public synchronized void removeOuterThread(Thread thread) {
        outerThreads.remove(thread);
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

    public synchronized void clean() {
        for (Thread thread : proxyThreads) {
            thread.interrupt();
        }
        for (Thread thread : outerThreads) {
            thread.interrupt();
        }
    }
}
