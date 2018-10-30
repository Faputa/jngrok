/**
 * 监听用户的tcp请求
 */
package ngrok.listener;

import java.net.ServerSocket;
import java.net.Socket;

import ngrok.NgdContext;
import ngrok.log.Logger;
import ngrok.server.TcpServer;

public class TcpListener implements Runnable
{
	private ServerSocket ssocket;
	private NgdContext context;
	private Logger log;

	public TcpListener(ServerSocket ssocket, NgdContext context)
	{
		this.ssocket = ssocket;
		this.context = context;
		this.log = context.log;
	}

	@Override
	public void run()
	{
		try(ServerSocket ssocket = this.ssocket)
		{
			log.log("监听建立成功：[%s:%s]", context.host, ssocket.getLocalPort());
			while(true)
			{
				Socket socket = ssocket.accept();
				Thread thread = new Thread(new TcpServer(socket, context));
				thread.setDaemon(true);
				thread.start();
			}
		}
		catch(Exception e)
		{
			log.log("监听退出：[%s:%s]", context.host, ssocket.getLocalPort());
		}
	}
}
