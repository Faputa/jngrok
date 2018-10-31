package ngrok;

public class NgdConfig
{
	public String sslKeyStore = "classpath:server_ks.jks";
	public String sslKeyStorePassword = "123456";
	public String domain = "";
	public String host = "";
	public int port = 4443;
	public int timeout = 120000;
	public Integer httpPort;
	public Integer httpsPort;
	public String authToken;
	public boolean enableLog = true;
}
