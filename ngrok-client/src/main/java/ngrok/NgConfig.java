package ngrok;

import ngrok.model.Tunnel;

import java.util.ArrayList;
import java.util.List;

public class NgConfig {

    List<Tunnel> tunnelList = new ArrayList<>();
    String serverHost = "";
    int serverPort = 4443;
    String authToken;
    long pingTime = 10000;
}
