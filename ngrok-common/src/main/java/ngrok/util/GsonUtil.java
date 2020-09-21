package ngrok.util;

public class GsonUtil {

    private GsonUtil() {
    }

    private static com.google.gson.GsonBuilder builder = new com.google.gson.GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String toJson(Object object) {
        return builder.excludeFieldsWithoutExposeAnnotation().create().toJson(object);
    }

    public static <T> T toBean(String json, Class<T> classOfT) {
        return builder.excludeFieldsWithoutExposeAnnotation().create().fromJson(json, classOfT);
    }
}
