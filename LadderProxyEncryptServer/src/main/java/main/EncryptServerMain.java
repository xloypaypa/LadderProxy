package main;

import data.Data;
import encrypt.RSA;
import net2.encryptServer.ClientEncryptConnection;
import net2.workServer.ClientConnectionSolver;
import net2.OneClient;
import net.server.Client;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class EncryptServerMain {

    public static void main(String[] args) throws Exception {
        Data.setKeyPair(RSA.buildKeyPair());

        int port = 8000;
        int num = 1;

        Data.setServerIp("127.0.0.1");
        Data.setServerPort(8001);
        if (args.length > 0) {
            Data.setServerIp(args[0]);
        }
        if (args.length > 1) {
            Data.setServerPort(Integer.parseInt(args[1]));
        }
        if (args.length > 2) {
            port = Integer.valueOf(args[2]);
        }
        if (args.length > 3) {
            num = Integer.valueOf(args[3]);
        }

        Client client = OneClient.getClient();
        client.getInstance(num);

        Server server = Server.getNewServer("ladder proxy encrypt server",
                () -> new ClientEncryptConnection(new ConnectionMessageImpl()));
        server.getInstance(port, num);
        server.accept();
    }

}
