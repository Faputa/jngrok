package ngrok.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static String readTextStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            int len;
            char[] buf = new char[1024];
            while ((len = br.read(buf)) != -1) {
                sb.append(new String(buf, 0, len));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String readTextFile(String name) {
        return readTextStream(getFileStream(name));
    }
}
