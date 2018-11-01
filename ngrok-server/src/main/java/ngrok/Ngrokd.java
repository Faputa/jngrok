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
        context.log = log;
    }

    public void start() {
        try {
            Thread clientListenerThread = new Thread(new ClientListener(context));
            clientListenerThread.setDaemon(true);
            clientListenerThread.start();
            if (context.httpPort != null) {
                Thread httpListenerThread = new Thread(new HttpListener(context));
                httpListenerThread.setDaemon(true);
                httpListenerThread.start();
            }
            if (context.httpsPort != null) {
                Thread httpsListenerThread = new Thread(new HttpsListener(context));
                httpsListenerThread.setDaemon(true);
                httpsListenerThread.start();
            }

            while (true) {
                Util.sleep(50000);
                context.closeIdleClient();
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String json = FileUtil.readTextFile("classpath:server.json");
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
