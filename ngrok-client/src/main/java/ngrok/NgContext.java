package ngrok;

import java.util.List;

import ngrok.model.Tunnel;

public class NgContext {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;

    /** 待机中 */
    public static final int PENDING = 0;
    /** 已连接 */
    public static final int CONNECTED = 1;
    /** 已认证 */
    public static final int AUTHERIZED = 2;
    /** 已退出 */
    public static final int EXITED = 3;

    private volatile int status = PENDING;

    public synchronized int getStatus() {
        return this.status;
    }

    public synchronized void setStatus(int status) {
        this.status = status;
    }
}
