package ngrok.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    private FileUtil() {
    }

    public static InputStream getFileStream(String name) {
        if (name.toLowerCase().startsWith("classpath:")) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(name.substring("classpath:".length()));
        }
        try {
            return new FileInputStream(name);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String readTextStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        int len;
        char[] buf = new char[1024];
        while ((len = br.read(buf)) != -1) {
            sb.append(new String(buf, 0, len));
        }
        return sb.toString();
    }

    public static String readTextFile(String name) throws IOException {
        return readTextStream(getFileStream(name));
    }
}
