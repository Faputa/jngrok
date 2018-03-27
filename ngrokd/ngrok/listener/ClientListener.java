/**
 * 监听Ngrok的连接请求
 */
package ngrok.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.server.ClientServer;
import ngrok.socket.SocketHelper;

public class ClientListener implements Runnable
{
	private NgdContext context;
	private Logger log;

	public ClientListener(NgdContext context) throws IOException
	{
		this.context = context;
		this.log = context.getLog();
	}

	@Override
	public void run()
	{
		try(ServerSocket ssocket = SocketHelper.newSSLServerSocket(context.getPort()))
		{
			log.log("监听建立成功：[%s:%s]", context.getHost(), context.getPort());
			while(true)
			{
				Socket socket = ssocket.accept();
				Thread thread = new Thread(new ClientServer(socket, context));
				thread.setDaemon(true);
				thread.start();
			}
		}
		catch(IOException e)
		{
			log.err(e.getMessage());
		}
	}
}
