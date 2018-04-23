/**
 * 处理Ngrok建立的连接
 */
package ngrok.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.NgdMsg;
import ngrok.Protocol;
import ngrok.listener.TcpListener;
import ngrok.model.OuterLink;
import ngrok.model.TunnelInfo;
import ngrok.socket.PacketReader;
import ngrok.socket.SocketHelper;
import ngrok.util.GsonUtil;
import ngrok.util.UBUtil;

public class ClientServer implements Runnable
{
	private Socket socket;
	private NgdContext context;
	private Logger log;

	public ClientServer(Socket socket, NgdContext context)
	{
		this.socket = socket;
		this.context = context;
		this.log = context.getLog();
	}

	@Override
	public void run()
	{
		String clientId = null;
		try(Socket socket = this.socket)
		{
			PacketReader pr = new PacketReader(socket);
			while(true)
			{
				String msg = pr.read();
				if(msg == null)
				{
					break;
				}
				log.log("收到客户端信息：" + msg);
				Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
				if("Auth".equals(protocol.Type))
				{
					clientId = UBUtil.MD5(String.valueOf(System.currentTimeMillis()));
					context.putOuterLinkQueue(clientId, new LinkedBlockingQueue<OuterLink>());
					SocketHelper.sendpack(socket, NgdMsg.AuthResp(clientId));
					SocketHelper.sendpack(socket, NgdMsg.ReqProxy());
				}
				else if("RegProxy".equals(protocol.Type))
				{
					String _clientId = protocol.Payload.ClientId;
					BlockingQueue<OuterLink> linkQueue = context.getOuterLinkQueue(_clientId);
					if(linkQueue == null)
					{
						break;
					}
					OuterLink link = linkQueue.take();// 如果没有会阻塞
					if(link.getUrl() == null)
					{
						break;
					}
					try
					{
						SocketHelper.sendpack(socket, NgdMsg.StartProxy(link.getUrl()));
					}
					catch(Exception e)// 防止代理连接睡死
					{
						SocketHelper.sendpack(link.getControlSocket(), NgdMsg.ReqProxy());
						linkQueue.put(link);
						break;
					}
					link.putProxySocket(socket);
					try(Socket outerSocket = link.getOuterSocket())
					{
						SocketHelper.sendpack(link.getControlSocket(), NgdMsg.ReqProxy());
						SocketHelper.forward(socket, outerSocket);
					}
					catch(Exception e)
					{
					}
					break;
				}
				else if("ReqTunnel".equals(protocol.Type))
				{
					if("http".equals(protocol.Payload.Protocol) || "https".equals(protocol.Payload.Protocol))
					{
						if(protocol.Payload.Hostname == null || protocol.Payload.Hostname.length() == 0)
						{
							if(protocol.Payload.Subdomain == null || protocol.Payload.Subdomain.length() == 0)
							{
								protocol.Payload.Subdomain = UBUtil.getRandString(5);
							}
							protocol.Payload.Hostname = protocol.Payload.Subdomain + "." + context.getDomain();
						}
						String url = protocol.Payload.Protocol + "://" + protocol.Payload.Hostname;
						if("http".equals(protocol.Payload.Protocol) && context.getHttpPort() != 80)
						{
							url += ":" + context.getHttpPort();
						}
						else if("https".equals(protocol.Payload.Protocol) && context.getHttpsPort() != 443)
						{
							url += ":" + context.getHttpsPort();
						}
						if(context.getTunnelInfo(url) != null)
						{
							String error = "The tunnel " + url + " is already registered.";
							SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
							break;
						}
						TunnelInfo tunnel = new TunnelInfo();
						tunnel.setClientId(clientId);
						tunnel.setControlSocket(socket);
						context.putTunnelInfo(url, tunnel);
						SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
					}
					else if("tcp".equals(protocol.Payload.Protocol))
					{
						String url = "tcp://" + context.getDomain() + ":" + protocol.Payload.RemotePort;
						if(context.getTunnelInfo(url) != null)
						{
							String error = "The tunnel " + url + " is already registered.";
							SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
							break;
						}
						ServerSocket serverSocket;
						try
						{
							serverSocket = SocketHelper.newServerSocket(protocol.Payload.RemotePort);
						}
						catch(Exception e)
						{
							String error = "The tunnel " + url + " is already registered.";
							SocketHelper.sendpack(socket, NgdMsg.NewTunnel(null, null, null, error));
							break;
						}
						Thread thread = new Thread(new TcpListener(serverSocket, context));
						thread.setDaemon(true);
						thread.start();

						TunnelInfo tunnel = new TunnelInfo();
						tunnel.setClientId(clientId);
						tunnel.setControlSocket(socket);
						tunnel.setTcpServerSocket(serverSocket);
						context.putTunnelInfo(url, tunnel);
						SocketHelper.sendpack(socket, NgdMsg.NewTunnel(protocol.Payload.ReqId, url, protocol.Payload.Protocol, null));
					}
				}
				else if("Ping".equals(protocol.Type))
				{
					SocketHelper.sendpack(socket, NgdMsg.Pong());
				}
			}
		}
		catch(Exception e)
		{
			log.err(e.getMessage());
		}
		if(clientId != null)
		{
			context.delOuterLinkQueue(clientId);
			context.delTunnelInfo(clientId);
		}
	}
}
