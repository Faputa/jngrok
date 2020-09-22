package ngrok.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.server.listener.ClientListener;
import ngrok.server.listener.HttpListener;
import ngrok.server.listener.HttpsListener;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.SSLContextUtil;
import ngrok.util.Util;

public class Ngrokd {

    private static final Logger log = LoggerFactory.getLogger(Ngrokd.class);

    private Context context = new Context();

    public void setDomain(String domain) {
        context.domain = domain;
    }

    public void setHost(String host) {
        context.host = host;
    }

    public void setHttpPort(Integer port) {
        context.httpPort = port;
    }

    public void setHttpsPort(Integer port) {
        context.httpsPort = port;
    }

    public void setPort(int port) {
        context.port = port;
    }

    public void setSoTimeout(int soTimeout) {
        context.soTimeout = soTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        context.pingTimeout = pingTimeout > 1000 ? pingTimeout : 1000;
    }

    public void setAuthToken(String authToken) {
        context.authToken = authToken;
    }

    public void setUseSsl(boolean useSsl) {
        context.useSsl = useSsl;
    }

    private Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void start() {
        Thread clientListener = null;
        Thread httpListener = null;
        Thread httpsListener = null;
        while (true) {
            try {
                if (clientListener == null || !clientListener.isAlive()) {
                    clientListener = newDaemonThread(new ClientListener(context));
                }
                if (context.httpPort != null && (httpListener == null || !httpListener.isAlive())) {
                    httpListener = newDaemonThread(new HttpListener(context));
                }
                if (context.httpsPort != null && (httpsListener == null || !httpsListener.isAlive())) {
                    httpsListener = newDaemonThread(new HttpsListener(context));
                }
                context.closeIdleClient();
                context.closePingTimeoutClient();
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
            Util.safeSleep(10000);
        }
    }

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "server.json";
        String json = FileUtil.readTextFile("classpath:" + filename);
        Config config = GsonUtil.toBean(json, Config.class);
        log.info("启动服务器：{}", GsonUtil.toJson(config));

        SSLContextUtil.createDefaultSSLContext(FileUtil.getFileStream(config.sslKeyStore), config.sslKeyStorePassword);

        Ngrokd ngrokd = new Ngrokd();
        ngrokd.setDomain(config.domain);
        ngrokd.setHost(config.host);
        ngrokd.setPort(config.port);
        ngrokd.setSoTimeout(config.soTimeout);
        ngrokd.setPingTimeout(config.pingTimeout);
        ngrokd.setHttpPort(config.httpPort);
        ngrokd.setHttpsPort(config.httpsPort);
        ngrokd.setAuthToken(config.authToken);
        ngrokd.setUseSsl(config.useSsl);
        ngrokd.start();
    }
}
