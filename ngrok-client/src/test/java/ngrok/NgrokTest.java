package ngrok;

import java.util.ArrayList;
import java.util.List;

import ngrok.model.Tunnel;

public class NgrokTest {

    public static void main(String[] args) {
        List<Tunnel> list = new ArrayList<Tunnel>();

        Tunnel t1 = new Tunnel();
        t1.setProtocol("http");
        t1.setHostname("");
        t1.setSubdomain("www");
        t1.setLocalHost("127.0.0.1");
        t1.setLocalPort(8080);
        list.add(t1);

        Tunnel t2 = new Tunnel();
        t2.setProtocol("https");
        t2.setHostname("www.myngrok.com");
        t2.setSubdomain("");
        t2.setLocalHost("127.0.0.1");
        t2.setLocalPort(8080);
        list.add(t2);

        Tunnel t3 = new Tunnel();
        t3.setProtocol("tcp");
        t3.setHostname("");
        t3.setSubdomain("");
        t3.setRemotePort(12345);
        t3.setLocalHost("127.0.0.1");
        t3.setLocalPort(8080);
        list.add(t3);

        Ngrok ngrok = new Ngrok();
        ngrok.setTunnelList(list);
        ngrok.setServerHost("myngrok.com");
        ngrok.setServerPort(4443);
//		ngrok.setLog(new LoggerImpl().setEnableLog(false));
        ngrok.start();
    }
}
