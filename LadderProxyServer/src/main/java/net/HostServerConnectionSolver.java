package net;

import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionManager.SelectorManager;
import net.tool.connectionSolver.ConnectionMessageImpl;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by xlo on 15-12-13.
 * it's the proxy server connection solver
 */
public class HostServerConnectionSolver extends AbstractSolver {

    private final String host;
    private volatile int port;

    private volatile boolean isConnect;
    private volatile ClientConnectionSolver clientConnectionSolver;

    public HostServerConnectionSolver(ClientConnectionSolver clientConnectionSolver, String host, int port) {
        super(new ConnectionMessageImpl());
        this.host = host;
        this.port = port;
        this.clientConnectionSolver = clientConnectionSolver;
        this.isConnect = false;
        whenInit();
        this.connectionStatus = ConnectionStatus.CONNECTING;
    }

    @Override
    public ConnectionStatus whenInit() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));

            this.getConnectionMessage().setSocket(socketChannel);
            ConnectionManager.getSolverManager().putSolver(socketChannel.socket(), this);

            SelectorManager.getSelectorManager().getOneSelectorThread("ladder proxy server")
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
        return this.clientConnectionSolver;
    }
}
