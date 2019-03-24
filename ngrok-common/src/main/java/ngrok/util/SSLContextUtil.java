package ngrok.util;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public class SSLContextUtil {

    private SSLContextUtil() {
    }

    public static void createDefaultSSLContext(InputStream keyStream, String keyPassword) throws Exception {
        createDefaultSSLContext(keyStream, keyPassword.toCharArray(), null, null);
    }

    public static void createDefaultSSLContext(InputStream keyStream, char[] keyPassword, InputStream trustStream, char[] trustPassword) throws Exception {
        // Get keyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // load the stream to your store
        keyStore.load(keyStream, keyPassword);

        // initialize a key manager factory with the key store
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(keyStore, keyPassword);

        // get the key managers from the factory
        KeyManager[] keyManagers = keyFactory.getKeyManagers();

        // Now get trustStore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // load the stream to your store
        trustStore.load(trustStream, trustPassword);

        // initialize a trust manager factory with the trusted store
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(trustStore);

        // get the trust managers from the factory
        TrustManager[] trustManagers = trustFactory.getTrustManagers();

        // initialize an SSL context to use these managers and set as default
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, null);
        SSLContext.setDefault(sslContext);
    }
}
