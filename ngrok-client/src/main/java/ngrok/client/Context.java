package ngrok.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ngrok.client.model.Tunnel;
import ngrok.common.Exitable;

public class Context {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;
    public int soTimeout;
    public boolean useSsl;

    private List<Exitable> proxyConnects = Collections.synchronizedList(new ArrayList<>());

    public synchronized void addProxyConnect(Exitable connect) {
        proxyConnects.add(connect);
    }

    public synchronized void removeProxyConnect(Exitable connect) {
        proxyConnects.remove(connect);
    }

    public synchronized void clean() {
        for (Tunnel tunnel : tunnelList) {
            tunnel.clean();
        }
        for (Exitable connect : proxyConnects) {
            connect.exit();
        }
    }

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
