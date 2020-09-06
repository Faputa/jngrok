package ngrok;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ngrok.model.Tunnel;
import ngrok.socket.SocketHelper;

public class NgContext {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;

    private List<Socket> localSockets = Collections.synchronizedList(new ArrayList<>());
    private List<Socket> proxySockets = Collections.synchronizedList(new ArrayList<>());

    public void addLocalSocket(Socket socket) {
        localSockets.add(socket);
    }

    public void removeLocalSocket(Socket socket) {
        localSockets.remove(socket);
    }

    public void addProxySocket(Socket socket) {
        proxySockets.add(socket);
    }

    public void removeProxySocket(Socket socket) {
        proxySockets.remove(socket);
    }

    public void closeLocalSockets() {
        for (Socket socket : localSockets) {
            SocketHelper.safeClose(socket);
        }
    }

    public void closeProxySockets() {
        for (Socket socket : proxySockets) {
            SocketHelper.safeClose(socket);
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
