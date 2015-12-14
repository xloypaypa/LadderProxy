package net;

import data.Data;
import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionManager.SelectorManager;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.packageWriter.packageWriterFactory.HttpRequestPackageWriterFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by xlo on 15-12-13.
 * it's the proxy server connection solver
 */
public class ProxyServerConnectionSolver extends AbstractSolver {

    private volatile boolean isConnect;
    private volatile BrowserConnectionSolver browserConnectionSolver;

    public ProxyServerConnectionSolver(BrowserConnectionSolver browserConnectionSolver) {
        super(new ConnectionMessageImpl());
        this.browserConnectionSolver = browserConnectionSolver;
        this.isConnect = false;
        whenInit();
        this.connectionStatus = ConnectionStatus.CONNECTING;
    }

    @Override
    public ConnectionStatus whenInit() {
        this.addBytes(HttpRequestPackageWriterFactory.getHttpReplyPackageWriterFactory()
                .setCommand("GET")
                .setHost("server")
                .setUrl("/login")
                .setVersion("HTTP/1.1")
                .addMessage("Password", Data.getPassword())
                .getHttpPackageBytes());

        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            this.getConnectionMessage().setSocket(socketChannel);
            this.getConnectionMessage().setServerName("ladder proxy client");

            ConnectionManager.getSolverManager().putSolver(socketChannel.socket(), this);
            SelectorManager.getSelectorManager().getOneSelectorThread("ladder proxy client")
                    .addSocketChannel(socketChannel, SelectionKey.OP_CONNECT);

            socketChannel.connect(new InetSocketAddress(Data.getServerIp(), Data.getServerPort()));
            return ConnectionStatus.WAITING;
        } catch (IOException e) {
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenConnecting() {
        if (this.getConnectionMessage().getSelectionKey().isConnectable()) {
            SocketChannel socket = this.getConnectionMessage().getSocket();
            try {
                if (socket.finishConnect()) {
                    isConnect = true;
                    return afterIO();
                } else {
                    return ConnectionStatus.WAITING;
                }
            } catch (IOException e) {
                return ConnectionStatus.ERROR;
            }
        } else {
            return ConnectionStatus.WAITING;
        }
    }

    @Override
    public ConnectionStatus whenWaiting() {
        if (!isConnect) {
            return ConnectionStatus.CONNECTING;
        }
        return super.whenWaiting();
    }

    @Override
    protected AbstractSolver getOther() {
        return this.browserConnectionSolver;
    }
}
