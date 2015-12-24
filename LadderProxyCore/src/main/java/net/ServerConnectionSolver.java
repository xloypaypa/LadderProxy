package net;

import data.Data;
import encrypt.RSA;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.packageReader.HttpPackageReader;
import net.tool.packageSolver.packageReader.PackageReader;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;

import java.io.IOException;
import java.security.PublicKey;

/**
 * Created by xlo on 15-12-24.
 * it's the server connection solver
 */
public class ServerConnectionSolver extends HostConnectionSolver {

    private PackageReader packageReader;
    private volatile boolean isFirst;
    private volatile PublicKey publicKey;

    public ServerConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage, ioNode);
        byte[] body = RSA.publicKey2Bytes(Data.getKeyPair().getPublic());
        this.addMessage(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                .setCommand("GET").setHost("server").setUrl(".login").setVersion("HTTP/1.1")
                .addMessage("Content-Length", body.length + "")
                .setBody(body).getHttpPackageBytes());
    }

    @Override
    public ConnectionStatus whenInit() {
        super.whenInit();
        this.isFirst = true;
        this.packageReader = new HttpPackageReader(this.getConnectionMessage().getSocket());
        return ConnectionStatus.WAITING;
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
        this.publicKey = RSA.bytes2PublicKey(packageReader.getBody());
        return afterIO();
    }
}
