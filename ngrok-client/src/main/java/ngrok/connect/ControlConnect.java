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
            SocketHelper.sendpack(socket, NgMsg.Auth(context.authToken));
            PacketReader pr = new PacketReader(socket);
            String msg = pr.read();
            if (msg == null) {
                // 服务器主动关闭连接，正常退出
                context.setStatus(NgContext.EXITED);
                return;
            }
            log.info("收到服务器信息：" + msg);
            Protocol protocol = GsonUtil.toBean(msg, Protocol.class);

            if ("AuthResp".equals(protocol.Type)) {
                if (ToolUtil.isNotEmpty(protocol.Payload.Error)) {
                    log.err("客户端认证失败：" + protocol.Payload.Error);
                    // 正常退出
                    context.setStatus(NgContext.EXITED);
                    return;
                }
                String clientId = protocol.Payload.ClientId;
                log.info("客户端认证成功：" + clientId);

                // 认证成功
                context.setStatus(NgContext.AUTHERIZED);
                for (Tunnel tunnel : context.tunnelList) {
                    SocketHelper.sendpack(socket, NgMsg.ReqTunnel(tunnel));
                }

                while (true) {
                    msg = pr.read();
                    if (msg == null) {
                        // 服务器主动关闭连接，正常退出
                        context.setStatus(NgContext.EXITED);
                        return;
                    }
                    log.info("收到服务器信息：" + msg);
                    protocol = GsonUtil.toBean(msg, Protocol.class);

                    if ("ReqProxy".equals(protocol.Type)) {
                        try {
                            Socket remoteSocket = SocketHelper.newSSLSocket(context.serverHost, context.serverPort);
                            Thread thread = new Thread(new ProxyConnect(remoteSocket, clientId, context));
                            thread.setDaemon(true);
                            thread.start();
                        } catch (Exception e) {
                            log.err(e.toString());
                        }
                    }

                    else if ("NewTunnel".equals(protocol.Type)) {
                        if (ToolUtil.isNotEmpty(protocol.Payload.Error)) {
                            log.err("管道注册失败：" + protocol.Payload.Error);
                            // 正常退出
                            context.setStatus(NgContext.EXITED);
                            return;
                        }
                        log.info("管道注册成功：" + protocol.Payload.Url);
                    }

                    else if ("Pong".equals(protocol.Type)) {
                        // do nothing
                    }
                }
            }
        } catch (IOException e) {
            log.err(e.toString());
        }
        // 异常退出
        context.setStatus(NgContext.PENDING);
    }
}
