package ngrok;

import java.util.List;

import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.Tunnel;

public class NgContext
{
	private String serverHost;
	private int serverPort;
	private List<Tunnel> tunnelList;
	private Logger log = new LoggerImpl();// 如果没有注入日志，则使用默认日志

	public String getServerHost()
	{
		return serverHost;
	}

	public void setServerHost(String serverHost)
	{
		this.serverHost = serverHost;
	}

	public int getServerPort()
	{
		return serverPort;
	}

	public void setServerPort(int serverPort)
	{
		this.serverPort = serverPort;
	}

	public List<Tunnel> getTunnelList()
	{
		return tunnelList;
	}

	public void setTunnelList(List<Tunnel> tunnelList)
	{
		this.tunnelList = tunnelList;
	}

	public Logger getLog()
	{
		return log;
	}

	public void setLog(Logger log)
	{
		this.log = log;
	}
}
