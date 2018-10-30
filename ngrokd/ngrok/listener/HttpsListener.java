/**
 * 监听用户的https请求
 */
package ngrok.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ngrok.log.Logger;
import ngrok.NgdContext;
import ngrok.server.HttpServer;
import ngrok.socket.SocketHelper;

public class HttpsListener implements Runnable
{
	private NgdContext context;
	private Logger log;

	public HttpsListener(NgdContext context)
	{
		this.context = context;
		this.log = context.log;
	}

	@Override
	public void run()
	{
		try(ServerSocket ssocket = SocketHelper.newSSLServerSocket(context.httpsPort))
		{
			log.log("监听建立成功：[%s:%s]", context.host, context.httpsPort);
			while(true)
			{
				Socket socket = ssocket.accept();
				Thread thread = new Thread(new HttpServer(socket, context, "https"));
				thread.setDaemon(true);
				thread.start();
			}
		}
		catch(IOException e)
		{
			log.log("监听退出：[%s:%s]", context.host, context.httpsPort);
		}
	}
}
