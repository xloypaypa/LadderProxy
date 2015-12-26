package net2.client;

import data.Data;
import encrypt.RSA;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.packageReader.HttpPackageReader;
import net.tool.packageSolver.packageReader.PackageReader;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;
import net2.IONode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.PublicKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by xlo on 2015/12/25.
 * it's the server connection solver
 */
public class ServerEncryptConnectionSolver extends IONode {

    protected volatile BlockingQueue<byte[]> waitQueue;
    private volatile PackageReader packageReader;
    private volatile boolean isFirst;
    private volatile PublicKey publicKey;

    public ServerEncryptConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage);
        this.waitQueue = new LinkedBlockingDeque<>();
        this.ioNode = ioNode;
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
        if (this.getConnectionMessage().getSelectionKey().isConnectable()) {
            SocketChannel socket = this.getConnectionMessage().getSocket();
            try {
                if (socket.finishConnect()) {
                    byte[] key = RSA.publicKey2Bytes(Data.getKeyPair().getPublic());
                    this.writeBuffer = ByteBuffer.wrap(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                            .setCommand("GET").setHost("server").setUrl("/check").setVersion("HTTP/1.1")
                            .addMessage("Content-Length", key.length + "")
                            .setBody(key).getHttpPackageBytes());
                    updateBufferAndInterestOps();
                    return ConnectionStatus.WAITING;
                } else {
                    return ConnectionStatus.WAITING;
                }
            } catch (IOException e) {
                this.sendException(e);
                return ConnectionStatus.ERROR;
            }
        } else {
            return ConnectionStatus.WAITING;
        }
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
                updateBufferAndInterestOps();
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
        if (this.publicKey == null) {
            this.waitQueue.add(message);
        } else {
            try {
                super.addMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void buildNextBuffer() {
        byte[] buffer = getNextMessage();
        try {
            byte[] encrypt = RSA.encrypt(this.publicKey, buffer);
            writeBuffer = ByteBuffer.wrap(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                    .setCommand("GET").setHost("server").setUrl("/check").setVersion("HTTP/1.1")
                    .addMessage("Content-Length", encrypt.length + "")
                    .setBody(encrypt).getHttpPackageBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ConnectionStatus whenFirst() {
        isFirst = false;
        this.publicKey = RSA.bytes2PublicKey(this.packageReader.getBody());
        while (!this.waitQueue.isEmpty()) {
            this.addMessage(this.waitQueue.poll());
        }
        updateBufferAndInterestOps();
        return ConnectionStatus.READING;
    }

    private ConnectionStatus whenPackage() {
        try {
            byte[] message = RSA.decrypt(Data.getKeyPair().getPrivate(), this.packageReader.getBody());
            this.ioNode.addMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateBufferAndInterestOps();
        return ConnectionStatus.READING;
    }

}
