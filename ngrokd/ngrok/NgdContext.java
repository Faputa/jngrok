package ngrok;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.log.Logger;
import ngrok.log.LoggerImpl;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;

public class NgdContext
{
	public String domain;
	public String host;
	public int port;
	public int timeout;
	public Integer httpPort;
	public Integer httpsPort;
	public String authToken;
	public Logger log = new LoggerImpl();// 如果没有注入日志，则使用默认日志

	// client info
	private Map<String, BlockingQueue<OuterLink>> outerLinkQueueMap = new ConcurrentHashMap<>();
	private Map<String, Socket> controlSocketMap = new ConcurrentHashMap<>();

	public void initClientInfo(String clientId, Socket controlSocket)
	{
		outerLinkQueueMap.put(clientId, new LinkedBlockingQueue<OuterLink>());
		controlSocketMap.put(clientId, controlSocket);
	}

	public BlockingQueue<OuterLink> getOuterLinkQueue(String clientId)
	{
		return outerLinkQueueMap.get(clientId);
	}

	public Socket getControlSocket(String clientId)
	{
		return controlSocketMap.get(clientId);
	}

	public void delClientInfo(String clientId)
	{
		controlSocketMap.remove(clientId);
		BlockingQueue<OuterLink> queue = outerLinkQueueMap.get(clientId);
		if(queue != null)
		{
			try
			{
				queue.put(new OuterLink());// 毒丸
			}
			catch(InterruptedException e)
			{
			}
			outerLinkQueueMap.remove(clientId);
		}
	}

	// tunnel info
	private Map<String, TunnelInfo> tunnelInfoMap = new ConcurrentHashMap<String, TunnelInfo>();

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

	public void closeIdleClient()
	{
		Set<String> flagSet = new HashSet<>();
		for(TunnelInfo tunnel : tunnelInfoMap.values())
		{
			flagSet.add(tunnel.getClientId());
		}
		for(Map.Entry<String, Socket> entry : controlSocketMap.entrySet())
		{
			if(!flagSet.contains(entry.getKey()))
			{
				try
				{
					entry.getValue().close();
				}
				catch(IOException e)
				{
				}
			}
		}
	}
}
