package ngrok;

import ngrok.listener.ClientListener;
import ngrok.listener.HttpListener;
import ngrok.listener.HttpsListener;
import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.util.GsonUtil;
import ngrok.util.UBUtil;

public class Ngrokd
{
	private NgdContext context = new NgdContext();

	public void setDomain(String domain)
	{
		context.setDomain(domain);
	}

	public void setHost(String host)
	{
		context.setHost(host);
	}

	public void setHttpPort(int port)
	{
		context.setHttpPort(port);
	}

	public void setHttpsPort(int port)
	{
		context.setHttpsPort(port);
	}

	public void setPort(int port)
	{
		context.setPort(port);
	}

	public void setLog(Logger log)
	{
		context.setLog(log);;
	}

	public void start()
	{
		try
		{
			Thread clientListenerThread = new Thread(new ClientListener(context));
			clientListenerThread.setDaemon(true);
			clientListenerThread.start();
			Thread httpListenerThread = new Thread(new HttpListener(context));
			httpListenerThread.setDaemon(true);
			httpListenerThread.start();
			Thread httpsListenerThread = new Thread(new HttpsListener(context));
			httpsListenerThread.setDaemon(true);
			httpsListenerThread.start();
			while(true)
			{
				try{Thread.sleep(5000);}catch(InterruptedException e){}
			}
		}
		catch(Exception e)
		{
			e.getStackTrace();
		}
	}

	public static void main(String[] args)
	{
		System.setProperty("javax.net.ssl.keyStore", UBUtil.getLocation("resource/server_ks.jks"));
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");

		String json = UBUtil.readTextFile(UBUtil.getLocation("resource/server.json"));
		NgdConfig config = GsonUtil.toBean(json, NgdConfig.class);
		Ngrokd ngrokd = new Ngrokd();
		ngrokd.setDomain(config.domain);
		ngrokd.setHost(config.host);
		ngrokd.setPort(config.port);
		ngrokd.setHttpPort(config.httpPort);
		ngrokd.setHttpsPort(config.httpsPort);
		ngrokd.setLog(new LoggerImpl().setEnableLog(config.enableLog));
		ngrokd.start();
	}
}
