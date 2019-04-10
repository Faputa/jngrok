/**
 * 处理用户建立的tcp连接
 */
package ngrok.handler;

import ngrok.NgdContext;
import ngrok.log.Logger;
import ngrok.model.ClientInfo;
import ngrok.model.Request;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class TcpHandler implements Runnable {

    private Socket socket;
    private NgdContext context;
    private Logger log = Logger.getLogger();

    public TcpHandler(Socket socket, NgdContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        log.info("收到tcp请求");
        try (Socket socket = this.socket) {
            String url = "tcp://" + context.domain + ":" + socket.getLocalPort();
            TunnelInfo tunnel = context.getTunnelInfo(url);
            if (tunnel == null) {
                return;
            }
            ClientInfo client = context.getClientInfo(tunnel.getClientId());
            Request request = new Request();
            request.setUrl(url);
            request.setOuterSocket(socket);
            client.getRequestQueue().put(request);
            try (Socket proxySocket = request.getProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                SocketHelper.forward(socket, proxySocket);
            } catch (Exception e) {
                // ignore
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
    }
}
