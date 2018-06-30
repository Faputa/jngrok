/**
 * 忽略SSL证书
 * 参考：https://github.com/cyejing/fast-ngrok/blob/master/fast-ngrok-core/src/main/java/cn/cyejing/ngrok/core/SocketFactory.java
 */
package ngrok.socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

public class TrustAllSSLSocketFactory
{
	private SSLSocketFactory ssf;

	public TrustAllSSLSocketFactory() throws Exception
	{
		ssf = trustAllSocketFactory();
	}

	public SSLSocket createSocket(String serverAddress, int serverPort) throws UnknownHostException, IOException
	{
		SSLSocket socket = (SSLSocket) ssf.createSocket(serverAddress, serverPort);
		socket.startHandshake();
		return socket;
	}

	/**
	 * 忽略证书
	 * @return
	 * @throws Exception
	 */
	public SSLSocketFactory trustAllSocketFactory() throws Exception
	{
		TrustManager[] trustAllCerts = new TrustManager[]
		{
			new X509TrustManager()
			{
				@Override
				public X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}
				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType)
				{
				}
				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType)
				{
				}
			}
		};
		SSLContext sslCxt = SSLContext.getInstance("TLSv1.2");
		sslCxt.init(null, trustAllCerts, null);
		return sslCxt.getSocketFactory();
	}
}
