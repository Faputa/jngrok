package ngrok.client;

import java.util.ArrayList;
import java.util.List;

import ngrok.client.model.Tunnel;

public class NgrokTest {

    public static void main(String[] args) {
        List<Tunnel> list = new ArrayList<>();

        Tunnel t1 = new Tunnel();
        t1.protocol = "http";
        t1.hostname = "";
        t1.subdomain = "www";
        t1.localHost = "127.0.0.1";
        t1.localPort = 5050;
        list.add(t1);

        Tunnel t2 = new Tunnel();
        t2.protocol= "https";
        t2.hostname = "www.myngrok.com";
        t2.subdomain = "";
        t2.localHost = "127.0.0.1";
        t2.localPort = 5050;
        list.add(t2);

        Tunnel t3 = new Tunnel();
        t3.protocol = "tcp";
        t3.hostname = "";
        t3.subdomain = "";
        t3.remotePort = 12345;
        t3.localHost = "127.0.0.1";
        t3.localPort = 5050;
        list.add(t3);

        Ngrok ngrok = new Ngrok();
        ngrok.setTunnelList(list);
        ngrok.setServerHost("myngrok.com");
        ngrok.setServerPort(4443);
        ngrok.start();
    }
}
