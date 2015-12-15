package net;

import net.server.AbstractServer;
import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionManager.SelectorManager;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by xlo on 2015/12/15.
 * it's the io node
 */
public abstract class IONode extends AbstractServer {
    protected volatile BlockingQueue<byte[]> messageQueue;
    protected volatile ByteBuffer writeBuffer;
    private boolean isClosed = false;

    public IONode(ConnectionMessage connectionMessage) {
        super(connectionMessage);
        this.messageQueue = new LinkedBlockingDeque<>();
    }

    @Override
    public ConnectionStatus whenWriting() {
        try {
            int len = this.getConnectionMessage().getSocket().write(writeBuffer);
            if (len == 0) {
                afterIO();
                return ConnectionStatus.WAITING;
            } else if (len < 0) {
                return ConnectionStatus.CLOSE;
            } else {
                return afterIO();
            }
        } catch (Exception e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenClosing() {
        this.isClosed = true;
        ConnectionManager.getSolverManager().removeConnection(this.getConnectionMessage().getSocket().socket());
        this.getConnectionMessage().closeSocket();
        return ConnectionStatus.WAITING;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public ConnectionStatus whenError() {
        this.getLastException().printStackTrace();
        return ConnectionStatus.CLOSE;
    }

    @Override
    public ConnectionStatus whenWaiting() {
        int ops = this.getConnectionMessage().getSelectionKey().interestOps();
        if (ops == SelectionKey.OP_READ) {
            if (this.isClosed) {
                return ConnectionStatus.CLOSE;
            }
            return ConnectionStatus.READING;
        } else if (ops == SelectionKey.OP_WRITE) {
            if (this.isClosed) {
                return ConnectionStatus.CLOSE;
            }
            return ConnectionStatus.WRITING;
        } else {
            return ConnectionStatus.CONNECTING;
        }
    }

    public void addMessage(byte[] message) {
        if (message.length == 0) {
            return;
        }

        this.messageQueue.add(message);
        if (this.getConnectionMessage().getSelectionKey() != null && this.getConnectionMessage().getSelectionKey().isValid()
                && this.getConnectionMessage().getSelectionKey().interestOps() == SelectionKey.OP_READ) {
            afterIO();
            SelectorManager.getSelectorManager().getSelectThread(this.getConnectionMessage().getSelectionKey().selector()).wakeUp();
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    protected ConnectionStatus afterIO() {
        updateBuffer();
        if (writeBuffer == null) {
            this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_READ);
            return ConnectionStatus.WAITING;
        } else {
            this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_WRITE);
            return ConnectionStatus.WAITING;
        }
    }

    protected void updateBuffer() {
        if (writeBuffer != null && writeBuffer.position() == writeBuffer.limit()) {
            writeBuffer = null;
        }

        if (writeBuffer == null && !messageQueue.isEmpty()) {
            writeBuffer = ByteBuffer.wrap(messageQueue.poll());
        }
    }
}
