package net2.workServer;

import net2.IONode;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by xlo on 2015/12/15.
 * it's the host connection solver
 */
public class HostConnectionSolver extends IONode {

    public HostConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage);
        this.ioNode = ioNode;
    }

    @Override
    public ConnectionStatus whenConnecting() {
        if (this.getConnectionMessage().getSelectionKey().isConnectable()) {
            SocketChannel socket = this.getConnectionMessage().getSocket();
            try {
                if (socket.finishConnect()) {
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

}
