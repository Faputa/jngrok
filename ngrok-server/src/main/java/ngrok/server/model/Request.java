package ngrok.server.model;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import ngrok.util.BlockingCell;

public class Request {

    private String url;
    private Socket publicSocket;
    private BlockingCell<Socket> proxySocket = new BlockingCell<>();

    public Request(String url, Socket publicSocket) {
        this.url = url;
        this.publicSocket = publicSocket;
    }

    public String getUrl() {
        return url;
    }

    public Socket getPublicSocket() {
        return publicSocket;
    }

    public Socket getProxySocket(long timeout, TimeUnit unit) throws InterruptedException {
        return proxySocket.get(timeout, unit);
    }

    public void setProxySocket(Socket proxySocket) {
        this.proxySocket.set(proxySocket);
    }
}
