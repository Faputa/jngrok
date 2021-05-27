package ngrok.socket;

import ngrok.util.ByteUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

public class PacketReader {

    private Socket socket;
    private byte[] buf = new byte[0];

    public PacketReader(Socket socket) {
        this.socket = socket;
    }

    public PacketReader(Socket socket, int timeout) throws SocketException {
        this.socket = socket;
        this.socket.setSoTimeout(timeout);
    }

    @Nullable
    public String read() throws IOException {
        while (true) {
            if (buf.length >= 8) {
                int size = ByteUtil.decodeInt(ByteUtil.subArr(buf, 0, 8));
                if (buf.length >= size + 8) {
                    String str = new String(buf, 8, size);
                    buf = ByteUtil.subArr(buf, size + 8);
                    return str;
                }
            }
            InputStream is = socket.getInputStream();
            byte[] bs = new byte[1024];
            int len = is.read(bs);
            if (len == -1) {
                return null;
            }
            buf = ByteUtil.concat(buf, bs, len);
        }
    }

    @Nullable
    public String read(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
        return read();
    }

    public void clean() {
        buf = new byte[0];
    }

    public byte[] getBuf() {
        return buf;
    }
}
