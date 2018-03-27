package ngrok.model;

import java.net.ServerSocket;
import java.net.Socket;

public class TunnelInfo
{
	private String clientId;
	private Socket controlSocket;
	private ServerSocket tcpServerSocket;

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public Socket getControlSocket()
	{
		return controlSocket;
	}

	public void setControlSocket(Socket controlSocket)
	{
		this.controlSocket = controlSocket;
	}

	public ServerSocket getTcpServerSocket()
	{
		return tcpServerSocket;
	}

	public void setTcpServerSocket(ServerSocket tcpServerSocket)
	{
		this.tcpServerSocket = tcpServerSocket;
	}
}
