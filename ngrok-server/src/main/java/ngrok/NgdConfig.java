package ngrok;

public class NgdConfig {

    String sslKeyStore = "classpath:server_ks.jks";
    String sslKeyStorePassword = "123456";
    String domain = "";
    String host = "";
    int port = 4443;
    /** 套接字超时时间8个小时 */
    int soTimeout = 28800000;
    /** 心跳超时时间2分种 */
    int pingTimeout = 120000;
    Integer httpPort;
    Integer httpsPort;
    String authToken;
}
