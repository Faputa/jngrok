/**
 * 与Ngrokd建立代理连接，并将接收到的流量转发给本地服务器
 */
package ngrok.client.connect;

import java.io.IOException;
import java.net.Socket;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.client.Context;
import ngrok.client.Message;
import ngrok.client.model.Tunnel;
import ngrok.common.Exitable;
import ngrok.common.Protocol;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;

public class ProxyConnect implements Runnable, Exitable {

    private static final Logger log = LoggerFactory.getLogger(ProxyConnect.class);

    private String clientId;
    private Socket socket;
    private Context context;

    public ProxyConnect(String clientId, Context context) {
        this.clientId = clientId;
        this.context = context;
    }

    @Override
    public void run() {
        try (Socket socket = context.connectServer()) {
            this.socket = socket;
            context.addProxyConnect(this);
            SocketHelper.sendpack(socket, Message.RegProxy(clientId));
            PacketReader pr = new PacketReader(socket, context.soTimeout);
            String msg = pr.read();
            if (msg == null) {
                return;
            }
            log.info("收到服务器信息：" + msg);
            Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
            if ("StartProxy".equals(protocol.Type)) {
                handleStartProxy(socket, protocol, pr);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        } finally {
            context.removeProxyConnect(this);
        }
    }

    private void handleStartProxy(Socket socket, Protocol protocol, PacketReader pr) throws IOException {
        Tunnel tunnel = getTunnelByUrl(protocol.Payload.Url);
        if (tunnel == null) {
            String html = "没有找到对应的管道：" + protocol.Payload.Url;
            log.warn(html);
            String header = "HTTP/1.0 404 Not Found\r\n";
            header += "Content-Length: " + html.getBytes().length + "\r\n\r\n";
            header = header + html;
            SocketHelper.sendbuf(socket, header.getBytes());
            return;
        }
        log.info("建立本地连接：[host]={} [port]={}", tunnel.localHost, tunnel.localPort);
        try (Socket localSocket = SocketHelper.newSocket(tunnel.localHost, tunnel.localPort, context.soTimeout)) {
            Thread thread = new Thread(new LocalConnect(localSocket, socket, tunnel));
            thread.setDaemon(true);
            thread.start();
            try {
                SocketHelper.sendbuf(localSocket, pr.getBuf());
                SocketHelper.forward(socket, localSocket);
            } catch (Exception e) {
                // ignore
            }
        } catch (IOException e) {
            log.warn("本地连接建立失败：[host]={} [port]={}", tunnel.localHost, tunnel.localPort);
            String html = FileUtil.readTextFile("classpath:502.html")
            .replace("{url}", protocol.Payload.Url)
            .replace("{localHost}", tunnel.localHost)
            .replace("{localPort}", String.valueOf(tunnel.localPort));
            String doc = "HTTP/1.0 502 Bad Gateway\r\n";
            doc += "Content-Type: text/html\r\n";
            doc += "Content-Length: " + html.getBytes().length;
            doc += "\r\n\r\n" + html;
            SocketHelper.sendbuf(socket, doc.getBytes());
        }
    }

    @Nullable
    private Tunnel getTunnelByUrl(String url) {
        String protocol = url.split(":")[0];
        String host = url.split("//")[1];
        if ("http".equals(protocol) || "https".equals(protocol)) {
            String hostname = host.split(":")[0];
            String subdomain = hostname.split("\\.")[0];
            for (Tunnel tunnel : context.tunnelList) {
                if (protocol.equals(tunnel.protocol) && (hostname.equals(tunnel.hostname) || subdomain.equals(tunnel.subdomain))) {
                    return tunnel;
                }
            }
        } else if ("tcp".equals(protocol)) {
            String remotePort = host.split(":")[1];
            for (Tunnel tunnel : context.tunnelList) {
                if ("tcp".equals(tunnel.protocol) && remotePort.equals(String.valueOf(tunnel.remotePort))) {
                    return tunnel;
                }
            }
        }
        return null;
    }

    @Override
    public void exit() {
        SocketHelper.safeClose(socket);
    }
}
