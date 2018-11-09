package ngrok;

import java.util.ArrayList;
import java.util.List;

import ngrok.model.Tunnel;

public class NgConfig {

    List<Tunnel> tunnelList = new ArrayList<>();
    String serverHost = "";
    int serverPort = 4443;
    String authToken;
    boolean enableLog = true;
    long pingTime = 10000;
}
