package ngrok.server;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private ConcurrentHashMap<String, Client> clientMap = new ConcurrentHashMap<>();
    /** Map<url, tunnel> */
    private ConcurrentHashMap<String, Tunnel> tunnelMap = new ConcurrentHashMap<>();

    public Client getClient(String clientId) {
        return clientMap.get(clientId);
    }

    public void putClient(String clientId, Client client) {
        clientMap.put(clientId, client);
    }

    public Tunnel getTunnel(String url) {
        return tunnelMap.get(url);
    }

    public void putTunnel(String url, Tunnel tunnel) {
        tunnelMap.put(url, tunnel);
    }

    private void cleanTunnel(String clientId) {
        tunnelMap.forEach((_clientId, tunnel) -> {
            if (Objects.equals(clientId, _clientId)) {
                tunnel.close();
                tunnelMap.remove(_clientId);
            }
        });
    }

    /**
     * 清理客户端
     */
    public void cleanClient(String clientId) {
        Client client = clientMap.get(clientId);
        assert client != null;
        client.clean();
        clientMap.remove(clientId);
        cleanTunnel(clientId);
    }

    /**
     * 关闭空闲的客户端
     */
    public void closeIdleClient() {
        Set<String> clientIdSet = tunnelMap.values().stream().map(Tunnel::getClientId).collect(Collectors.toSet());
        clientMap.forEach((clientId, client) -> {
            if (!clientIdSet.contains(clientId)) {
                client.close();
            }
        });
    }

    /**
     * 关闭心跳超时的客户端
     */
    public void closePingTimeoutClient() {
        clientMap.forEach((clientId, client) -> {
            if (System.currentTimeMillis() > client.getLastPingTime() + pingTimeout) {
                client.close();
            }
        });
    }
}
