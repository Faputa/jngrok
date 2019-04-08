package ngrok;

import ngrok.Protocol.Payload;
import ngrok.util.GsonUtil;

public class NgdMsg {

    private NgdMsg() {
    }

    public static String AuthResp(String ClientId, String Error) {
        Payload payload = new Payload();
        payload.ClientId = ClientId;
        payload.Version = "2";
        payload.MmVersion = "1.7";
        payload.Error = Error;
        Protocol protocol = new Protocol();
        protocol.Type = "AuthResp";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String NewTunnel(String ReqId, String Url, String Protocol, String Error) {
        Payload payload = new Payload();
        payload.ReqId = ReqId;
        payload.Url = Url;
        payload.Protocol = Protocol;
        payload.Error = Error;
        Protocol protocol = new Protocol();
        protocol.Type = "NewTunnel";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String ReqProxy() {
        Payload payload = new Payload();
        Protocol protocol = new Protocol();
        protocol.Type = "ReqProxy";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String StartProxy(String Url) {
        Payload payload = new Payload();
        payload.Url = Url;
        Protocol protocol = new Protocol();
        protocol.Type = "StartProxy";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }

    public static String Pong() {
        Payload payload = new Payload();
        Protocol protocol = new Protocol();
        protocol.Type = "Pong";
        protocol.Payload = payload;
        return GsonUtil.toJson(protocol);
    }
}
