package ngrok;

import ngrok.listener.ClientListener;
import ngrok.listener.HttpListener;
import ngrok.listener.HttpsListener;
import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;
import ngrok.util.SSLContextUtil;
import ngrok.util.Util;

public class Ngrokd {

    private NgdContext context = new NgdContext();

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

    public void setTimeout(int timeout) {
        context.timeout = timeout;
    }

    public void setAuthToken(String authToken) {
        context.authToken = authToken;
    }

    public void setLog(Logger log) {
        Logger.setLogger(log);
    }

    private Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void start() {
        try {
            Thread clientListener = null;
            Thread httpListener = null;
            Thread httpsListener = null;
            long lastTime = System.currentTimeMillis();
            while (true) {
                if (clientListener == null || !clientListener.isAlive()) {
                    clientListener = newDaemonThread(new ClientListener(context));
                }
                if (context.httpPort != null && (httpListener == null || !httpListener.isAlive())) {
                    httpListener = newDaemonThread(new HttpListener(context));
                }
                if (context.httpsPort != null && (httpsListener == null || !httpsListener.isAlive())) {
                    httpsListener = newDaemonThread(new HttpsListener(context));
                }
                // 关闭空闲的客户端
                if (System.currentTimeMillis() > lastTime + 50000) {
                    context.closeIdleClient();
                    lastTime = System.currentTimeMillis();
                }
                Util.sleep(10000);
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "server.json";
        String json = FileUtil.readTextFile("classpath:" + filename);
        NgdConfig config = GsonUtil.toBean(json, NgdConfig.class);

        SSLContextUtil.createDefaultSSLContext(FileUtil.getFileStream(config.sslKeyStore), config.sslKeyStorePassword);

        Ngrokd ngrokd = new Ngrokd();
        ngrokd.setDomain(config.domain);
        ngrokd.setHost(config.host);
        ngrokd.setPort(config.port);
        ngrokd.setTimeout(config.timeout);
        ngrokd.setHttpPort(config.httpPort);
        ngrokd.setHttpsPort(config.httpsPort);
        ngrokd.setAuthToken(config.authToken);
        ngrokd.setLog(new LoggerImpl().setEnableLog(config.enableLog));
        ngrokd.start();
    }
}
