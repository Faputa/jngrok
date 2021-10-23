package ngrok.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.client.connect.ControlConnect;
import ngrok.client.model.Tunnel;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class Ngrok {

    private static final Logger log = LoggerFactory.getLogger(Ngrok.class);

    private Context context = new Context();

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

    public void setPingTime(int pingTime) {
        context.pingTime = pingTime;
    }

    public void start() {
        ControlConnect controlConnect = new ControlConnect(context);
        while (true) {
            try {
                controlConnect.run();
                return;
            } catch (Exception e) {
                log.error(e.toString(), e);
                // 断线重连频率10秒一次
                Util.safeSleep(10000);
            }
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
