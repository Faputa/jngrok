package ngrok.client;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ngrok.client.model.Tunnel;
import ngrok.common.Exitable;
import ngrok.socket.SocketHelper;

public class Context {

    public String serverHost;
    public int serverPort;
    public List<Tunnel> tunnelList;
    public String authToken;
    public int soTimeout;
    public boolean useSsl;
    public int pingTime;

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

    public Socket connectServer() throws Exception {
        Socket socket = useSsl
        ? SocketHelper.newSSLSocket(serverHost, serverPort, soTimeout)
        : SocketHelper.newSocket(serverHost, serverPort, soTimeout);
        return socket;
    }
}
