package ngrok.client.model;

import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.annotations.Expose;

import ngrok.common.Exitable;

public class Tunnel {

    @Expose public String protocol;
    @Expose public String hostname;
    @Expose public String subdomain;
    @Expose public int remotePort;
    @Expose public String localHost;
    @Expose public int localPort;

    private CopyOnWriteArrayList<Exitable> localConnects = new CopyOnWriteArrayList<>();

    public void addLocalConnect(Exitable connect) {
        localConnects.add(connect);
    }

    public void removeLocalConnect(Exitable connect) {
        localConnects.remove(connect);
    }

    public void clean() {
        for (Exitable connect : localConnects) {
            connect.exit();
        }
    }
}
