package ngrok;

import java.net.Socket;
import java.util.List;

import ngrok.connect.ControlConnect;
import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.ToolUtil;

public class Ngrok {

    private NgContext context = new NgContext();
    private Logger log = Logger.getLogger();
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

    public void setLog(Logger log) {
        Logger.setLogger(log);
        this.log = log;
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
        long lastTime = System.currentTimeMillis();

        while (true) {
            if (context.getStatus() == NgContext.PENDING) {
                try {
                    socket = newControlConnect();
                    context.setStatus(NgContext.CONNECTED);
                } catch (Exception e) {
                    log.err(e.toString());
                    // 断线重连频率10秒一次
                    ToolUtil.sleep(10000);
                    continue;
                }
            }

            else if (context.getStatus() == NgContext.CONNECTED) {
                // do nothing
            }

            else if (context.getStatus() == NgContext.AUTHERIZED) {
                if (System.currentTimeMillis() > lastTime + pingTime) {
                    try {
                        SocketHelper.sendpack(socket, NgMsg.Ping());
                    } catch (Exception e) {
                        log.err(e.toString());
                        // 关闭套接字，读取此套接字的线程将退出阻塞
                        SocketHelper.safeClose(socket);
                    }
                    lastTime = System.currentTimeMillis();
                }
            }

            else if (context.getStatus() == NgContext.EXITED) {
                // 停顿3秒后退出
                ToolUtil.sleep(3000);
                return;
            }
            ToolUtil.sleep(1000);
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
        ngrok.setLog(new LoggerImpl().setEnableLog(config.enableLog));
        ngrok.start();
    }
}
