package ngrok;

import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.connect.ControlConnect;
import ngrok.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class Ngrok {

    private static final Logger log = LoggerFactory.getLogger(Ngrok.class);

    private NgContext context = new NgContext();
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

    public void setPingTime(long pingTime) {
        this.pingTime = pingTime;
    }

    private Socket newControlConnect() throws Exception {
        Socket socket = SocketHelper.newSSLSocket(context.serverHost, context.serverPort);
        Thread thread = new Thread(new ControlConnect(socket, context));
        thread.setDaemon(true);
        thread.start();
        return socket;
    }

    public void start() {
        Socket socket = null;
        long lastPingTime = System.currentTimeMillis();

        while (true) {
            if (context.getStatus() == NgContext.PENDING) {
                try {
                    socket = newControlConnect();
                    context.setStatus(NgContext.CONNECTED);
                } catch (Exception e) {
                    log.error(e.toString());
                    // 断线重连频率10秒一次
                    Util.sleep(10000);
                    continue;
                }
            } else if (context.getStatus() == NgContext.AUTHERIZED) {
                if (System.currentTimeMillis() > lastPingTime + pingTime) {
                    try {
                        SocketHelper.sendpack(socket, NgMsg.Ping());
                    } catch (Exception e) {
                        log.error(e.toString());
                        // 关闭套接字，读取此套接字的线程将退出阻塞
                        SocketHelper.safeClose(socket);
                    }
                    lastPingTime = System.currentTimeMillis();
                }
            } else if (context.getStatus() == NgContext.EXITED) {
                // 停顿3秒后退出
                Util.sleep(3000);
                return;
            } else if (context.getStatus() == NgContext.CONNECTED) {
                // do nothing
            }
            Util.sleep(1000);
        }
    }

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "client.json";
        String json = FileUtil.readTextFile("classpath:" + filename);
        NgConfig config = GsonUtil.toBean(json, NgConfig.class);
        Ngrok ngrok = new Ngrok();
        ngrok.setTunnelList(config.tunnelList);
        ngrok.setServerHost(config.serverHost);
        ngrok.setServerPort(config.serverPort);
        ngrok.setPingTime(config.pingTime);
        ngrok.setAuthToken(config.authToken);
        ngrok.start();
    }
}
