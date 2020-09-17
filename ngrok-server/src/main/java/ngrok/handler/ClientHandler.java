/**
 * 处理Ngrok建立的连接
 */
package ngrok.handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.ExitConnectException;
import ngrok.NgdContext;
import ngrok.NgdMsg;
import ngrok.Protocol;
import ngrok.listener.TcpListener;
import ngrok.model.ClientInfo;
import ngrok.model.Request;
import ngrok.model.TunnelInfo;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private Socket socket;
    private NgdContext context;

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
        } catch (ExitConnectException e) {
            // ignore
        } catch (Exception e) {
            log.error(e.toString());
        }
        log.info("Connect exit: " + socket);
    }

    private Protocol readProtocol(Socket socket) throws ExitConnectException, IOException {
        PacketReader pr = new PacketReader(socket, context.soTimeout);
        String msg = pr.read();
        if (msg == null) {
            throw new ExitConnectException(socket);
        }
        log.info("收到客户端信息：" + msg);
        Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
        return protocol;
    }

    private void hanldeRegProxy(Socket socket, Protocol protocol) throws ExitConnectException, IOException {
        String clientId = protocol.Payload.ClientId;
        ClientInfo clientInfo = context.getClientInfo(clientId);
        if (clientInfo == null) {
            throw new ExitConnectException(socket);
        }
        try {
            clientInfo.addProxyThread(Thread.currentThread());
            processProxy(socket, clientInfo);
        } finally {
            clientInfo.removeProxyThread(Thread.currentThread());
        }
    }

    private void processProxy(Socket socket, ClientInfo clientInfo) throws ExitConnectException, IOException {
        Request request;
        try {
            request = clientInfo.pollRequest(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ExitConnectException(socket);
        }
        if (request == null) {
            // 队列超时，重新发起代理连接
            SocketHelper.sendpack(clientInfo.getControlSocket(), NgdMsg.ReqProxy());
            throw new ExitConnectException(socket);
        }
        try {
            SocketHelper.sendpack(socket, NgdMsg.StartProxy(request.getUrl()));
            request.setProxySocket(socket);
        } catch (Exception e) {
            log.error(e.toString());
        }
        try (Socket outerSocket = request.getOuterSocket()) {
            SocketHelper.sendpack(clientInfo.getControlSocket(), NgdMsg.ReqProxy());
            SocketHelper.forward(socket, outerSocket);
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleAuth(Socket socket, Protocol protocol) throws ExitConnectException, IOException {
        if (context.authToken != null && !context.authToken.equals(protocol.Payload.AuthToken)) {
            SocketHelper.sendpack(socket, NgdMsg.AuthResp(null, "Auth token check failure"));
            throw new ExitConnectException(socket);
        }
        String clientId = Util.MD5(String.valueOf(System.currentTimeMillis()));
        try {
            ClientInfo clientInfo = new ClientInfo(socket);
            context.putClientInfo(clientId, clientInfo);
            SocketHelper.sendpack(socket, NgdMsg.AuthResp(clientId, null));
            SocketHelper.sendpack(socket, NgdMsg.ReqProxy());
            // 客户端注册成功，接下来接收管道注册和心跳
            while (true) {
                protocol = readProtocol(socket);
                if ("ReqTunnel".equals(protocol.Type)) {
                    handelReqTunnel(socket, protocol, clientId);
                } else if ("Ping".equals(protocol.Type)) {
                    handlePing(socket, clientId);
                }
            }
        } finally {
            log.info("客户端 {} 退出", clientId);
            context.cleanClient(clientId);
        }
    }

    private void handelReqTunnel(Socket socket, Protocol protocol, String clientId) throws ExitConnectException, IOException {
        if ("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol)) {
            handelHttpReqTunnel(socket, protocol, clientId);
        } else if ("tcp".equals(protocol.Payload.Protocol)) {
            handelTcpReqTunnel(socket, protocol, clientId);
        }
    }

    private void handelHttpReqTunnel(Socket socket, Protocol protocol, String clientId) throws ExitConnectException, IOException {
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
                throw new ExitConnectException(socket);
            }
            if (context.httpPort != 80) {
                url += ":" + context.httpPort;
            }
        } else if ("https".equals(protocol.Payload.Protocol)) {
            if (context.httpsPort == null) {
                String error = "The https tunnel " + url + " is registration failed, becase https is disabled.";
                SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
                throw new ExitConnectException(socket);
            }
            if (context.httpsPort != 443) {
                url += ":" + context.httpsPort;
            }
        }
        TunnelInfo tunnelInfo = new TunnelInfo(clientId, null);
        context.putTunnelInfo(url, tunnelInfo);
        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
    }

    private void handelTcpReqTunnel(Socket socket, Protocol protocol, String clientId) throws ExitConnectException, IOException {
        String url = "tcp://" + context.domain + ":" + protocol.Payload.RemotePort;
        if (context.getTunnelInfo(url) != null) {
            context.getTunnelInfo(url).close();
            Util.safeSleep(1000);
        }
        ServerSocket serverSocket;
        try {
            serverSocket = SocketHelper.newServerSocket(protocol.Payload.RemotePort);
        } catch (Exception e) {
            String error = "The tunnel " + url + " is already registered, becase the port " + protocol.Payload.RemotePort + " is occupied.";
            SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
            throw new ExitConnectException(socket);
        }
        Thread thread = new Thread(new TcpListener(serverSocket, context));
        thread.setDaemon(true);
        thread.start();
        TunnelInfo tunnelInfo = new TunnelInfo(clientId, serverSocket);
        context.putTunnelInfo(url, tunnelInfo);
        SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
    }

    private void handlePing(Socket socket, String clientId) throws IOException {
        ClientInfo clientInfo = context.getClientInfo(clientId);
        clientInfo.setLastPingTime(System.currentTimeMillis());
        SocketHelper.sendpack(socket, NgdMsg.Pong());
    }
}
