package ngrok;

import ngrok.util.FileUtil;
import ngrok.util.SSLContextUtil;

public class NgrokdTest {

    public static void main(String[] args) throws Exception {
//		System.setProperty("javax.net.ssl.keyStore", ToolUtil.getLocation("server_ks.jks"));
//		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        SSLContextUtil.createDefaultSSLContext(FileUtil.getFileStream("classpath:server_ks.jks"), "123456");

        Ngrokd ngrokd = new Ngrokd();
        ngrokd.setDomain("myngrok.com");
        ngrokd.setHost("");
        ngrokd.setPort(4443);
        ngrokd.setHttpPort(80);
        ngrokd.setHttpsPort(443);
//		ngrokd.setLog(new LoggerImpl().setEnableLog(false));
        ngrokd.start();
    }
}
