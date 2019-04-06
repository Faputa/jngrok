/**
 * 监听Ngrok的连接请求
 */
package ngrok.listener;

import ngrok.NgdContext;
import ngrok.handler.ClientHandler;
import ngrok.log.Logger;
import ngrok.socket.SocketHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener implements Runnable {

    private NgdContext context;
    private Logger log = Logger.getLogger();

    public ClientListener(NgdContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (ServerSocket ssocket = SocketHelper.newSSLServerSocket(context.port)) {
            log.info("监听建立成功：[%s:%s]", context.host, context.port);
            while (true) {
                Socket socket = ssocket.accept();
                Thread thread = new Thread(new ClientHandler(socket, context));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            log.err(e.toString());
        }
    }
}
