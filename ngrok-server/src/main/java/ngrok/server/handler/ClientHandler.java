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

import ngrok.common.ExitConnectException;
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
            Protocol protocol = readProtocol(pr);
            if ("RegProxy".equals(protocol.Type)) {
                hanldeRegProxy(socket, protocol);
            } else if ("Auth".equals(protocol.Type)) {
                handleAuth(socket, pr, protocol);
            }
        } catch (ExitConnectException e) {
            // ignore
        } catch (Exception e) {
            log.error(e.toString());
        }
        log.info("Connect exit: " + socket);
    }

    private Protocol readProtocol(PacketReader pr) throws ExitConnectException, IOException {
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
        Client client = context.getClient(clientId);
        if (client == null) {
            throw new ExitConnectException(socket);
        }
        try {
            client.addProxyHandler(this);
            processProxy(socket, client);
        } finally {
            client.removeProxyHandler(this);
        }
    }

    private void processProxy(Socket socket, Client client) throws ExitConnectException, IOException {
        Request request;
        try {
            request = client.pollRequest(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ExitConnectException(socket);
        }
        if (request == null) {
            // 队列超时，重新发起代理连接
            SocketHelper.sendpack(client.getControlSocket(), Message.ReqProxy());
            throw new ExitConnectException(socket);
        }
        try {
            SocketHelper.sendpack(socket, Message.StartProxy(request.getUrl()));
            request.setProxySocket(socket);
        } catch (Exception e) {
            log.error(e.toString());
        }
        try (Socket outerSocket = request.getOuterSocket()) {
            SocketHelper.sendpack(client.getControlSocket(), Message.ReqProxy());
            SocketHelper.forward(socket, outerSocket);
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleAuth(Socket socket, PacketReader pr, Protocol protocol) throws ExitConnectException, IOException {
        if (context.authToken != null && !context.authToken.equals(protocol.Payload.AuthToken)) {
            SocketHelper.sendpack(socket, Message.AuthResp(null, "Auth token check failure"));
            throw new ExitConnectException(socket);
        }
        String clientId = Util.MD5(String.valueOf(System.currentTimeMillis()));
        try {
            Client client = new Client(socket);
            context.putClient(clientId, client);
            SocketHelper.sendpack(socket, Message.AuthResp(clientId, null));
            SocketHelper.sendpack(socket, Message.ReqProxy());
            // 客户端注册成功，接下来接收管道注册和心跳
            while (true) {
                protocol = readProtocol(pr);
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
                SocketHelper.sendpack(socket, Message.NewTunnel(null, null, null, error));
                throw new ExitConnectException(socket);
            }
            if (context.httpPort != 80) {
                url += ":" + context.httpPort;
            }
        } else if ("https".equals(protocol.Payload.Protocol)) {
            if (context.httpsPort == null) {
                String error = "The https tunnel " + url + " is registration failed, becase https is disabled.";
                SocketHelper.sendpack(socket, Message.NewTunnel(null, null, null, error));
                throw new ExitConnectException(socket);
            }
            if (context.httpsPort != 443) {
                url += ":" + context.httpsPort;
            }
        }
        Tunnel tunnel = new Tunnel(clientId, null);
        context.putTunnel(url, tunnel);
        SocketHelper.sendpack(socket, Message.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
    }

    private void handelTcpReqTunnel(Socket socket, Protocol protocol, String clientId) throws ExitConnectException, IOException {
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
            SocketHelper.sendpack(socket, Message.NewTunnel(null, null, null, error));
            throw new ExitConnectException(socket);
        }
        Thread thread = new Thread(new TcpListener(serverSocket, context));
        thread.setDaemon(true);
        thread.start();
        Tunnel tunnel = new Tunnel(clientId, serverSocket);
        context.putTunnel(url, tunnel);
        SocketHelper.sendpack(socket, Message.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
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
