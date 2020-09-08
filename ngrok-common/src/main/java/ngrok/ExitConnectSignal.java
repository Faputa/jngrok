package ngrok;

import java.net.Socket;

/**
 * 退出连接信号
 */
public class ExitConnectSignal extends Exception {

    private static final long serialVersionUID = 5678601433752028922L;

    private Socket socket;

    public ExitConnectSignal(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public String toString() {
        return socket.toString();
    }
}
