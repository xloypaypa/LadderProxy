package net2.client;

import data.Data;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;
import net2.OneClient;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;
import net2.IONode;

import java.io.IOException;

/**
 * Created by xlo on 2015/12/25.
 * it's the browser connection solver
 */
public class BrowserConnectionSolver extends IONode {
    public BrowserConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    public ConnectionStatus whenConnecting() {
        try {
            connectOtherSolver(Data.getServerIp(), Data.getServerPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ConnectionStatus.WAITING;
    }

    protected void connectOtherSolver(String host, int port) throws IOException {
        this.ioNode = new ServerConnectionSolver(new ConnectionMessageImpl(), this);
        OneClient.getClient().connect(host, port, this.ioNode);
        sendPassword();
    }

    private void sendPassword() {
        this.ioNode.addMessage(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                .setCommand("GET").setHost("server").setUrl("/check").setVersion("HTTP/1.1")
                .addMessage("Content-Length", Data.getPassword().getBytes().length + "")
                .setBody(Data.getPassword().getBytes()).getHttpPackageBytes());
    }
}
