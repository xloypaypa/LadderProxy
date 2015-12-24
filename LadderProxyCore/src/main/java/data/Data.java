package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Scanner;

/**
 * Created by xlo on 15-12-13.
 * it's the data
 */
public class Data {
    private volatile static KeyPair keyPair;
    private volatile static String serverIp = "127.0.0.1";
    private volatile static int serverPort = 8000;

    public static int getServerPort() {
        return serverPort;
    }

    public static String getServerIp() {
        return serverIp;
    }

    public static void setServerIp(String serverIp) {
        Data.serverIp = serverIp;
    }

    public static void setServerPort(int serverPort) {
        Data.serverPort = serverPort;
    }

    public static void setKeyPair(KeyPair keyPair) {
        Data.keyPair = keyPair;
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    public static void save() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File("./ladderProxyConfig.ladder"));
        fileOutputStream.write((serverIp + "\r\n").getBytes());
        fileOutputStream.write((serverPort + "\r\n").getBytes());
    }

    public static void load() throws IOException {
        Scanner scanner = new Scanner(new FileInputStream(new File("./ladderProxyConfig.ladder")));
        serverIp = scanner.next();
        serverPort = scanner.nextInt();
        scanner.close();
    }
}
