package ngrok;

import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.model.Request;
import ngrok.model.TunnelInfo;
import ngrok.socket.SocketHelper;

public class NgdContext {

    public String domain;
    public String host;
    public int port;
    public int timeout;
    public Integer httpPort;
    public Integer httpsPort;
    public String authToken;

    // client info
    private Map<String, BlockingQueue<Request>> requestQueueMap = new ConcurrentHashMap<>();
    private Map<String, Socket> controlSocketMap = new ConcurrentHashMap<>();

    public void initClientInfo(String clientId, Socket controlSocket) {
        requestQueueMap.put(clientId, new LinkedBlockingQueue<>());
        controlSocketMap.put(clientId, controlSocket);
    }

    public BlockingQueue<Request> getRequestQueue(String clientId) {
        return requestQueueMap.get(clientId);
    }

    public Socket getControlSocket(String clientId) {
        return controlSocketMap.get(clientId);
    }

    public void delClientInfo(String clientId) {
        controlSocketMap.remove(clientId);
        BlockingQueue<Request> queue = requestQueueMap.get(clientId);
        if (queue != null) {
            try {
                queue.put(new Request());// 毒丸
            } catch (InterruptedException e) {
            }
            requestQueueMap.remove(clientId);
        }
    }

    // tunnel info
    private Map<String, TunnelInfo> tunnelInfoMap = new ConcurrentHashMap<>();

    public TunnelInfo getTunnelInfo(String url) {
        return tunnelInfoMap.get(url);
    }

    public void putTunnelInfo(String url, TunnelInfo tunnelInfo) {
        tunnelInfoMap.put(url, tunnelInfo);
    }

    public void delTunnelInfo(String clientId) {
        Iterator<Map.Entry<String, TunnelInfo>> it = tunnelInfoMap.entrySet().iterator();
        while (it.hasNext()) {
            TunnelInfo tunnel = it.next().getValue();
            if (clientId.equals(tunnel.getClientId())) {
                if (tunnel.getTcpServerSocket() != null) {
                    SocketHelper.safeClose(tunnel.getTcpServerSocket());
                }
                it.remove();
            }
        }
    }

    public void closeIdleClient() {
        Set<String> clientIdSet = new HashSet<>();
        for (TunnelInfo tunnel : tunnelInfoMap.values()) {
            clientIdSet.add(tunnel.getClientId());
        }
        for (Map.Entry<String, Socket> entry : controlSocketMap.entrySet()) {
            if (!clientIdSet.contains(entry.getKey())) {
                SocketHelper.safeClose(entry.getValue());
            }
        }
    }
}
