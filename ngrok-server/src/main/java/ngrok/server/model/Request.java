package ngrok.server.model;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import ngrok.util.BlockingCell;

public class Request {

    private String url;
    private Socket outerSocket;
    private BlockingCell<Socket> proxySocket = new BlockingCell<>();

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
        return proxySocket.get(timeout, unit);
    }

    public void setProxySocket(Socket proxySocket) {
        this.proxySocket.set(proxySocket);
    }
}
