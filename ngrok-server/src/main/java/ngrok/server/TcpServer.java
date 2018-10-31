/**
 * 处理用户建立的tcp连接
 */
package ngrok.server;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;

public class TcpServer implements Runnable {

    private Socket socket;
    private NgdContext context;
    private Logger log;

    public TcpServer(Socket socket, NgdContext context) {
        this.socket = socket;
        this.context = context;
        this.log = context.log;
    }

    @Override
    public void run() {
        log.log("收到tcp请求");
        try (Socket socket = this.socket) {
            String url = "tcp://" + context.domain + ":" + socket.getLocalPort();
            TunnelInfo tunnel = context.getTunnelInfo(url);
            if (tunnel != null) {
                OuterLink outerLink = new OuterLink();
                outerLink.setUrl(url);
                outerLink.setOuterSocket(socket);
                outerLink.setControlSocket(tunnel.getControlSocket());
                context.getOuterLinkQueue(tunnel.getClientId()).put(outerLink);
                try (Socket proxySocket = outerLink.pollProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                    SocketHelper.forward(socket, proxySocket);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
    }
}
