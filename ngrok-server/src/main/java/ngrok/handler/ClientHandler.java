/**
 * 处理Ngrok建立的连接
 */
package ngrok.handler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ngrok.NgdContext;
import ngrok.NgdMsg;
import ngrok.Protocol;
import ngrok.listener.TcpListener;
import ngrok.log.Logger;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class ClientHandler implements Runnable {

    private Socket socket;
    private NgdContext context;
    private Logger log;

    public ClientHandler(Socket socket, NgdContext context) {
        this.socket = socket;
        this.context = context;
        this.log = context.log;
    }

    @Override
    public void run() {
        String clientId = null;
        try (Socket socket = this.socket) {
            PacketReader pr = new PacketReader(socket, context.timeout);
            while (true) {
                String msg = pr.read();
                if (msg == null) {
                    break;
                }
                log.log("收到客户端信息：" + msg);
                Protocol protocol = GsonUtil.toBean(msg, Protocol.class);

                if ("Auth".equals(protocol.Type)) {
                    if (context.authToken != null && !context.authToken.equals(protocol.Payload.AuthToken)) {
                        SocketHelper.sendpack(socket, NgdMsg.AuthResp(null, "Auth token check failure"));
                        return;
                    }
                    clientId = Util.MD5(String.valueOf(System.currentTimeMillis()));
                    context.initClientInfo(clientId, socket);
                    SocketHelper.sendpack(socket, NgdMsg.AuthResp(clientId, null));
                    SocketHelper.sendpack(socket, NgdMsg.ReqProxy());
                    continue;
                }
                if ("RegProxy".equals(protocol.Type)) {
                    String _clientId = protocol.Payload.ClientId;
                    BlockingQueue<OuterLink> queue = context.getOuterLinkQueue(_clientId);
                    if (queue == null) {
                        // 客户端信息不存在
                        break;
                    }
                    OuterLink link = queue.poll(60, TimeUnit.SECONDS);
                    if (link == null) {
                        // 队列超时，重新发起代理连接
                        SocketHelper.sendpack(context.getControlSocket(_clientId), NgdMsg.ReqProxy());
                        break;
                    }
                    if (link.getUrl() == null) {
                        // 毒丸
                        break;
                    }
                    try {
                        SocketHelper.sendpack(socket, NgdMsg.StartProxy(link.getUrl()));
                        link.putProxySocket(socket);
                    } catch (Exception e) {
                        log.err(e.toString());
                    }
                    try (Socket outerSocket = link.getOuterSocket()) {
                        SocketHelper.sendpack(link.getControlSocket(), NgdMsg.ReqProxy());
                        SocketHelper.forward(socket, outerSocket);
                    } catch (Exception e) {
                    }
                    break;
                }

                if (clientId == null) {
                    SocketHelper.sendpack(socket, NgdMsg.Message("Please authenticate the client first"));
                    return;
                }

                if ("ReqTunnel".equals(protocol.Type)) {
                    if ("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol)) {
                        if (protocol.Payload.Hostname == null || protocol.Payload.Hostname.isEmpty()) {
                            if (protocol.Payload.Subdomain == null || protocol.Payload.Subdomain.isEmpty()) {
                                protocol.Payload.Subdomain = Util.getRandString(5);
                            }
                            protocol.Payload.Hostname = protocol.Payload.Subdomain + "." + context.domain;
                        }
                        String url = protocol.Payload.Protocol + "://" + protocol.Payload.Hostname;
                        if ("http".equals(protocol.Payload.Protocol)) {
                            if (context.httpPort == null) {
                                String error = "The http tunnel " + url + " is registration failed, becase http is disabled.";
                                SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                                break;
                            }
                            if (context.httpPort != 80) {
                                url += ":" + context.httpPort;
                            }
                        } else if ("https".equals(protocol.Payload.Protocol)) {
                            if (context.httpsPort == null) {
                                String error = "The https tunnel " + url + " is registration failed, becase https is disabled.";
                                SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                                break;
                            }
                            if (context.httpsPort != 443) {
                                url += ":" + context.httpsPort;
                            }
                        }
                        TunnelInfo tunnel = new TunnelInfo();
                        tunnel.setClientId(clientId);
                        tunnel.setControlSocket(socket);
                        context.putTunnelInfo(url, tunnel);
                        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));

                    } else if ("tcp".equals(protocol.Payload.Protocol)) {
                        String url = "tcp://" + context.domain + ":" + protocol.Payload.RemotePort;
                        if (context.getTunnelInfo(url) != null) {
                            context.getTunnelInfo(url).getTcpServerSocket().close();
                        }
                        ServerSocket serverSocket;
                        try {
                            serverSocket = SocketHelper.newServerSocket(protocol.Payload.RemotePort);
                        } catch (Exception e) {
                            String error = "The tunnel " + url + " is already registered, becase the port " + protocol.Payload.RemotePort + " is occupied.";
                            SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                            break;
                        }
                        Thread thread = new Thread(new TcpListener(serverSocket, context));
                        thread.setDaemon(true);
                        thread.start();

                        TunnelInfo tunnel = new TunnelInfo();
                        tunnel.setClientId(clientId);
                        tunnel.setControlSocket(socket);
                        tunnel.setTcpServerSocket(serverSocket);
                        context.putTunnelInfo(url, tunnel);
                        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
                    }

                } else if ("Ping".equals(protocol.Type)) {
                    SocketHelper.sendpack(socket, NgdMsg.Pong());
                }
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
        if (clientId != null) {
            log.log("客户端 %s 退出", clientId);
            context.delClientInfo(clientId);
            context.delTunnelInfo(clientId);
        }
    }
}
