/**
 * 处理用户建立的http、https连接
 */
package ngrok.server.handler;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.common.Exitable;
import ngrok.server.Context;
import ngrok.server.model.Client;
import ngrok.server.model.Request;
import ngrok.server.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.ByteUtil;

public class HttpHandler implements Runnable, Exitable {

    private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);

    private Socket socket;
    private Context context;
    private String protocol;

    public HttpHandler(Socket socket, Context context, String protocol) {
        this.socket = socket;
        this.context = context;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        log.info("收到{}请求", protocol);
        try (Socket socket = this.socket) {
            byte[] buf = SocketHelper.recvbuf(socket);
            if (buf == null) {
                return;
            }
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
                Tunnel tunnel = context.getTunnel(url);
                if (tunnel == null) {
                    String html = "Tunnel " + head.get("Host") + " not found";
                    String header = "HTTP/1.0 404 Not Found\r\n";
                    header += "Content-Length: " + html.getBytes().length + "\r\n\r\n";
                    header = header + html;
                    SocketHelper.sendbuf(socket, header.getBytes());
                    break;
                }
                Client client = context.getClient(tunnel.getClientId());
                try {
                    Request request = new Request(url, socket);
                    tunnel.addOuterHandler(this);
                    client.putRequest(request);
                    try (Socket proxySocket = request.getProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                        SocketHelper.sendbuf(proxySocket, buf);
                        SocketHelper.forward(socket, proxySocket);
                    } catch (Exception e) {
                        // ignore
                    }
                } finally {
                    tunnel.removeOuterHandler(this);
                }
                break;
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    @Override
    public void exit() {
        Thread.currentThread().interrupt();
        SocketHelper.safeClose(socket);
    }
}
