/**
 * 处理Ngrok建立的连接
 */
package ngrok.handler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import ngrok.NgdContext;
import ngrok.NgdMsg;
import ngrok.Protocol;
import ngrok.listener.TcpListener;
import ngrok.log.Logger;
import ngrok.model.ClientInfo;
import ngrok.model.Request;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.ToolUtil;

public class ClientHandler implements Runnable {

    private Socket socket;
    private NgdContext context;
    private Logger log = Logger.getLogger();

    public ClientHandler(Socket socket, NgdContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        String clientId = null;
        try (Socket socket = this.socket) {
            PacketReader pr = new PacketReader(socket, context.timeout);
            String msg = pr.read();
            if (msg == null) {
                return;
            }
            log.info("收到客户端信息：" + msg);
            Protocol protocol = GsonUtil.toBean(msg, Protocol.class);

            if ("RegProxy".equals(protocol.Type)) {
                String _clientId = protocol.Payload.ClientId;
                ClientInfo client = context.getClientInfo(_clientId);
                if (client == null) {
                    // 客户端信息不存在
                    return;
                }
                Request request = client.getRequestQueue().poll(60, TimeUnit.SECONDS);
                if (request == null) {
                    // 队列超时，重新发起代理连接
                    SocketHelper.sendpack(client.getControlSocket(), NgdMsg.ReqProxy());
                    return;
                }
                if (request.getUrl() == null) {
                    // 毒丸
                    return;
                }
                try {
                    SocketHelper.sendpack(socket, NgdMsg.StartProxy(request.getUrl()));
                    request.setProxySocket(socket);
                } catch (Exception e) {
                    log.err(e.toString());
                }
                try (Socket outerSocket = request.getOuterSocket()) {
                    SocketHelper.sendpack(request.getControlSocket(), NgdMsg.ReqProxy());
                    SocketHelper.forward(socket, outerSocket);
                } catch (Exception e) {
                    // ignore
                }
                return;
            }

            if ("Auth".equals(protocol.Type)) {
                if (context.authToken != null && !context.authToken.equals(protocol.Payload.AuthToken)) {
                    SocketHelper.sendpack(socket, NgdMsg.AuthResp(null, "Auth token check failure"));
                    return;
                }
                clientId = ToolUtil.MD5(String.valueOf(System.currentTimeMillis()));
                context.createClientInfo(clientId, socket);
                SocketHelper.sendpack(socket, NgdMsg.AuthResp(clientId, null));
                SocketHelper.sendpack(socket, NgdMsg.ReqProxy());

                while (true) {
                    msg = pr.read();
                    if (msg == null) {
                        break;
                    }
                    log.info("收到客户端信息：" + msg);
                    protocol = GsonUtil.toBean(msg, Protocol.class);

                    if ("ReqTunnel".equals(protocol.Type)) {
                        if ("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol)) {
                            if (ToolUtil.isEmpty(protocol.Payload.Hostname)) {
                                if (ToolUtil.isEmpty(protocol.Payload.Subdomain)) {
                                    protocol.Payload.Subdomain = ToolUtil.getRandString(5);
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
                            context.createTunnelInfo(url, clientId, null);
                            SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
                        } else if ("tcp".equals(protocol.Payload.Protocol)) {
                            String url = "tcp://" + context.domain + ":" + protocol.Payload.RemotePort;
                            if (context.getTunnelInfo(url) != null) {
                                context.getTunnelInfo(url).close();
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
                            context.createTunnelInfo(url, clientId, serverSocket);
                            SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
                        }

                    } else if ("Ping".equals(protocol.Type)) {
                        SocketHelper.sendpack(socket, NgdMsg.Pong());
                    }
                }
            }
        } catch (Exception e) {
            log.err(e.toString());
        }
        if (clientId != null) {
            log.info("客户端 %s 退出", clientId);
            context.deleteClientInfo(clientId);
            context.deleteTunnelInfo(clientId);
        }
    }
}
