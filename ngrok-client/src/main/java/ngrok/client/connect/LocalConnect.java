/**
 * 与本地服务器建立连接，并将本地服务器的流量转发给管道
 */
package ngrok.client.connect;

import java.net.Socket;

import ngrok.client.model.Tunnel;
import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class LocalConnect implements Runnable, Exitable {

    private Socket localSocket;
    private Socket remoteSocket;
    private Tunnel tunnel;

    public LocalConnect(Socket localSocket, Socket remoteSocket, Tunnel tunnel) {
        this.localSocket = localSocket;
        this.remoteSocket = remoteSocket;
        this.tunnel = tunnel;
    }

    @Override
    public void run() {
        try (Socket localSocket = this.localSocket;
             Socket remoteSocket = this.remoteSocket) {
            tunnel.addLocalConnect(this);
            SocketHelper.forward(localSocket, remoteSocket);
        } catch (Exception e) {
            // ignore
        } finally {
            tunnel.removeLocalConnect(this);
        }
    }

    @Override
    public void exit() {
        SocketHelper.safeClose(localSocket);
    }
}
