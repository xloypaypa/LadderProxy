package net2;

import net.server.AbstractServer;
import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionManager.SelectorManager;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by xlo on 2015/12/15.
 * it's the io node
 */
public abstract class IONode extends AbstractServer {
    protected volatile BlockingQueue<byte[]> messageQueue;
    protected volatile ByteBuffer writeBuffer;
    protected volatile IONode ioNode;
    protected volatile ByteBuffer byteBuffer;
    private volatile boolean isClosed = false;

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
                afterIO();
                return ConnectionStatus.WAITING;
            } else {
                return readOnce(len);
            }
        } catch (IOException e) {
            this.sendException(e);
            return ConnectionStatus.ERROR;
        }
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
        if (this.ioNode != null) {
            this.ioNode.setClosed(true);
            this.ioNode.addMessage("close".getBytes());
        }
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
        changeInterestOps();
        return ConnectionStatus.WAITING;
    }

    protected byte[] decryptMessage(byte[] message) {
        return message;
    }

    private void changeInterestOps() {
        if (writeBuffer == null) {
            this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_READ);
        } else {
            this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void updateBuffer() {
        if (writeBuffer != null && writeBuffer.position() == writeBuffer.limit()) {
            writeBuffer = null;
        }

        if (writeBuffer == null && !messageQueue.isEmpty()) {
            buildOutput();
        }
    }

    private ConnectionStatus readOnce(int len) {
        byteBuffer.flip();
        byte[] message = new byte[len];
        for (int i = 0; i < len; i++) {
            message[i] = byteBuffer.get();
        }
        this.ioNode.addMessage(decryptMessage(message));
        byteBuffer.clear();
        return ConnectionStatus.READING;
    }

    protected void buildOutput() {
        byte[] buffer = buildBuffer();
        writeBuffer = ByteBuffer.wrap(buffer);
    }

    protected byte[] buildBuffer() {
        List<byte[]> temp = new LinkedList<>();
        while (!messageQueue.isEmpty()) {
            temp.add(messageQueue.poll());
        }
        int len = 0;
        for (byte[] now : temp) {
            len += now.length;
        }
        byte[] buffer = new byte[len];
        int pos = 0;
        for (byte[] bytes : temp ) {
            for (byte now : bytes) {
                buffer[pos++] = now;
            }
        }
        return buffer;
    }
}
