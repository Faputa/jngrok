package ngrok;

import ngrok.model.Tunnel;

import java.util.ArrayList;
import java.util.List;

public class NgConfig {

    List<Tunnel> tunnelList = new ArrayList<>();
    String serverHost = "";
    int serverPort = 4443;
    String authToken;
    /** 套接字超时时间8个小时 */
    int soTimeout = 28800000;
    long pingTime = 10000;
}
