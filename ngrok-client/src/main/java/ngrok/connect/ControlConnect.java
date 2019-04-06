/**
 * 与Ngrokd建立控制连接，并交换控制信息
 */
package ngrok.connect;

import java.io.IOException;
import java.net.Socket;

import ngrok.NgContext;
import ngrok.NgMsg;
import ngrok.Protocol;
import ngrok.log.Logger;
import ngrok.model.Tunnel;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.ToolUtil;

public class ControlConnect implements Runnable {

    private Socket socket;
    private NgContext context;
    private Logger log = Logger.getLogger();

    public ControlConnect(Socket socket, NgContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        try (Socket socket = this.socket) {
            String clientId = null;
            SocketHelper.sendpack(socket, NgMsg.Auth(context.authToken));
            PacketReader pr = new PacketReader(socket);
            while (true) {
                String msg = pr.read();
                if (msg == null) {
                    break;
                }
                log.info("收到服务器信息：" + msg);
                Protocol protocol = GsonUtil.toBean(msg, Protocol.class);

                switch (protocol.Type) {
                case "ReqProxy":
                    try {
                        Socket remoteSocket = SocketHelper.newSSLSocket(context.serverHost, context.serverPort);
                        Thread thread = new Thread(new ProxyConnect(remoteSocket, clientId, context));
                        thread.setDaemon(true);
                        thread.start();
                    } catch (Exception e) {
                        log.err(e.toString());
                    }
                    continue;

                case "NewTunnel":
                    if (ToolUtil.isNotEmpty(protocol.Payload.Error)) {
                        log.err("管道注册失败：" + protocol.Payload.Error);
                        // 正常退出
                        context.setStatus(NgContext.EXITED);
                        return;
                    }
                    log.info("管道注册成功：" + protocol.Payload.Url);
                    continue;

                case "AuthResp":
                    if (ToolUtil.isNotEmpty(protocol.Payload.Error)) {
                        log.err("客户端认证失败：" + protocol.Payload.Error);
                        // 正常退出
                        context.setStatus(NgContext.EXITED);
                        return;
                    }
                    clientId = protocol.Payload.ClientId;
                    log.info("客户端认证成功：" + clientId);
                    // 认证成功
                    context.setStatus(NgContext.AUTHERIZED);
                    for (Tunnel tunnel : context.tunnelList) {
                        SocketHelper.sendpack(socket, NgMsg.ReqTunnel(tunnel));
                    }
                    continue;
                }
            }
        } catch (IOException e) {
            log.err(e.toString());
        }
        // 异常退出
        context.setStatus(NgContext.PENDING);
    }
}
