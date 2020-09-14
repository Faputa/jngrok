package ngrok;

import java.net.Socket;

/**
 * 退出连接异常
 */
public class ExitConnectException extends Exception {

    private static final long serialVersionUID = 5678601433752028922L;

    private Socket socket;

    public ExitConnectException(Socket socket) {
        super();
        this.socket = socket;
    }

    @Override
    public String toString() {
        return socket.toString();
    }
}
