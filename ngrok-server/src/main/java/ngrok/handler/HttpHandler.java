/**
 * 处理用户建立的http、https连接
 */
package ngrok.handler;

import ngrok.NgdContext;
import ngrok.log.Logger;
import ngrok.model.ClientInfo;
import ngrok.model.Request;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;
import ngrok.util.ByteUtil;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpHandler implements Runnable {

    private Socket socket;
    private NgdContext context;
    private String protocol;
    private Logger log = Logger.getLogger();

    public HttpHandler(Socket socket, NgdContext context, String protocol) {
        this.socket = socket;
        this.context = context;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        log.info("收到%s请求", protocol);
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
                TunnelInfo tunnel = context.getTunnelInfo(url);
                if (tunnel == null) {
                    String html = "Tunnel " + head.get("Host") + " not found";
                    String header = "HTTP/1.0 404 Not Found\r\n";
                    header += "Content-Length: " + html.getBytes().length + "\r\n\r\n";
                    header = header + html;
                    SocketHelper.sendbuf(socket, header.getBytes());
                    break;
                }
                ClientInfo client = context.getClientInfo(tunnel.getClientId());
                Request request = new Request();
                request.setUrl(url);
                request.setOuterSocket(socket);
                client.getRequestQueue().put(request);
                try (Socket proxySocket = request.getProxySocket(60, TimeUnit.SECONDS)) { // 最多等待60秒
                    SocketHelper.sendbuf(proxySocket, buf);
                    SocketHelper.forward(socket, proxySocket);
                } catch (Exception e) {
                    // ignore
                }
                break;
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
    }
}
