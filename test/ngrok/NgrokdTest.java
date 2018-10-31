package ngrok;

import java.io.InputStream;

import ngrok.util.FileUtil;
import ngrok.util.SSLContextUtil;

public class NgrokdTest
{
	public static void main(String[] args) throws Exception
	{
//		System.setProperty("javax.net.ssl.keyStore", Util.getLocation("resource/server_ks.jks"));
//		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		InputStream keyStream = FileUtil.getFileStream("classpath:resource/server_ks.jks");
		SSLContextUtil.createDefaultSSLContext(keyStream, "123456");

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
