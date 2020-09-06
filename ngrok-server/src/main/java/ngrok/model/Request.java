package ngrok.model;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Request {

    private String url;
    private Socket outerSocket;
    private BlockingQueue<Socket> proxySocket = new ArrayBlockingQueue<>(1);

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Socket getOuterSocket() {
        return outerSocket;
    }

    public void setOuterSocket(Socket outerSocket) {
        this.outerSocket = outerSocket;
    }

    public Socket getProxySocket(long timeout, TimeUnit unit) throws InterruptedException {
        return proxySocket.poll(timeout, unit);
    }

    public void setProxySocket(Socket proxySocket) throws InterruptedException {
        this.proxySocket.put(proxySocket);
    }
}
