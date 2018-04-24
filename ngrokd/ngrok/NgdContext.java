package ngrok;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;

public class NgdContext
{
	private String domain;
	private String host;
	private int port;
	private int httpPort;
	private int httpsPort;
	private Logger log = new LoggerImpl();// 如果没有注入日志，则使用默认日志

	public String getDomain()
	{
		return domain;
	}

	public void setDomain(String domain)
	{
		this.domain = domain;
	}

	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	public int getHttpPort()
	{
		return httpPort;
	}

	public void setHttpPort(int httpPort)
	{
		this.httpPort = httpPort;
	}

	public int getHttpsPort()
	{
		return httpsPort;
	}

	public void setHttpsPort(int httpsPort)
	{
		this.httpsPort = httpsPort;
	}

	public Logger getLog()
	{
		return log;
	}

	public void setLog(Logger log)
	{
		this.log = log;
	}

	private Map<String, BlockingQueue<OuterLink>> outerLinkQueueMap = new ConcurrentHashMap<String, BlockingQueue<OuterLink>>();
	private Map<String, TunnelInfo> tunnelInfoMap = new ConcurrentHashMap<String, TunnelInfo>();

	public OuterLink takeOuterLink(String clientId) throws InterruptedException
	{
		BlockingQueue<OuterLink> queue = outerLinkQueueMap.get(clientId);
		if(queue == null)
		{
			return null;
		}
		return queue.take();// 如果没有会阻塞
	}

	public void putOuterLink(String clientId, OuterLink link) throws InterruptedException
	{
		outerLinkQueueMap.get(clientId).put(link);
	}

	public void putOuterLinkQueue(String clientId)
	{
		outerLinkQueueMap.put(clientId, new LinkedBlockingQueue<OuterLink>());
	}

	public void delOuterLinkQueue(String clientId)
	{
		BlockingQueue<OuterLink> queue = outerLinkQueueMap.get(clientId);
		if(queue != null)
		{
			for(OuterLink link : queue)
			{
				try
				{
					link.putProxySocket(new Socket());// put一个空Socket，确保HttpServer/TcpServer线程能够结束阻塞
				}
				catch(InterruptedException e)
				{
				}
			}
			try
			{
				queue.put(new OuterLink());// put一个空OuterLink，确保ClientServer线程能够结束阻塞
			}
			catch(InterruptedException e)
			{
			}
			outerLinkQueueMap.remove(clientId);
		}
	}

	public TunnelInfo getTunnelInfo(String url)
	{
		return tunnelInfoMap.get(url);
	}

	public void putTunnelInfo(String url, TunnelInfo tunnelInfo)
	{
		tunnelInfoMap.put(url, tunnelInfo);
	}

	public void delTunnelInfo(String clientId)
	{
		Iterator<Map.Entry<String, TunnelInfo>> it = tunnelInfoMap.entrySet().iterator();
		while(it.hasNext())
		{
			TunnelInfo tunnel = it.next().getValue();
			if(clientId.equals(tunnel.getClientId()))
			{
				if(tunnel.getTcpServerSocket() != null)
				{
					try
					{
						tunnel.getTcpServerSocket().close();
					}
					catch(IOException e)
					{
					}
				}
				it.remove();
			}
		}
	}
}
