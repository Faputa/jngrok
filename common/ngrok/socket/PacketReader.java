package ngrok.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import ngrok.util.ByteUtil;

public class PacketReader
{
	private Socket socket;
	private byte[] para = new byte[0];

	public PacketReader(Socket socket)
	{
		this.socket = socket;
	}

	public String read() throws IOException
	{
		while(true)
		{
			if(para.length >= 8)
			{
				int size = ByteUtil.unpackInt(ByteUtil.subArr(para, 0, 8));
				if(para.length >= size + 8)
				{
					String str = new String(para, 8, size);
					para = ByteUtil.subArr(para, size + 8);
					return str;
				}
			}
			InputStream is = socket.getInputStream();
			byte[] buf = new byte[1024];
			int len = is.read(buf);
			if(len == -1)
			{
				return null;
			}
			para = ByteUtil.concat(para, buf, len);
		}
	}

	public void clean()
	{
		para = new byte[0];
	}
}
