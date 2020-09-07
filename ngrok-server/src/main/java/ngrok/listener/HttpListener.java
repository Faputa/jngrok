/**
 * 监听用户的http请求
 */
package ngrok.listener;

import ngrok.NgdContext;
import ngrok.handler.HttpHandler;
import ngrok.socket.SocketHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HttpListener.class);

    private NgdContext context;

    public HttpListener(NgdContext context) {
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
