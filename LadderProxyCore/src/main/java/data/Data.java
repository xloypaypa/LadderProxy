package data;

/**
 * Created by xlo on 15-12-13.
 * it's the data
 */
public class Data {
    private volatile static String password = "123";
    private volatile static String serverIp = "127.0.0.1";
    private volatile static int serverPort = 8000;

    public static String getPassword() {
        return password;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getServerIp() {
        return serverIp;
    }

    public static void setPassword(String password) {
        Data.password = password;
    }

    public static void setServerIp(String serverIp) {
        Data.serverIp = serverIp;
    }

    public static void setServerPort(int serverPort) {
        Data.serverPort = serverPort;
    }
}
