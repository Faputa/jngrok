package ngrok;

import java.net.Socket;

/**
 * 关闭套接字信号
 */
public class CloseSocketSignal extends Exception {

    private static final long serialVersionUID = 5678601433752028922L;

    private Socket socket;

    public CloseSocketSignal(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public String toString() {
        return socket.toString();
    }
}
