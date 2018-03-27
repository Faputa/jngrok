package ngrok;

import java.net.Socket;
import java.util.List;

import ngrok.connect.ControlConnect;
import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.Tunnel;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.UBUtil;

public class Ngrok
{
	private NgContext context = new NgContext();
	private Logger log = context.getLog();

	public void setServerHost(String serverHost)
	{
		context.setServerHost(serverHost);
	}

	public void setServerPort(int serverPort)
	{
		context.setServerPort(serverPort);
	}

	public void setTunnelList(List<Tunnel> tunnelList)
	{
		context.setTunnelList(tunnelList);
	}

	public void setLog(Logger log)
	{
		context.setLog(log);;
	}

	public void start()
	{
		try(Socket socket = SocketHelper.newSSLSocket(context.getServerHost(), context.getServerPort()))
		{
			Thread thread = new Thread(new ControlConnect(socket, context));
			thread.setDaemon(true);
			thread.start();
			while(true)
			{
				SocketHelper.sendpack(socket, NgMsg.Ping());
				try{Thread.sleep(5000);}catch(InterruptedException e){}
			}
		}
		catch(Exception e)
		{
			log.err(e.getMessage());
		}
	}

	public static void main(String[] args)
	{
		String json = UBUtil.readTextFile(UBUtil.getLocation("resource/client.json"));
		NgConfig config = GsonUtil.toBean(json, NgConfig.class);
		Ngrok ngrok = new Ngrok();
		ngrok.setTunnelList(config.tunnelList);
		ngrok.setServerHost(config.serverHost);
		ngrok.setServerPort(config.serverPort);
		ngrok.setLog(new LoggerImpl().setEnableLog(config.enableLog));
		ngrok.start();
	}
}
