package ngrok.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public abstract class FileUtil {

    public static InputStream getFileStream(String name) throws FileNotFoundException {
        if (name.toLowerCase().startsWith("classpath:")) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name.substring("classpath:".length()));
            if (is == null) {
                throw new FileNotFoundException(name);
            }
            return is;
        }
        return new FileInputStream(name);
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
