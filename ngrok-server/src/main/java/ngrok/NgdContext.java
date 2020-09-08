package ngrok;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

    // client info
    private Map<String, ClientInfo> clientInfoMap = new ConcurrentHashMap<>();

    public ClientInfo getClientInfo(String clientId) {
        return clientInfoMap.get(clientId);
    }

    public void createClientInfo(String clientId, Socket controlSocket) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setLastPingTime(System.currentTimeMillis());
        clientInfo.setControlSocket(controlSocket);
        clientInfoMap.put(clientId, clientInfo);
    }

    public void deleteClientInfo(String clientId) {
        ClientInfo clientInfo = clientInfoMap.get(clientId);
        if (clientInfo != null) {
            clientInfo.close();
            clientInfoMap.remove(clientId);
        }
    }

    // tunnel info
    private Map<String, TunnelInfo> tunnelInfoMap = new ConcurrentHashMap<>();

    public TunnelInfo getTunnelInfo(String url) {
        return tunnelInfoMap.get(url);
    }

    public void createTunnelInfo(String url, String clientId, ServerSocket tcpServerSocket) {
        TunnelInfo tunnelInfo = new TunnelInfo();
        tunnelInfo.setClientId(clientId);
        tunnelInfo.setTcpServerSocket(tcpServerSocket);
        tunnelInfoMap.put(url, tunnelInfo);
    }

    public void deleteTunnelInfo(String clientId) {
        Iterator<Map.Entry<String, TunnelInfo>> it = tunnelInfoMap.entrySet().iterator();
        while (it.hasNext()) {
            TunnelInfo ti = it.next().getValue();
            if (clientId.equals(ti.getClientId())) {
                ti.close();
                it.remove();
            }
        }
    }

    /**
     * 关闭空闲的客户端
     */
    public void closeIdleClient() {
        Set<String> clientIdSet = new HashSet<>();
        for (TunnelInfo ti : tunnelInfoMap.values()) {
            clientIdSet.add(ti.getClientId());
        }
        for (Map.Entry<String, ClientInfo> e : clientInfoMap.entrySet()) {
            if (!clientIdSet.contains(e.getKey())) {
                e.getValue().close();
            }
        }
    }

    /**
     * 关闭心跳超时的客户端
     */
    public void closePingTimeoutClient() {
        for (ClientInfo e : clientInfoMap.values()) {
            if (System.currentTimeMillis() > e.getLastPingTime() + pingTimeout) {
                e.close();
            }
        }
    }
}
