package ngrok.client;

import ngrok.client.model.Tunnel;
import ngrok.common.Protocol;
import ngrok.common.Protocol.Payload;
import ngrok.util.GsonUtil;
import ngrok.util.Util;

public class Message {

    private Message() {
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
        payload.ReqId = Util.getRandString(8);
        payload.Protocol = tunnel.protocol;
        payload.Hostname = tunnel.hostname;
        payload.Subdomain = tunnel.subdomain;
        payload.RemotePort = tunnel.remotePort;
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
