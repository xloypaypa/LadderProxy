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
    public void addMessage(byte[] message) {
//        System.out.println(new String(message));
        super.addMessage(message);
    }

    @Override
    protected byte[] decryptMessage(byte[] message) {
//        System.out.println(new String(message));
        return super.decryptMessage(message);
    }

    int all = 0;

    @Override
    public ConnectionStatus whenReading() {
        try {
            int len = this.getConnectionMessage().getSocket().read(byteBuffer);
            all += len;
            if (len < 0) {
                System.out.println(this.getConnectionMessage().getSocket().socket().isClosed());
                return ConnectionStatus.CLOSE;
            } else if (len == 0) {
                updateBufferAndInterestOps();
                return ConnectionStatus.WAITING;
            } else {
                readOnce(len);
                return ConnectionStatus.READING;
            }
        } catch (IOException e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenWriting() {
        try {
            updateBuffer();
            int len = this.getConnectionMessage().getSocket().write(writeBuffer);
            if (len == 0) {
                updateBufferAndInterestOps();
                return ConnectionStatus.WAITING;
            } else if (len < 0) {
                return ConnectionStatus.CLOSE;
            } else {
                updateBufferAndInterestOps();
                return ConnectionStatus.WAITING;
            }
        } catch (Exception e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenError() {
        System.out.println("error");
        this.getLastException().printStackTrace();
        return super.whenError();
    }

    @Override
    public ConnectionStatus whenClosing() {
        System.out.println("close " + all);
        return super.whenClosing();
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
