package ngrok.server;

import com.google.gson.annotations.Expose;

public class Config {

    @Expose String sslKeyStore = "classpath:server_ks.jks";
    @Expose String sslKeyStorePassword = "123456";
    @Expose String domain = "";
    @Expose String host = "";
    @Expose int port = 4443;
    /** 套接字超时时间8个小时 */
    @Expose int soTimeout = 28800000;
    /** 心跳超时时间2分种 */
    @Expose int pingTimeout = 120000;
    @Expose Integer httpPort;
    @Expose Integer httpsPort;
    @Expose String authToken;
    @Expose boolean useSsl = true;
}
