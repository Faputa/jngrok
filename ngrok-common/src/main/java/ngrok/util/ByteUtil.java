package ngrok.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ByteUtil {

    private ByteUtil() {
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            sb.append("\\x");
            sb.append(hexArray[v >>> 4]);
            sb.append(hexArray[v & 0x0F]);
        }
        return sb.toString();
    }

    public static byte[] encodeInt(int x) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(x);
        buf.putInt(0);
        return buf.array();
    }

    public static int decodeInt(byte[] byteArr) {
        ByteBuffer buf = ByteBuffer.wrap(byteArr);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getInt(0);
    }

    public static byte[] concat(byte[] arr1, byte[] arr2, int count) {
        byte[] newArr = Arrays.copyOf(arr1, arr1.length + count);
        System.arraycopy(arr2, 0, newArr, arr1.length, count);
        return newArr;
    }

    public static byte[] concat(byte[] arr1, byte[] arr2) {
        return concat(arr1, arr2, arr2.length);
    }

    public static byte[] subArr(byte[] arr, int begin, int count) {
        byte[] newArr = new byte[count];
        System.arraycopy(arr, begin, newArr, 0, count);
        return newArr;
    }

    public static byte[] subArr(byte[] arr, int begin) {
        return subArr(arr, begin, arr.length - begin);
    }
}
