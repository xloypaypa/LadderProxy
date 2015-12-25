package net2.encryptServer;

import data.Data;
import encrypt.RSA;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.packageReader.HttpPackageReader;
import net.tool.packageSolver.packageReader.PackageReader;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;
import net2.IONode;
import net2.OneClient;

import java.io.IOException;
import java.security.PublicKey;

/**
 * Created by xlo on 15-12-25.
 * it's the client encrypt connection
 */
public class ClientEncryptConnection extends IONode {

    private volatile PackageReader packageReader;
    private volatile boolean isFirst;
    private PublicKey publicKey;

    public ClientEncryptConnection(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    public ConnectionStatus whenInit() {
        super.whenInit();
        this.isFirst = true;
        this.packageReader = new HttpPackageReader(this.getConnectionMessage().getSocket());
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

    protected void connectOtherSolver(String host, int port) throws IOException {
        this.ioNode = new HostEncryptConnectionSolver(new ConnectionMessageImpl(), this);
        OneClient.getClient().connect(host, port, this.ioNode);
    }

    @Override
    public ConnectionStatus whenReading() {
        try {
            PackageStatus packageStatus = packageReader.read();
            if (packageStatus.equals(PackageStatus.END)) {
                if (this.isFirst) {
                    return whenFirst();
                } else {
                    return whenPackage();
                }

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
    }

    @Override
    public void addMessage(byte[] message) {
        try {
            byte[] encrypt = RSA.encrypt(this.publicKey, message);
            super.addMessage(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                    .setCommand("GET").setHost("server").setUrl("/check").setVersion("HTTP/1.1")
                    .addMessage("Content-Length", encrypt.length + "")
                    .setBody(encrypt).getHttpPackageBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ConnectionStatus whenPackage() {
        try {
            this.ioNode.addMessage(RSA.decrypt(Data.getKeyPair().getPrivate(), this.packageReader.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ConnectionStatus.READING;
    }

    private ConnectionStatus whenFirst() {
        isFirst = false;
        this.publicKey = RSA.bytes2PublicKey(this.packageReader.getBody());
        byte[] key = RSA.publicKey2Bytes(Data.getKeyPair().getPublic());
        this.messageQueue.add(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                .setCommand("GET").setHost("server").setUrl("/check").setVersion("HTTP/1.1")
                .addMessage("Content-Length", key.length + "")
                .setBody(key).getHttpPackageBytes());
        afterIO();
        return ConnectionStatus.READING;
    }
}
