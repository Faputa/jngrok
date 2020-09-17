/**
 * 与本地服务器建立连接，并将本地服务器的流量转发给管道
 */
package ngrok.connect;

import ngrok.NgContext;
import ngrok.socket.SocketHelper;

import java.net.Socket;

public class LocalConnect implements Runnable {

    private Socket localSocket;
    private Socket remoteSocket;
    private NgContext context;

    public LocalConnect(Socket localSocket, Socket remoteSocket, NgContext context) {
        this.localSocket = localSocket;
        this.remoteSocket = remoteSocket;
        this.context = context;
    }

    @Override
    public void run() {
        try (Socket localSocket = this.localSocket;
             Socket remoteSocket = this.remoteSocket) {
            context.addLocalThread(Thread.currentThread());
            SocketHelper.forward(localSocket, remoteSocket);
        } catch (Exception e) {
            // ignore
        } finally {
            context.removeLocalThread(Thread.currentThread());
        }
    }
}
