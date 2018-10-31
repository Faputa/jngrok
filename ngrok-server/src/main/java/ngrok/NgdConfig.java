package ngrok;

public class NgdConfig {

    String sslKeyStore = "classpath:server_ks.jks";
    String sslKeyStorePassword = "123456";
    String domain = "";
    String host = "";
    int port = 4443;
    int timeout = 120000;
    Integer httpPort;
    Integer httpsPort;
    String authToken;
    boolean enableLog = true;
}
