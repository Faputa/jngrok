/**
 * 处理Ngrok建立的连接
 */
package ngrok.handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import ngrok.CloseSocketSignal;
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
import ngrok.util.Util;

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
        try (Socket socket = this.socket) {
            Protocol protocol = readProtocol(socket);
            if ("RegProxy".equals(protocol.Type)) {
                hanldeRegProxy(socket, protocol);
            } else if ("Auth".equals(protocol.Type)) {
                handleAuth(socket, protocol);
            }
        } catch (CloseSocketSignal e) {
            // ignore
        } catch (Exception e) {
            log.err(e.toString());
        }
        log.info("Connect exit: " + socket);
    }

    private Protocol readProtocol(Socket socket) throws Exception {
        PacketReader pr = new PacketReader(socket, context.timeout);
        String msg = pr.read();
        if (msg == null) {
            throw new CloseSocketSignal(socket);
        }
        log.info("收到客户端信息：" + msg);
        Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
        return protocol;
    }

    private void hanldeRegProxy(Socket socket, Protocol protocol) throws Exception {
        String clientId = protocol.Payload.ClientId;
        ClientInfo client = context.getClientInfo(clientId);
        if (client == null) {
            // 客户端信息不存在
            throw new CloseSocketSignal(socket);
        }
        Request request = client.getRequestQueue().poll(60, TimeUnit.SECONDS);
        if (request == null) {
            // 队列超时，重新发起代理连接
            SocketHelper.sendpack(client.getControlSocket(), NgdMsg.ReqProxy());
            throw new CloseSocketSignal(socket);
        }
        if (request.getUrl() == null) {
            // 毒丸
            throw new CloseSocketSignal(socket);
        }
        try {
            SocketHelper.sendpack(socket, NgdMsg.StartProxy(request.getUrl()));
            request.setProxySocket(socket);
        } catch (Exception e) {
            log.err(e.toString());
        }
        try (Socket outerSocket = request.getOuterSocket()) {
            try {
                client.addProxySocket(socket);
                client.addOuterSocket(outerSocket);
                SocketHelper.sendpack(client.getControlSocket(), NgdMsg.ReqProxy());
                SocketHelper.forward(socket, outerSocket);
            } finally {
                client.removeProxySocket(socket);
                client.removeOuterSocket(outerSocket);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleAuth(Socket socket, Protocol protocol) throws Exception {
        if (context.authToken != null && !context.authToken.equals(protocol.Payload.AuthToken)) {
            SocketHelper.sendpack(socket, NgdMsg.AuthResp(null, "Auth token check failure"));
            throw new CloseSocketSignal(socket);
        }
        String clientId = Util.MD5(String.valueOf(System.currentTimeMillis()));
        try {
            context.createClientInfo(clientId, socket);
            SocketHelper.sendpack(socket, NgdMsg.AuthResp(clientId, null));
            SocketHelper.sendpack(socket, NgdMsg.ReqProxy());
            // 客户端注册成功，接下来接收管道注册和心跳
            while (true) {
                protocol = readProtocol(socket);
                if ("ReqTunnel".equals(protocol.Type)) {
                    handelReqTunnel(socket, protocol, clientId);
                } else if ("Ping".equals(protocol.Type)) {
                    handlePing(socket);
                }
            }
        } finally {
            log.info("客户端 %s 退出", clientId);
            context.deleteClientInfo(clientId);
            context.deleteTunnelInfo(clientId);
        }
    }

    private void handelReqTunnel(Socket socket, Protocol protocol, String clientId) throws Exception {
        if ("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol)) {
            handelHttpReqTunnel(socket, protocol, clientId);
            return;
        }
        if ("tcp".equals(protocol.Payload.Protocol)) {
            handelTcpReqTunnel(socket, protocol, clientId);
            return;
        }
    }

    private void handelHttpReqTunnel(Socket socket, Protocol protocol, String clientId) throws Exception {
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
                SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                throw new CloseSocketSignal(socket);
            }
            if (context.httpPort != 80) {
                url += ":" + context.httpPort;
            }
        } else if ("https".equals(protocol.Payload.Protocol)) {
            if (context.httpsPort == null) {
                String error = "The https tunnel " + url + " is registration failed, becase https is disabled.";
                SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                throw new CloseSocketSignal(socket);
            }
            if (context.httpsPort != 443) {
                url += ":" + context.httpsPort;
            }
        }
        context.createTunnelInfo(url, clientId, null);
        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
    }

    private void handelTcpReqTunnel(Socket socket, Protocol protocol, String clientId) throws Exception {
        String url = "tcp://" + context.domain + ":" + protocol.Payload.RemotePort;
        if (context.getTunnelInfo(url) != null) {
            context.getTunnelInfo(url).close();
            Util.sleep(1000);
        }
        ServerSocket serverSocket;
        try {
            serverSocket = SocketHelper.newServerSocket(protocol.Payload.RemotePort);
        } catch (Exception e) {
            String error = "The tunnel " + url + " is already registered, becase the port " + protocol.Payload.RemotePort + " is occupied.";
            SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
            throw new CloseSocketSignal(socket);
        }
        Thread thread = new Thread(new TcpListener(serverSocket, context));
        thread.setDaemon(true);
        thread.start();
        context.createTunnelInfo(url, clientId, serverSocket);
        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
    }

    private void handlePing(Socket socket) throws IOException {
        SocketHelper.sendpack(socket, NgdMsg.Pong());
    }
}
