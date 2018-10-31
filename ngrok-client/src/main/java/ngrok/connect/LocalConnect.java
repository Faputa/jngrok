/**
 * 与本地服务器建立连接，并将本地服务器的流量转发给管道
 */
package ngrok.connect;

import java.net.Socket;

import ngrok.socket.SocketHelper;

public class LocalConnect implements Runnable {

    private Socket localSocket;
    private Socket remoteSocket;

    public LocalConnect(Socket localSocket, Socket remoteSocket) {
        this.localSocket = localSocket;
        this.remoteSocket = remoteSocket;
    }

    @Override
    public void run() {
        try (Socket localSocket = this.localSocket;
             Socket remoteSocket = this.remoteSocket) {
            SocketHelper.forward(localSocket, remoteSocket);
        } catch (Exception e) {
        }
    }
}
