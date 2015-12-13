package main;

import net.ClientConnectionSolver;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class ServerMain {

    public static void main(String[] args) {
        Server server = Server.getNewServer("ladder proxy server",
                () -> new ClientConnectionSolver(new ConnectionMessageImpl()));
        server.getInstance(8000, 5);
        server.accept();
    }

}
