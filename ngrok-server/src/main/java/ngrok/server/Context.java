package ngrok.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ngrok.server.model.Client;
import ngrok.server.model.Tunnel;

public class Context {

    public String domain;
    public String host;
    public int port;
    public int soTimeout;
    public int pingTimeout;
    public Integer httpPort;
    public Integer httpsPort;
    public String authToken;
    public boolean useSsl;

    /** Map<clientId, client> */
    private Map<String, Client> clientMap = new ConcurrentHashMap<>();
    /** Map<url, tunnel> */
    private Map<String, Tunnel> tunnelMap = new ConcurrentHashMap<>();

    public Client getClient(String clientId) {
        return clientMap.get(clientId);
    }

    public synchronized void putClient(String clientId, Client client) {
        clientMap.put(clientId, client);
    }

    public Tunnel getTunnel(String url) {
        return tunnelMap.get(url);
    }

    public synchronized void putTunnel(String url, Tunnel tunnel) {
        tunnelMap.put(url, tunnel);
    }

    private void cleanTunnel(String clientId) {
        Iterator<Entry<String, Tunnel>> it = tunnelMap.entrySet().iterator();
        while (it.hasNext()) {
            Tunnel ti = it.next().getValue();
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
        Client client = clientMap.get(clientId);
        assert client != null;
        client.clean();
        clientMap.remove(clientId);
        cleanTunnel(clientId);
    }

    /**
     * 关闭空闲的客户端
     */
    public synchronized void closeIdleClient() {
        Set<String> clientIdSet = new HashSet<>();
        for (Tunnel ti : tunnelMap.values()) {
            clientIdSet.add(ti.getClientId());
        }
        for (Entry<String, Client> e : clientMap.entrySet()) {
            if (!clientIdSet.contains(e.getKey())) {
                e.getValue().close();
            }
        }
    }

    /**
     * 关闭心跳超时的客户端
     */
    public synchronized void closePingTimeoutClient() {
        for (Client e : clientMap.values()) {
            if (System.currentTimeMillis() > e.getLastPingTime() + pingTimeout) {
                e.close();
            }
        }
    }
}
