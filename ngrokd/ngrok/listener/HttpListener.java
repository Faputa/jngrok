/**
 * 监听用户的http请求
 */
package ngrok.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.server.HttpServer;
import ngrok.socket.SocketHelper;

public class HttpListener implements Runnable
{
	private NgdContext context;
	private Logger log;

	public HttpListener(NgdContext context)
	{
		this.context = context;
		this.log = context.log;
	}

	@Override
	public void run()
	{
		try(ServerSocket ssocket = SocketHelper.newServerSocket(context.httpPort))
		{
			log.log("监听建立成功：[%s:%s]", context.host, context.httpPort);
			while(true)
			{
				Socket socket = ssocket.accept();
				Thread thread = new Thread(new HttpServer(socket, context, "http"));
				thread.setDaemon(true);
				thread.start();
			}
		}
		catch(IOException e)
		{
			log.err(e.toString());
		}
	}
}
