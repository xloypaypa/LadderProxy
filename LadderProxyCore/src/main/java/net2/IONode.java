package net2;

import net.server.AbstractServer;
import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionManager.SelectorManager;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by xlo on 2015/12/15.
 * it's the io node
 */
public abstract class IONode extends AbstractServer {

    protected volatile IONode ioNode = null;
    protected volatile ByteBuffer writeBuffer;
    protected volatile ByteBuffer byteBuffer;
    protected volatile BlockingQueue<byte[]> messageQueue;

    public IONode(ConnectionMessage connectionMessage) {
        super(connectionMessage);
        this.messageQueue = new LinkedBlockingDeque<>();
    }

    @Override
    public ConnectionStatus whenInit() {
        this.byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.clear();
        return ConnectionStatus.CONNECTING;
    }

    @Override
    public ConnectionStatus whenReading() {
        try {
            int len = this.getConnectionMessage().getSocket().read(byteBuffer);
            if (len < 0) {
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

    public void addMessage(byte[] message) {
        if (message.length == 0) {
            return;
        }
        this.messageQueue.add(message);
        if (this.getConnectionMessage().getSelectionKey() != null && this.getConnectionMessage().getSelectionKey().isValid()
                && this.getConnectionMessage().getSelectionKey().interestOps() == SelectionKey.OP_READ) {
            changeInterestOps(SelectionKey.OP_WRITE);
        }
    }

    @Override
    public ConnectionStatus whenWaiting() {
        int ops = this.getConnectionMessage().getSelectionKey().interestOps();
        if (ops == SelectionKey.OP_READ) {
            return ConnectionStatus.READING;
        } else if (ops == SelectionKey.OP_WRITE) {
            return ConnectionStatus.WRITING;
        } else {
            return ConnectionStatus.CONNECTING;
        }
    }

    protected void updateBufferAndInterestOps() {
        updateBuffer();
        updateInterestOps();
    }

    protected byte[] decryptMessage(byte[] message) {
        return message;
    }

    private void updateInterestOps() {
        if (writeBuffer == null && !this.ioNode.isClosed()) {
            changeInterestOps(SelectionKey.OP_READ);
        } else {
            changeInterestOps(SelectionKey.OP_WRITE);
        }
    }

    private void changeInterestOps(int ops) {
        if (!this.getConnectionMessage().getSelectionKey().isValid()) {
            return ;
        }
        if (this.getConnectionMessage().getSelectionKey().interestOps() != ops) {
            this.getConnectionMessage().getSelectionKey().interestOps(ops);
            SelectorManager.getSelectorManager().getSelectThread(this.getConnectionMessage().getSelectionKey().selector()).wakeUp();
        }
    }

    protected void updateBuffer() {
        removeEmptyBuffer();
        if (needAndHaveNextMessage()) {
            buildNextBuffer();
        }
    }

    private boolean needAndHaveNextMessage() {
        return writeBuffer == null && !messageQueue.isEmpty();
    }

    private void removeEmptyBuffer() {
        if (writeBuffer != null && writeBuffer.position() == writeBuffer.limit()) {
            writeBuffer = null;
        }
    }

    protected void readOnce(int len) {
        byteBuffer.flip();
        byte[] message = new byte[len];
        for (int i = 0; i < len; i++) {
            message[i] = byteBuffer.get();
        }
        this.ioNode.addMessage(decryptMessage(message));
        byteBuffer.clear();
    }

    protected void buildNextBuffer() {
        this.writeBuffer = ByteBuffer.wrap(getNextMessage());
    }

    protected byte[] getNextMessage() {
        return this.messageQueue.poll();
    }

    /**
     * close status
     */
    private volatile boolean isClosed = false;

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    @Override
    public ConnectionStatus whenClosing() {
        try {
            this.isClosed = true;
            ConnectionManager.getSolverManager().removeConnection(this.getConnectionMessage().getSocket().socket());
            this.getConnectionMessage().closeSocket();
            if (this.ioNode != null) {
                this.ioNode.updateInterestOps();
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return ConnectionStatus.WAITING;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public ConnectionStatus whenError() {
        return ConnectionStatus.CLOSE;
    }
}
