package main;

import data.Data;
import encrypt.RSA;
import net2.client.BrowserConnectionSolver;
import net2.OneClient;
import net.server.Client;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

import java.io.IOException;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class ClientMain {

    public static void main(String[] args) throws Exception {
        Data.setKeyPair(RSA.buildKeyPair());

        try {
            Data.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (args.length == 4) {
            startServer(args[0], args[1], args[2]);
        } else {
            new MainPage().create();
        }
    }

    public static void startServer(String ip, String serverPort, String localPort) {
        Data.setServerIp(ip);
        Data.setServerPort(Integer.valueOf(serverPort));

        Client client = OneClient.getClient();
        client.getInstance(1);

        try {
            Data.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                Server server = Server.getNewServer("ladder proxy client",
                        () -> new BrowserConnectionSolver(new ConnectionMessageImpl()));
                server.getInstance(Integer.valueOf(localPort), 5);
                server.accept();
            }
        }.start();
    }

}
