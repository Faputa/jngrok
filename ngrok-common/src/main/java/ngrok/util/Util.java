/**
 * 其他公用方法
 */
package ngrok.util;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Util {

    private Util() {
    }

    public static String getRandString(int len) {
        final String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(str.charAt(rand.nextInt(str.length())));
        }
        return sb.toString();
    }

    public static String MD5(String str) {
        StringBuilder sb = new StringBuilder();
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes("utf-8"));
            for (byte b : bytes) {
                sb.append(hexArray[(b >> 4) & 0x0F]);
                sb.append(hexArray[b & 0x0F]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getTime(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }

    public static String getTime() {
        return getTime("yyyy-MM-dd HH:mm:ss");
    }
}
