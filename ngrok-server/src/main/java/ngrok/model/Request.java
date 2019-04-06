package ngrok.model;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Request {

    private String url;
    private Socket outerSocket;
    private Socket controlSocket;
    private BlockingQueue<Socket> _proxySocket = new ArrayBlockingQueue<>(1);

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

    public Socket getControlSocket() {
        return controlSocket;
    }

    public void setControlSocket(Socket controlSocket) {
        this.controlSocket = controlSocket;
    }

    public Socket getProxySocket(long timeout, TimeUnit unit) throws InterruptedException {
        return _proxySocket.poll(timeout, unit);
    }

    public void setProxySocket(Socket proxySocket) throws InterruptedException {
        _proxySocket.put(proxySocket);
    }
}
