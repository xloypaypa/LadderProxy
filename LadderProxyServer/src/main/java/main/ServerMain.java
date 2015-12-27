package main;

import data.Data;
import encrypt.RSA;
import net2.workServer.ClientConnectionSolver;
import net2.OneClient;
import net.server.Client;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class ServerMain {

    public static void main(String[] args) throws Exception {
        Data.setKeyPair(RSA.buildKeyPair());

        int port = 8000;
        int num = 5;

        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        if (args.length > 1) {
            num = Integer.valueOf(args[1]);
        }

        Client client = OneClient.getClient();
        client.getInstance(num);

        Server server = Server.getNewServer("ladder proxy server",
                () -> new ClientConnectionSolver(new ConnectionMessageImpl()));
        server.getInstance(port, num);
        server.accept();
    }

}
