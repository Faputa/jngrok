/**
 * 监听用户的tcp请求
 */
package ngrok.listener;

import ngrok.NgdContext;
import ngrok.handler.TcpHandler;
import ngrok.log.Logger;

import java.net.ServerSocket;
import java.net.Socket;

public class TcpListener implements Runnable {

    private ServerSocket ssocket;
    private NgdContext context;
    private Logger log = Logger.getLogger();

    public TcpListener(ServerSocket ssocket, NgdContext context) {
        this.ssocket = ssocket;
        this.context = context;
    }

    @Override
    public void run() {
        try (ServerSocket ssocket = this.ssocket) {
            log.info("监听建立成功：[%s:%s]", context.host, ssocket.getLocalPort());
            while (true) {
                Socket socket = ssocket.accept();
                Thread thread = new Thread(new TcpHandler(socket, context));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (Exception e) {
            log.info("监听退出：[%s:%s]", context.host, ssocket.getLocalPort());
        }
    }
}
