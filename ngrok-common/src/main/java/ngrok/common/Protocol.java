package ngrok.common;

import com.google.gson.annotations.Expose;

public class Protocol {

    public static class Payload {

        @Expose public String ClientId;
        @Expose public String OS;
        @Expose public String Arch;
        @Expose public String Version;
        @Expose public String MmVersion;
        @Expose public String User;
        @Expose public String Password;

        @Expose public String ReqId;
        @Expose public String Protocol;
        @Expose public String Hostname;
        @Expose public String Subdomain;
        @Expose public String HttpAuth;
        @Expose public Integer RemotePort;

        @Expose public String Error;
        @Expose public String Url;
        @Expose public String ClientAddr;
    }

    @Expose public String Type;
    @Expose public Payload Payload;
}
