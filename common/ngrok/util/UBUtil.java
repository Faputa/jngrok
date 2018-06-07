/**
 * 一些难以归类的公用方法
 */
package ngrok.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class UBUtil
{
	private UBUtil()
	{
	}

	public static String getRandString(int len)
	{
		final String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		for(int i = 0; i < len; i++)
		{
			sb.append(str.charAt(rand.nextInt(str.length())));
		}
		return sb.toString();
	}

	public static String getLocation(String path)
	{
		String location = UBUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		if(location.endsWith(".jar"))
		{
			location = location.substring(0, location.lastIndexOf("/") + 1);
		}
		return location + path;
	}

	public static String readTextFile(String filePath)
	{
		StringBuilder sb = new StringBuilder();
		File file = new File(filePath);
		if(file.isFile())
		{
			try(BufferedReader br = new BufferedReader(new FileReader(file)))
			{
				int len;
				char[] buf = new char[1024];
				while((len = br.read(buf)) != -1)
				{
					sb.append(new String(buf, 0, len));
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String MD5(String str)
	{
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] bytes = md.digest(str.getBytes("utf-8"));
			StringBuilder sb = new StringBuilder();
			for(byte b : bytes)
			{
				sb.append(hexArray[(b >> 4) & 0x0F]);
				sb.append(hexArray[b & 0x0F]);
			}
			return sb.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}

	public static String getTime(String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(new Date());
	}

	public static String getTime()
	{
		return getTime("yyyy-MM-dd HH:mm:ss");
	}
}
