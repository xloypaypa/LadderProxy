package net;

import data.Data;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by xlo on 2015/12/15.
 * it's the browser connection solver
 */
public class BrowserConnectionSolver extends IONode {

    public BrowserConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    public ConnectionStatus whenInit() {
        this.byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.clear();
        return ConnectionStatus.CONNECTING;
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

    @Override
    public ConnectionStatus whenReading() {
        try {
            int len = this.getConnectionMessage().getSocket().read(byteBuffer);
            if (len < 0) {
                return ConnectionStatus.CLOSE;
            } else if (len == 0) {
                afterIO();
                return ConnectionStatus.WAITING;
            } else {
                return ReadOnce(len);
            }
        } catch (IOException e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenClosing() {
        if (ioNode != null) {
            ioNode.setClosed(true);
            ioNode.addMessage("close".getBytes());
        }
        return super.whenClosing();
    }

    protected void connectOtherSolver(String host, int port) throws IOException {
        this.ioNode = new ServerConnectionSolver(new ConnectionMessageImpl(), this);
        OneClient.getClient().connect(host, port, this.ioNode);
    }
}
