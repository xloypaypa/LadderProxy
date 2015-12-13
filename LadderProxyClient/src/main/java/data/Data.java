package data;

/**
 * Created by xlo on 15-12-13.
 * it's the data
 */
public class Data {
    private final static String password = "123";
    private final static String serverIp = "127.0.0.1";
    private final static int serverPort = 8000;

    public static String getPassword() {
        return password;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getServerIp() {
        return serverIp;
    }
}
