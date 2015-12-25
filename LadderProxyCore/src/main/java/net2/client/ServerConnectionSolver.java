package net2.client;

import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;
import net2.IONode;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by xlo on 2015/12/25.
 * it's the server connection solver
 */
public class ServerConnectionSolver extends IONode {
    public ServerConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage);
        this.ioNode = ioNode;
    }

    @Override
    public ConnectionStatus whenConnecting() {
        if (this.getConnectionMessage().getSelectionKey().isConnectable()) {
            SocketChannel socket = this.getConnectionMessage().getSocket();
            try {
                if (socket.finishConnect()) {
                    return afterIO();
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
}
