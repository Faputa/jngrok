package ngrok;

import java.util.List;

import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.Tunnel;

public class NgContext {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;
    public Logger log = new LoggerImpl();// 如果没有注入日志，则使用默认日志

    private volatile Boolean authOk = false;
    private final Object lock = new Object();

    public Boolean getAuthOk() {
        synchronized (lock) {
            return authOk;
        }
    }

    public void setAuthOk(Boolean authOk) {
        synchronized (lock) {
            this.authOk = authOk;
        }
    }
}
