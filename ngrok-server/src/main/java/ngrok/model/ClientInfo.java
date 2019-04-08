package ngrok.model;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.socket.SocketHelper;

public class ClientInfo {

    private Socket controlSocket;
    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();

    public Socket getControlSocket() {
        return controlSocket;
    }

    public void setControlSocket(Socket controlSocket) {
        this.controlSocket = controlSocket;
    }

    public BlockingQueue<Request> getRequestQueue() {
        return requestQueue;
    }

    public void close() {
        try {
            requestQueue.put(new Request());// 毒丸
        } catch (InterruptedException e) {
            // ignore
        }
        SocketHelper.safeClose(controlSocket);
    }
}
