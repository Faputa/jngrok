/**
 * 监听用户的http请求
 */
package ngrok.server.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ngrok.server.Context;
import ngrok.server.handler.HttpHandler;
import ngrok.socket.SocketHelper;

public class HttpListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HttpListener.class);

    private Context context;

    public HttpListener(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        try (ServerSocket ssocket = SocketHelper.newServerSocket(context.httpPort)) {
            log.info("监听建立成功：[{}:{}]", context.host, context.httpPort);
            while (true) {
                Socket socket = ssocket.accept();
                Thread thread = new Thread(new HttpHandler(socket, context, "http"));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            log.error(e.toString());
        }
    }
}
