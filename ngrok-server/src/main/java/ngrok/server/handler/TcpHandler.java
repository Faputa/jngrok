/**
 * 处理用户建立的tcp连接
 */
package ngrok.server.handler;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.common.Exitable;
import ngrok.server.Context;
import ngrok.server.model.Client;
import ngrok.server.model.Request;
import ngrok.server.model.Tunnel;
import ngrok.socket.SocketHelper;

public class TcpHandler implements Runnable, Exitable {

    private static final Logger log = LoggerFactory.getLogger(TcpHandler.class);

    private Socket socket;
    private Context context;

    public TcpHandler(Socket socket, Context context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        log.info("收到tcp请求");
        try (Socket socket = this.socket) {
            String url = "tcp://" + context.domain + ":" + socket.getLocalPort();
            Tunnel tunnel = context.getTunnel(url);
            if (tunnel == null) {
                return;
            }
            Client client = context.getClient(tunnel.getClientId());
            try {
                Request request = new Request(url, socket);
                tunnel.addOuterHandler(this);
                client.putRequest(request);
                try (Socket proxySocket = request.getProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                    SocketHelper.forward(socket, proxySocket);
                } catch (Exception e) {
                    // ignore
                }
            } finally {
                tunnel.removeOuterHandler(this);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    @Override
    public void exit() {
        Thread.currentThread().interrupt();
        SocketHelper.safeClose(socket);
    }
}
