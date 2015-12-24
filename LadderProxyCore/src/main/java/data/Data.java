package data;

import encrypt.AES;

import java.io.*;
import java.util.Scanner;

/**
 * Created by xlo on 15-12-13.
 * it's the data
 */
public class Data {
    private volatile static String password = "123";
    private volatile static byte[] key;
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

    public static byte[] getKey() {
        if (key == null) {
            synchronized (Data.class) {
                if (key == null) {
                    key = AES.getRawKey(password);
                    for (byte now : key) {
                        System.out.println(now);
                    }
                }
            }
        }
        return key;
    }

    public static void save() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File("./ladderProxyConfig.ladder"));
        fileOutputStream.write((password + "\r\n").getBytes());
        fileOutputStream.write((serverIp + "\r\n").getBytes());
        fileOutputStream.write((serverPort + "\r\n").getBytes());
    }

    public static void load() throws IOException {
        Scanner scanner = new Scanner(new FileInputStream(new File("./ladderProxyConfig.ladder")));
        password = scanner.next();
        serverIp = scanner.next();
        serverPort = scanner.nextInt();
        scanner.close();
    }
}
