package ngrok;

import java.util.ArrayList;
import java.util.List;

import ngrok.model.Tunnel;

public class NgConfig
{
	public List<Tunnel> tunnelList = new ArrayList<Tunnel>();
	public String serverHost = "";
	public int serverPort = 4443;
	public boolean enableLog = true;
	public long pingTime = 10000;
}
