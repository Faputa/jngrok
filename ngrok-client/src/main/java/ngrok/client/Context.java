package ngrok.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ngrok.client.model.Tunnel;
import ngrok.common.Exitable;

public class Context {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;
    public int soTimeout;
    public boolean useSsl;

    private CopyOnWriteArrayList<Exitable> proxyConnects = new CopyOnWriteArrayList<>();

    public void addProxyConnect(Exitable connect) {
        proxyConnects.add(connect);
    }

    public void removeProxyConnect(Exitable connect) {
        proxyConnects.remove(connect);
    }

    public void clean() {
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

    private AtomicInteger status = new AtomicInteger(PENDING);

    public int getStatus() {
        return this.status.get();
    }

    public void setStatus(int status) {
        this.status.set(status);
    }
}
