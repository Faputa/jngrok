package ngrok.socket;

import ngrok.util.ByteUtil;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SocketHelper {

    private SocketHelper() {
    }

    public static Socket newSocket(String host, int port, int timeout) throws IOException {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(timeout);
        return socket;
    }

    public static SSLSocket newSSLSocket(String host, int port, int timeout) throws Exception {
        TrustAllSSLSocketFactory sf = new TrustAllSSLSocketFactory();
        SSLSocket socket = sf.createSocket(host, port);
        socket.setSoTimeout(timeout);
        return socket;
    }

    public static ServerSocket newServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    public static SSLServerSocket newSSLServerSocket(int port) throws IOException {
        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        SSLServerSocket ssocket = (SSLServerSocket) ssf.createServerSocket(port);
        ssocket.setNeedClientAuth(false);
        return ssocket;
    }

    public static void safeClose(@Nullable Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void safeClose(@Nullable ServerSocket serverSocket) {
        if (serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void sendpack(Socket socket, String msg) throws IOException {
        OutputStream os = socket.getOutputStream();
        byte[] bs = msg.getBytes();
        os.write(ByteUtil.concat(ByteUtil.encodeInt(bs.length), bs));
        os.flush();
    }

    public static void sendbuf(Socket socket, byte[] buf) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(buf);
        os.flush();
    }

    public static byte[] recvbuf(Socket socket) throws IOException {
        InputStream is = socket.getInputStream();
        int len;
        byte[] buf = new byte[1024];
        if ((len = is.read(buf)) == -1) {
            return null;
        }
        return ByteUtil.subArr(buf, 0, len);
    }

    public static Map<String, String> readHttpHead(Socket socket) throws IOException {
        Map<String, String> map = new HashMap<>();
        InputStream is = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            String[] ss = line.split(": ");
            if (ss.length == 1) {
                // do nothing
            } else if (ss.length == 2) {
                map.put(ss[0], ss[1]);
            }
        }
        return map;
    }

    @Nullable
    public static Map<String, String> readHttpHead(byte[] buf) throws IOException {
        Map<String, String> map = new HashMap<>();
        InputStream is = new ByteArrayInputStream(buf);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isEmpty()) {
                return map;
            }
            String[] ss = line.split(": ");
            if (ss.length == 1) {
                // do nothing
            } else if (ss.length == 2) {
                map.put(ss[0], ss[1]);
            }
        }
        return null;// 如果没有完整读取head会返回null
    }

    public static void forward(Socket s1, Socket s2) throws IOException {
        InputStream is = s1.getInputStream();
        OutputStream os = s2.getOutputStream();
        int len;
        byte[] buf = new byte[1024];
        while ((len = is.read(buf)) != -1) {
            os.write(buf, 0, len);
        }
    }
}
