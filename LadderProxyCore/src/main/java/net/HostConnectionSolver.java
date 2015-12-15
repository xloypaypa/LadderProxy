package net;

import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by xlo on 2015/12/15.
 * it's the host connection solver
 */
public class HostConnectionSolver extends IONode {

    private ByteBuffer byteBuffer;
    private volatile IONode ioNode;

    public HostConnectionSolver(ConnectionMessage connectionMessage, IONode ioNode) {
        super(connectionMessage);
        this.ioNode = ioNode;
    }

    @Override
    public ConnectionStatus whenInit() {
        this.byteBuffer = ByteBuffer.allocate(1024);
        return ConnectionStatus.CONNECTING;
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

    @Override
    public ConnectionStatus whenReading() {
        try {
            int len = this.getConnectionMessage().getSocket().read(byteBuffer);
            if (len < 0) {
                return ConnectionStatus.CLOSE;
            } else if (len == 0) {
                afterIO();
                return ConnectionStatus.WAITING;
            } else {
                byteBuffer.flip();
                byte[] message = new byte[len];
                for (int i = 0; i < len; i++) {
                    message[i] = byteBuffer.get();
                }
                this.ioNode.addMessage(message);
                byteBuffer.clear();
                return ConnectionStatus.READING;
            }
        } catch (IOException e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenClosing() {
        ioNode.setClosed(true);
        return super.whenClosing();
    }

}
