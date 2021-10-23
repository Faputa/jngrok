/**
 * 与Ngrokd建立控制连接，并交换控制信息
 */
package ngrok.client.connect;

import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.client.Context;
import ngrok.client.Message;
import ngrok.client.model.Tunnel;
import ngrok.common.Protocol;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class ControlConnect {

    private static final Logger log = LoggerFactory.getLogger(ControlConnect.class);

    private Context context;

    public ControlConnect(Context context) {
        this.context = context;
    }

    public void run() throws Exception {
        Socket socket = context.connectServer();
        try (Socket _socket = socket) {
            SocketHelper.sendpack(socket, Message.Auth(context.authToken));
            PacketReader pr = new PacketReader(socket, context.soTimeout);
            String msg = pr.read();
            if (msg == null) {
                // 服务器主动关闭连接，正常退出
                return;
            }
            log.info("收到服务器信息：" + msg);
            Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
            if ("AuthResp".equals(protocol.Type)) {
                handleAuthResp(socket, pr, protocol);
            }
        } finally {
            log.info("Connect exit: " + socket);
            context.clean();
        }
    }

    private void handleAuthResp(Socket socket, PacketReader pr, Protocol protocol) throws Exception {
        if (Util.isNotEmpty(protocol.Payload.Error)) {
            log.warn("客户端认证失败：" + protocol.Payload.Error);
            return;
        }
        String clientId = protocol.Payload.ClientId;
        log.info("客户端认证成功：" + clientId);
        for (Tunnel tunnel : context.tunnelList) {
            SocketHelper.sendpack(socket, Message.ReqTunnel(tunnel));
        }
        // 客户端注册成功，接下来处理数据代理、管道注册和心跳
        while (true) {
            String msg;
            try {
                msg = pr.read(context.pingTime);
            } catch (SocketTimeoutException e) {
                SocketHelper.sendpack(socket, Message.Ping());
                continue;
            }

            if (msg == null) {
                // 服务器主动关闭连接，正常退出
                return;
            }
            log.info("收到服务器信息：" + msg);
            protocol = GsonUtil.toBean(msg, Protocol.class);

            if ("ReqProxy".equals(protocol.Type)) {
                Thread thread = new Thread(new ProxyConnect(clientId, context));
                thread.setDaemon(true);
                thread.start();
            } else if ("NewTunnel".equals(protocol.Type)) {
                if (Util.isNotEmpty(protocol.Payload.Error)) {
                    log.warn("管道注册失败：" + protocol.Payload.Error);
                    return;
                }
                log.info("管道注册成功：" + protocol.Payload.Url);
            } else if ("Pong".equals(protocol.Type)) {
                // do nothing
            }
        }
    }
}
