package ngrok;

public class Protocol
{
	public static class Payload
	{
		public String ClientId;
		public String OS;
		public String Arch;
		public String Version;
		public String MmVersion;
		public String User;
		public String Password;

		public String ReqId;
		public String Protocol;
		public String Hostname;
		public String Subdomain;
		public String HttpAuth;
		public Integer RemotePort;

		public String Error;
		public String Url;
		public String ClientAddr;

		// 扩展字段，增加token校验功能
		public String AuthToken;
	}

	public String Type;
	public Payload Payload;
}
