package ngrok.client;

import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.client.connect.ControlConnect;
import ngrok.client.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class Ngrok {

    private static final Logger log = LoggerFactory.getLogger(Ngrok.class);

    private Context context = new Context();
    private long pingTime = 10000;// 心跳包周期默认为10秒

    public void setServerHost(String serverHost) {
        context.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        context.serverPort = serverPort;
    }

    public void setTunnelList(List<Tunnel> tunnelList) {
        context.tunnelList = tunnelList;
    }

    public void setAuthToken(String authToken) {
        context.authToken = authToken;
    }

    public void setSoTimeout(int soTimeout) {
        context.soTimeout = soTimeout;
    }

    public void setUseSsl(boolean useSsl) {
        context.useSsl = useSsl;
    }

    public void setPingTime(long pingTime) {
        this.pingTime = pingTime;
    }

    private Socket newControlConnect() throws Exception {
        Socket socket = context.useSsl
        ? SocketHelper.newSSLSocket(context.serverHost, context.serverPort, context.soTimeout)
        : SocketHelper.newSocket(context.serverHost, context.serverPort, context.soTimeout);
        Thread thread = new Thread(new ControlConnect(socket, context));
        thread.setDaemon(true);
        thread.start();
        return socket;
    }

    public void start() {
        Socket socket = null;
        while (true) {
            if (context.getStatus() == Context.EXITED) {
                // 停顿3秒后退出
                Util.safeSleep(3000);
                return;
            }
            if (context.getStatus() == Context.PENDING) {
                try {
                    socket = newControlConnect();
                    context.setStatus(Context.CONNECTED);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                    // 断线重连频率10秒一次
                    Util.safeSleep(10000);
                    continue;
                }
            } else if (context.getStatus() == Context.AUTHERIZED) {
                try {
                    SocketHelper.sendpack(socket, Message.Ping());
                } catch (Exception e) {
                    log.error(e.toString(), e);
                    // 关闭套接字，读取此套接字的线程将退出阻塞
                    SocketHelper.safeClose(socket);
                }
            }
            Util.safeSleep(pingTime);
        }
    }

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "client.json";
        String json = FileUtil.readTextFile("classpath:" + filename);
        Config config = GsonUtil.toBean(json, Config.class);
        log.info("启动客户端：{}", GsonUtil.toJson(config));

        Ngrok ngrok = new Ngrok();
        ngrok.setTunnelList(config.tunnelList);
        ngrok.setServerHost(config.serverHost);
        ngrok.setServerPort(config.serverPort);
        ngrok.setPingTime(config.pingTime);
        ngrok.setAuthToken(config.authToken);
        ngrok.setUseSsl(config.useSsl);
        ngrok.start();
    }
}
