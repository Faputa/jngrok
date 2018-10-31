package ngrok;

import java.net.Socket;
import java.util.List;

import ngrok.connect.ControlConnect;
import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.FileUtil;
import ngrok.util.GsonUtil;

public class Ngrok
{
	private NgContext context = new NgContext();
	private long pingTime = 10000;// 心跳包周期默认为10秒

	public void setServerHost(String serverHost)
	{
		context.serverHost = serverHost;
	}

	public void setServerPort(int serverPort)
	{
		context.serverPort = serverPort;
	}

	public void setTunnelList(List<Tunnel> tunnelList)
	{
		context.tunnelList = tunnelList;
	}

	public void setAuthToken(String authToken)
	{
		context.authToken = authToken;
	}

	public void setLog(Logger log)
	{
		context.log = log;
	}
	
	public void setPingTime(long pingTime)
	{
		this.pingTime = pingTime;
	}

	public void start()
	{
		try(Socket socket = SocketHelper.newSSLSocket(context.serverHost, context.serverPort))
		{
			Thread thread = new Thread(new ControlConnect(socket, context));
			thread.setDaemon(true);
			thread.start();
			while(true)
			{
				try{Thread.sleep(this.pingTime);}catch(InterruptedException e){}
				SocketHelper.sendpack(socket, NgMsg.Ping());
			}
		}
		catch(Exception e)
		{
			context.log.err(e.toString());
		}
	}

	public static void main(String[] args)
	{
		String json = FileUtil.readTextFile("classpath:client.json");
		NgConfig config = GsonUtil.toBean(json, NgConfig.class);
		Ngrok ngrok = new Ngrok();
		ngrok.setTunnelList(config.tunnelList);
		ngrok.setServerHost(config.serverHost);
		ngrok.setServerPort(config.serverPort);
		ngrok.setPingTime(config.pingTime);
		ngrok.setAuthToken(config.authToken);
		ngrok.setLog(new LoggerImpl().setEnableLog(config.enableLog));
		ngrok.start();
	}
}
