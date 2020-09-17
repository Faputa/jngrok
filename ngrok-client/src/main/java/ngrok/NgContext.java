package ngrok;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ngrok.model.Tunnel;

public class NgContext {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;
    public int soTimeout;

    private List<Thread> localThreads = Collections.synchronizedList(new ArrayList<>());
    private List<Thread> proxyThreads = Collections.synchronizedList(new ArrayList<>());

    public synchronized void addLocalThread(Thread thread) {
        localThreads.add(thread);
    }

    public synchronized void removeLocalThread(Thread thread) {
        localThreads.remove(thread);
    }

    public synchronized void addProxyThread(Thread thread) {
        proxyThreads.add(thread);
    }

    public synchronized void removeProxyThread(Thread thread) {
        proxyThreads.remove(thread);
    }

    public synchronized void clean() {
        for (Thread thread : localThreads) {
            thread.interrupt();
        }
        for (Thread thread : proxyThreads) {
            thread.interrupt();
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
