/**
 * 监听Ngrok的连接请求
 */
package ngrok.server.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.server.Context;
import ngrok.server.handler.ClientHandler;
import ngrok.socket.SocketHelper;

public class ClientListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientListener.class);

    private Context context;

    public ClientListener(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (
            ServerSocket ssocket = context.useSsl
            ? SocketHelper.newSSLServerSocket(context.port)
            : SocketHelper.newServerSocket(context.port)
        ) {
            log.info("监听建立成功：[{}:{}]", context.host, context.port);
            while (true) {
                Socket socket = ssocket.accept();
                Thread thread = new Thread(new ClientHandler(socket, context));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }
}
