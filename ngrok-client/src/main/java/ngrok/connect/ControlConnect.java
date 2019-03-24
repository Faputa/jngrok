/**
 * 与Ngrokd建立控制连接，并交换控制信息
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
import ngrok.util.Util;

import java.io.IOException;
import java.net.Socket;

public class ControlConnect implements Runnable {

    private Socket socket;
    private NgContext context;
    private Logger log;

    public ControlConnect(Socket socket, NgContext context) {
        this.socket = socket;
        this.context = context;
        this.log = context.log;
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
                log.log("收到服务器信息：" + msg);
                Protocol protocol = GsonUtil.toBean(msg, Protocol.class);

                if ("ReqProxy".equals(protocol.Type)) {
                    try {
                        Socket remoteSocket = SocketHelper.newSSLSocket(context.serverHost, context.serverPort);
                        Thread thread = new Thread(new ProxyConnect(remoteSocket, clientId, context));
                        thread.setDaemon(true);
                        thread.start();
                    } catch (Exception e) {
                        log.err(e.toString());
                    }
                } else if ("NewTunnel".equals(protocol.Type)) {
                    if (protocol.Payload.Error != null && protocol.Payload.Error.length() > 0) {
                        log.err("管道注册失败：" + protocol.Payload.Error);
                        Util.sleep(3000);
                        break;
                    }
                    log.log("管道注册成功：" + protocol.Payload.Url);
                } else if ("AuthResp".equals(protocol.Type)) {
                    if (protocol.Payload.Error != null && protocol.Payload.Error.length() > 0) {
                        log.err("客户端认证失败：" + protocol.Payload.Error);
                        Util.sleep(3000);
                        break;
                    }
                    clientId = protocol.Payload.ClientId;
                    context.setAuthOk(true);
                    log.log("客户端认证成功：" + clientId);
                    for (Tunnel tunnel : context.tunnelList) {
                        SocketHelper.sendpack(socket, NgMsg.ReqTunnel(tunnel));
                    }
                }
            }
        } catch (IOException e) {
            log.err(e.toString());
        } finally {
            context.setAuthOk(null);
        }
    }
}
