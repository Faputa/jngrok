/**
 * 处理用户建立的http、https连接
 */
package ngrok.server;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;
import ngrok.util.ByteUtil;

public class HttpServer implements Runnable {

    private Socket socket;
    private NgdContext context;
    private String protocol;
    private Logger log;

    public HttpServer(Socket socket, NgdContext context, String protocol) {
        this.socket = socket;
        this.context = context;
        this.protocol = protocol;
        this.log = context.log;
    }

    @Override
    public void run() {
        log.log("收到%s请求", protocol);
        try (Socket socket = this.socket) {
            byte[] buf = SocketHelper.recvbuf(socket);
            if (buf != null) {
                while (true) {
                    Map<String, String> head = SocketHelper.readHttpHead(buf);
                    if (head == null) {
                        byte[] _buf = SocketHelper.recvbuf(socket);
                        if (_buf == null) {
                            break;
                        }
                        buf = ByteUtil.concat(buf, _buf);
                        continue;
                    }
                    String url = protocol + "://" + head.get("Host");
                    TunnelInfo tunnel = context.getTunnelInfo(url);
                    if (tunnel == null) {
                        String html = "Tunnel " + head.get("Host") + " not found";
                        String header = "HTTP/1.0 404 Not Found\r\n";
                        header += "Content-Length: " + html.getBytes().length + "\r\n\r\n";
                        header = header + html;
                        SocketHelper.sendbuf(socket, header.getBytes());
                        break;
                    }
                    OuterLink outerLink = new OuterLink();
                    outerLink.setUrl(url);
                    outerLink.setOuterSocket(socket);
                    outerLink.setControlSocket(tunnel.getControlSocket());
                    context.getOuterLinkQueue(tunnel.getClientId()).put(outerLink);
                    try (Socket proxySocket = outerLink.pollProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                        SocketHelper.sendbuf(proxySocket, buf);
                        SocketHelper.forward(socket, proxySocket);
                    } catch (Exception e) {
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
    }
}
