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

    private BrowserConnectionSolver browserConnectionSolver;

    public ProxyServerConnectionSolver(BrowserConnectionSolver browserConnectionSolver) {
        super(new ConnectionMessageImpl());
        this.browserConnectionSolver = browserConnectionSolver;
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
            socketChannel.connect(new InetSocketAddress(Data.getServerIp(), Data.getServerPort()));

            this.getConnectionMessage().setSocket(socketChannel);
            ConnectionManager.getSolverManager().putSolver(socketChannel.socket(), this);

            SelectorManager.getSelectorManager().getOneSelectorThread("ladder proxy client")
                    .addSocketChannel(socketChannel, SelectionKey.OP_CONNECT);
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
                    return afterIO();
                } else {
                    return ConnectionStatus.CONNECTING;
                }
            } catch (IOException e) {
                return ConnectionStatus.ERROR;
            }
        } else {
            return ConnectionStatus.CONNECTING;
        }
    }

    @Override
    public ConnectionStatus whenWaiting() {
        if (this.getConnectionStatus().equals(ConnectionStatus.CONNECTING)) {
            return ConnectionStatus.CONNECTING;
        }
        return super.whenWaiting();
    }

    @Override
    protected AbstractSolver getOther() {
        return this.browserConnectionSolver;
    }
}
