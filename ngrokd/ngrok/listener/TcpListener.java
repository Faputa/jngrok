/**
 * 监听用户的tcp请求
 */
package ngrok.listener;

import java.net.ServerSocket;
import java.net.Socket;

import ngrok.NgdContext;
import ngrok.server.TcpServer;

public class TcpListener implements Runnable
{
	private ServerSocket ssocket;
	private NgdContext context;

	public TcpListener(ServerSocket ssocket, NgdContext context)
	{
		this.ssocket = ssocket;
		this.context = context;
	}

	@Override
	public void run()
	{
		try(ServerSocket ssocket = this.ssocket)
		{
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
		}
	}
}
