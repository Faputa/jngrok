package ngrok;

import ngrok.Protocol.Payload;
import ngrok.model.Tunnel;
import ngrok.util.GsonUtil;
import ngrok.util.ToolUtil;

public class NgMsg {

    private NgMsg() {
    }

    public static String Auth(String AuthToken) {
        Payload payload = new Payload();
        payload.ClientId = "";
        payload.OS = "darwin";
        payload.Arch = "amd64";
        payload.Version = "2";
        payload.MmVersion = "1.7";
        payload.User = "user";
        payload.Password = "";
        payload.AuthToken = AuthToken;
        Protocol protocol = new Protocol();
        protocol.Type = "Auth";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String ReqTunnel(Tunnel tunnel) {
        Payload payload = new Payload();
        payload.ReqId = ToolUtil.getRandString(8);
        payload.Protocol = tunnel.getProtocol();
        payload.Hostname = tunnel.getHostname();
        payload.Subdomain = tunnel.getSubdomain();
        payload.RemotePort = tunnel.getRemotePort();
        payload.HttpAuth = "";
        Protocol protocol = new Protocol();
        protocol.Type = "ReqTunnel";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String RegProxy(String ClientId) {
        Payload payload = new Payload();
        payload.ClientId = ClientId;
        Protocol protocol = new Protocol();
        protocol.Type = "RegProxy";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String Ping() {
        Payload payload = new Payload();
        Protocol protocol = new Protocol();
        protocol.Type = "Ping";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }
}
