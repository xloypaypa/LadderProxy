package net;

import data.Data;
import encrypt.RSA;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;

/**
 * Created by xlo on 15-12-24.
 * it's the server connection solver
 */
public class ServerConnectionSolver extends HostConnectionSolver {
    public ServerConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage, ioNode);
        byte[] body = RSA.publicKey2Bytes(Data.getKeyPair().getPublic());
        this.addMessage(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                .setCommand("GET").setHost("server").setUrl(".login").setVersion("HTTP/1.1")
                .addMessage("Content-Length", body.length + "")
                .setBody(body).getHttpPackageBytes());
    }
}
