package net;

import data.Data;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by xlo on 2015/12/15.
 * it's the browser connection solver
 */
public class BrowserConnectionSolver extends IONode {

    private ByteBuffer byteBuffer;
    protected volatile IONode ioNode;

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
                byteBuffer.flip();
                byte[] message = new byte[len];
                for (int i = 0; i < len; i++) {
                    message[i] = byteBuffer.get();
                }
                this.ioNode.addMessage(message);
                byteBuffer.clear();
                return ConnectionStatus.READING;
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
        this.ioNode = new HostConnectionSolver(new ConnectionMessageImpl(), this);
        OneClient.getClient().connect(host, port, this.ioNode);
        this.ioNode.addMessage(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
        .setCommand("GET").setHost("server").setUrl(".login").setVersion("HTTP/1.1").addMessage("Password", Data.getPassword())
                .getHttpPackageBytes());
    }
}
