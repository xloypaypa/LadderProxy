package main;

import data.Data;
import net.ClientConnectionSolver;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class ServerMain {

    public static void main(String[] args) {
        int port = 8000;
        int num = 5;
        String password = "123";

        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        if (args.length > 1) {
            num = Integer.valueOf(args[1]);
        }
        if (args.length > 2) {
            password = args[2];
        }

        Data.setPassword(password);

        Server server = Server.getNewServer("ladder proxy server",
                () -> new ClientConnectionSolver(new ConnectionMessageImpl()));
        server.getInstance(port, num);
        server.accept();
    }

}
