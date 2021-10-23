/**
 * 处理Ngrok建立的连接
 */
package ngrok.server.handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.common.Exitable;
import ngrok.common.Protocol;
import ngrok.server.Context;
import ngrok.server.Message;
import ngrok.server.listener.TcpListener;
import ngrok.server.model.Client;
import ngrok.server.model.Request;
import ngrok.server.model.Tunnel;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class ClientHandler implements Runnable, Exitable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private Socket socket;
    private Context context;

    public ClientHandler(Socket socket, Context context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        try (Socket socket = this.socket) {
            PacketReader pr = new PacketReader(socket, context.soTimeout);
            String msg = pr.read();
            if (msg == null) {
                return;
            }
            log.info("收到客户端信息：" + msg);
            Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
            if ("RegProxy".equals(protocol.Type)) {
                handleRegProxy(socket, protocol);
            } else if ("Auth".equals(protocol.Type)) {
                handleAuth(socket, pr, protocol);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        log.info("Connect exit: " + socket);
    }

    private void handleRegProxy(Socket socket, Protocol protocol) throws IOException {
        String clientId = protocol.Payload.ClientId;
        Client client = context.getClient(clientId);
        if (client == null) {
            log.info("没找到客户端：" + clientId);
            return;
        }
        try {
            client.addProxyHandler(this);
            handleProxy(socket, client);
        } finally {
            client.removeProxyHandler(this);
        }
    }

    private void handleProxy(Socket socket, Client client) throws IOException {
        Request request;
        try {
            request = client.pollRequest(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // 收到中断信号，退出
            return;
        }
        if (request == null) {
            // 队列超时，重新发起代理连接
            SocketHelper.sendpack(client.getControlSocket(), Message.ReqProxy());
            return;
        }
        try {
            String clientAddr = request.getPublicSocket().getRemoteSocketAddress().toString();
            SocketHelper.sendpack(socket, Message.StartProxy(request.getUrl(), clientAddr));
            request.setProxySocket(socket);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        try (Socket outerSocket = request.getPublicSocket()) {
            SocketHelper.sendpack(client.getControlSocket(), Message.ReqProxy());
            SocketHelper.forward(socket, outerSocket);
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleAuth(Socket socket, PacketReader pr, Protocol protocol) throws IOException {
        if (context.authToken != null && !context.authToken.equals(protocol.Payload.User)) {
            SocketHelper.sendpack(socket, Message.AuthResp(null, "Auth token check failure"));
            return;
        }
        String clientId = Util.MD5(String.valueOf(System.currentTimeMillis()));
        try {
            Client client = new Client(socket);
            context.putClient(clientId, client);
            SocketHelper.sendpack(socket, Message.AuthResp(clientId, null));
            SocketHelper.sendpack(socket, Message.ReqProxy());
            // 客户端注册成功，接下来接收管道注册和心跳
            while (true) {
                String msg = pr.read();
                if (msg == null) {
                    return;
                }
                log.info("收到客户端信息：" + msg);
                protocol = GsonUtil.toBean(msg, Protocol.class);
                if ("ReqTunnel".equals(protocol.Type)) {
                    try {
                        String url = handleReqTunnel(protocol, clientId);
                        SocketHelper.sendpack(socket, Message.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
                    } catch (Exception e) {
                        SocketHelper.sendpack(socket, Message.NewTunnel(null, null, null, e.getMessage()));
                        return;
                    }
                } else if ("Ping".equals(protocol.Type)) {
                    handlePing(socket, clientId);
                }
            }
        } finally {
            log.info("客户端 {} 退出", clientId);
            context.cleanClient(clientId);
        }
    }

    private String handleReqTunnel(Protocol protocol, String clientId) throws Exception {
        if ("tcp".equals(protocol.Payload.Protocol)) {
            return handleTcpReqTunnel(socket, protocol, clientId);
        }
        if ("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol)) {
            return handleHttpReqTunnel(protocol, clientId);
        }
        throw new Exception("不支持的协议：" + protocol.Type);
    }

    private String handleHttpReqTunnel(Protocol protocol, String clientId) throws Exception {
        if (Util.isEmpty(protocol.Payload.Hostname)) {
            if (Util.isEmpty(protocol.Payload.Subdomain)) {
                protocol.Payload.Subdomain = Util.getRandString(5);
            }
            protocol.Payload.Hostname = protocol.Payload.Subdomain + "." + context.domain;
        }
        String url = protocol.Payload.Protocol + "://" + protocol.Payload.Hostname;
        if ("http".equals(protocol.Payload.Protocol)) {
            if (context.httpPort == null) {
                String error = "The http tunnel " + url + " is registration failed, becase http is disabled.";
                throw new Exception(error);
            }
            if (context.httpPort != 80) {
                url += ":" + context.httpPort;
            }
        } else if ("https".equals(protocol.Payload.Protocol)) {
            if (context.httpsPort == null) {
                String error = "The https tunnel " + url + " is registration failed, becase https is disabled.";
                throw new Exception(error);
            }
            if (context.httpsPort != 443) {
                url += ":" + context.httpsPort;
            }
        }
        Tunnel tunnel = new Tunnel(clientId, null);
        context.putTunnel(url, tunnel);
        return url;
    }

    private String handleTcpReqTunnel(Socket socket, Protocol protocol, String clientId) throws Exception {
        String url = "tcp://" + context.domain + ":" + protocol.Payload.RemotePort;
        if (context.getTunnel(url) != null) {
            context.getTunnel(url).close();
            Util.safeSleep(1000);
        }
        ServerSocket serverSocket;
        try {
            serverSocket = SocketHelper.newServerSocket(protocol.Payload.RemotePort);
        } catch (Exception e) {
            String error = "The tunnel " + url + " is already registered, becase the port " + protocol.Payload.RemotePort + " is occupied.";
            throw new Exception(error);
        }
        Thread thread = new Thread(new TcpListener(serverSocket, context));
        thread.setDaemon(true);
        thread.start();
        Tunnel tunnel = new Tunnel(clientId, serverSocket);
        context.putTunnel(url, tunnel);
        return url;
    }

    private void handlePing(Socket socket, String clientId) throws IOException {
        Client client = context.getClient(clientId);
        client.setLastPingTime(System.currentTimeMillis());
        SocketHelper.sendpack(socket, Message.Pong());
    }

    @Override
    public void exit() {
        Thread.currentThread().interrupt();
        SocketHelper.safeClose(socket);
    }
}
