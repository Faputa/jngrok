/**
 * 处理用户建立的tcp连接
 */
package ngrok.handler;

import ngrok.NgdContext;
import ngrok.model.ClientInfo;
import ngrok.model.Request;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TcpHandler.class);

    private Socket socket;
    private NgdContext context;

    public TcpHandler(Socket socket, NgdContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        log.info("收到tcp请求");
        try (Socket socket = this.socket) {
            String url = "tcp://" + context.domain + ":" + socket.getLocalPort();
            TunnelInfo tunnelInfo = context.getTunnelInfo(url);
            if (tunnelInfo == null) {
                return;
            }
            ClientInfo clientInfo = context.getClientInfo(tunnelInfo.getClientId());
            try {
                Request request = new Request(url, socket);
                clientInfo.addOuterThread(Thread.currentThread());
                clientInfo.putRequest(request);
                try (Socket proxySocket = request.getProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                    SocketHelper.forward(socket, proxySocket);
                } catch (Exception e) {
                    // ignore
                }
            } finally {
                clientInfo.removeOuterThread(Thread.currentThread());
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
    }
}
