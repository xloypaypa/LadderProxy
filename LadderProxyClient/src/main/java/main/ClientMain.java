package main;

import net.BrowserConnectionSolver;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

/**
 * Created by xlo on 15-12-13.
 * it's the main class
 */
public class ClientMain {

    public static void main(String[] args) {
        Server server = Server.getNewServer("ladder proxy client",
                () -> new BrowserConnectionSolver(new ConnectionMessageImpl()));
        server.getInstance(8080, 5);
        server.accept();
    }

}
