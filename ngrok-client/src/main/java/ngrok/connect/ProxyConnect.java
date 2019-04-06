/**
 * 与Ngrokd建立代理连接，并将接收到的流量转发给本地服务器
 */
package ngrok.connect;

import ngrok.NgContext;
import ngrok.NgMsg;
import ngrok.Protocol;
import ngrok.log.Logger;
import ngrok.model.Tunnel;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;

import java.net.Socket;
import java.util.List;

public class ProxyConnect implements Runnable {

    private String clientId;
    private Socket socket;
    private List<Tunnel> tunnelList;
    private Logger log = Logger.getLogger();

    public ProxyConnect(Socket socket, String clientId, NgContext context) {
        this.socket = socket;
        this.clientId = clientId;
        this.tunnelList = context.tunnelList;
    }

    @Override
    public void run() {
        try (Socket socket = this.socket) {
            SocketHelper.sendpack(socket, NgMsg.RegProxy(clientId));
            PacketReader pr = new PacketReader(socket);
            while (true) {
                String msg = pr.read();
                if (msg == null) {
                    break;
                }
                log.info("收到服务器信息：" + msg);
                Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
                if ("StartProxy".equals(protocol.Type)) {
                    Tunnel tunnel = getTunnelByUrl(protocol.Payload.Url);
                    if (tunnel == null) {
                        String html = "没有找到对应的管道：" + protocol.Payload.Url;
                        log.err(html);
                        String header = "HTTP/1.0 404 Not Found\r\n";
                        header += "Content-Length: " + html.getBytes().length + "\r\n\r\n";
                        header = header + html;
                        SocketHelper.sendbuf(socket, header.getBytes());
                        break;
                    }
                    log.info("建立本地连接：[host]=%s [port]=%s", tunnel.getLocalHost(), tunnel.getLocalPort());
                    try (Socket localSocket = SocketHelper.newSocket(tunnel.getLocalHost(), tunnel.getLocalPort())) {
                        Thread thread = new Thread(new LocalConnect(localSocket, socket));
                        thread.setDaemon(true);
                        thread.start();
                        try {
                            SocketHelper.forward(socket, localSocket);
                        } catch (Exception e) {
                        }
                    } catch (Exception e) {
                        log.err("本地连接建立失败：[host]=%s [port]=%s", tunnel.getLocalHost(), tunnel.getLocalPort());
                        String html = "<html><body style=\"background-color: #97a8b9\"><div style=\"margin:auto; width:400px;padding: 20px 60px; background-color: #D3D3D3; border: 5px solid maroon;\"><h2>Tunnel ";
                        html += protocol.Payload.Url;
                        html += " unavailable</h2><p>Unable to initiate connection to <strong>";
                        html += tunnel.getLocalHost() + ":" + String.valueOf(tunnel.getLocalPort());
                        html += "</strong>. This port is not yet available for web server.</p>";
                        String header = "HTTP/1.0 502 Bad Gateway\r\n";
                        header += "Content-Type: text/html\r\n";
                        header += "Content-Length: " + html.getBytes().length;
                        header += "\r\n\r\n" + html;
                        SocketHelper.sendbuf(socket, header.getBytes());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
    }

    private Tunnel getTunnelByUrl(String url) {
        String protocol = url.split(":")[0];
        String host = url.split("//")[1];
        if ("http".equals(protocol) || "https".equals(protocol)) {
            String hostname = host.split(":")[0];
            String subdomain = hostname.split("\\.")[0];
            for (Tunnel tunnel : tunnelList) {
                if (protocol.equals(tunnel.getProtocol()) && (hostname.equals(tunnel.getHostname()) || subdomain.equals(tunnel.getSubdomain()))) {
                    return tunnel;
                }
            }
        } else if ("tcp".equals(protocol)) {
            String remotePort = host.split(":")[1];
            for (Tunnel tunnel : tunnelList) {
                if ("tcp".equals(tunnel.getProtocol()) && remotePort.equals(String.valueOf(tunnel.getRemotePort()))) {
                    return tunnel;
                }
            }
        }
        return null;
    }
}
