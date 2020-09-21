package ngrok.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

import ngrok.client.model.Tunnel;

public class Config {

    @Expose List<Tunnel> tunnelList = new ArrayList<>();
    @Expose String serverHost = "";
    @Expose int serverPort = 4443;
    @Expose String authToken;
    /** 套接字超时时间8个小时 */
    @Expose int soTimeout = 28800000;
    @Expose long pingTime = 10000;
    @Expose boolean useSsl = true;
}
