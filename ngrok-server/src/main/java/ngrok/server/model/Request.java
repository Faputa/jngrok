package ngrok.server.model;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Request {

    private String url;
    private Socket outerSocket;
    private BlockingQueue<Socket> proxySocket = new ArrayBlockingQueue<>(1);

    public Request(String url, Socket outerSocket) {
        this.url = url;
        this.outerSocket = outerSocket;
    }

    public String getUrl() {
        return url;
    }

    public Socket getOuterSocket() {
        return outerSocket;
    }

    public Socket getProxySocket(long timeout, TimeUnit unit) throws InterruptedException {
        return proxySocket.poll(timeout, unit);
    }

    public void setProxySocket(Socket proxySocket) throws InterruptedException {
        this.proxySocket.put(proxySocket);
    }
}
