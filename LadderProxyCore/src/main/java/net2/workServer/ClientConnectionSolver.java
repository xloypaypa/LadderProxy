package net2.workServer;

import data.Data;
import encrypt.RSA;
import net2.client.BrowserConnectionSolver;
import net2.OneClient;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.headSolver.HttpRequestHeadSolver;
import net.tool.packageSolver.packageReader.HttpPackageReader;
import net.tool.packageSolver.packageReader.PackageReader;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpReplyPackageWriterFactory;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;

import java.io.IOException;
import java.security.PublicKey;

/**
 * Created by xlo on 2015/12/15.
 * it's the connected node solver
 */
public class ClientConnectionSolver extends BrowserConnectionSolver {

    private PackageReader packageReader;
    private volatile boolean isFirst;

    public ClientConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    public ConnectionStatus whenInit() {
        super.whenInit();
        this.isFirst = true;
        this.packageReader = new HttpPackageReader(this.getConnectionMessage().getSocket());
        return ConnectionStatus.WAITING;
    }

    @Override
    public ConnectionStatus whenConnecting() {
        return null;
    }

    @Override
    public ConnectionStatus whenReading() {
        if (this.isFirst) {
            try {
                PackageStatus packageStatus = packageReader.read();
                if (packageStatus.equals(PackageStatus.END)) {
                    return whenFirst();
                } else if (packageStatus.equals(PackageStatus.WAITING)) {
                    afterIO();
                    return ConnectionStatus.WAITING;
                } else if (packageStatus.equals(PackageStatus.ERROR) || packageStatus.equals(PackageStatus.CLOSED)) {
                    return ConnectionStatus.CLOSE;
                } else {
                    return ConnectionStatus.READING;
                }
            } catch (IOException e) {
                this.sendException(e);
                return ConnectionStatus.ERROR;
            }
        } else {
            return super.whenReading();
        }
    }

    private ConnectionStatus whenFirst() throws IOException {
        isFirst = false;
        HttpRequestHeadSolver httpRequestHeadSolver = (HttpRequestHeadSolver) packageReader.getHeadPart();
        connectOtherSolver(httpRequestHeadSolver.getHost(), httpRequestHeadSolver.getPort());

        if (httpRequestHeadSolver.getCommand().equals("CONNECT")) {
            this.addMessage(HttpReplyPackageWriterFactory.getHttpReplyPackageWriterFactory()
                    .setVersion("HTTP/1.1").setMessage("OK").setReply(200).getHttpPackageBytes());
        } else {
            this.ioNode.addMessage(this.packageReader.getPackage());
            this.ioNode.addMessage(this.packageReader.stop());
        }
        return afterIO();
    }

    @Override
    protected void connectOtherSolver(String host, int port) throws IOException {
        this.ioNode = new HostConnectionSolver(new ConnectionMessageImpl(), this);
        OneClient.getClient().connect(host, port, this.ioNode);
    }
}
