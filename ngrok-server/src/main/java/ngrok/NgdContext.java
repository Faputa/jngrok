package ngrok;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ngrok.model.ClientInfo;
import ngrok.model.TunnelInfo;

public class NgdContext {

    public String domain;
    public String host;
    public int port;
    public int soTimeout;
    public int pingTimeout;
    public Integer httpPort;
    public Integer httpsPort;
    public String authToken;

    /** Map<clientId, clientInfo> */
    private Map<String, ClientInfo> clientInfoMap = new ConcurrentHashMap<>();
    /** Map<url, tunnelInfo> */
    private Map<String, TunnelInfo> tunnelInfoMap = new ConcurrentHashMap<>();

    public ClientInfo getClientInfo(String clientId) {
        return clientInfoMap.get(clientId);
    }

    public synchronized void putClientInfo(String clientId, ClientInfo clientInfo) {
        clientInfoMap.put(clientId, clientInfo);
    }

    public TunnelInfo getTunnelInfo(String url) {
        return tunnelInfoMap.get(url);
    }

    public synchronized void putTunnelInfo(String url, TunnelInfo tunnelInfo) {
        tunnelInfoMap.put(url, tunnelInfo);
    }

    private void cleanTunnelInfo(String clientId) {
        Iterator<Entry<String, TunnelInfo>> it = tunnelInfoMap.entrySet().iterator();
        while (it.hasNext()) {
            TunnelInfo ti = it.next().getValue();
            if (clientId.equals(ti.getClientId())) {
                ti.close();
                it.remove();
            }
        }
    }

    /**
     * 清理客户端
     */
    public synchronized void cleanClient(String clientId) {
        clientInfoMap.remove(clientId);
        cleanTunnelInfo(clientId);
    }

    /**
     * 关闭空闲的客户端
     */
    public synchronized void closeIdleClient() {
        Set<String> clientIdSet = new HashSet<>();
        for (TunnelInfo ti : tunnelInfoMap.values()) {
            clientIdSet.add(ti.getClientId());
        }
        for (Entry<String, ClientInfo> e : clientInfoMap.entrySet()) {
            if (!clientIdSet.contains(e.getKey())) {
                e.getValue().close();
            }
        }
    }

    /**
     * 关闭心跳超时的客户端
     */
    public synchronized void closePingTimeoutClient() {
        for (ClientInfo e : clientInfoMap.values()) {
            if (System.currentTimeMillis() > e.getLastPingTime() + pingTimeout) {
                e.close();
            }
        }
    }
}
