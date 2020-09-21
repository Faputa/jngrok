package ngrok.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.Expose;

import ngrok.common.Exitable;

public class Tunnel {

    @Expose public String protocol;
    @Expose public String hostname;
    @Expose public String subdomain;
    @Expose public int remotePort;
    @Expose public String localHost;
    @Expose public int localPort;

    private List<Exitable> localConnects = Collections.synchronizedList(new ArrayList<>());

    public synchronized void addLocalConnect(Exitable connect) {
        localConnects.add(connect);
    }

    public synchronized void removeLocalConnect(Exitable connect) {
        localConnects.remove(connect);
    }

    public synchronized void clean() {
        for (Exitable connect : localConnects) {
            connect.exit();
        }
    }
}
