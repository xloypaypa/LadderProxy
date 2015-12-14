package net;

import net.server.AbstractServer;
import net.tool.connectionManager.ConnectionManager;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by xlo on 15-12-13.
 * it's the abstract solver
 */
public abstract class AbstractSolver extends AbstractServer {

    protected final BlockingQueue<byte[]> writeList;
    protected volatile ByteBuffer readBuffer, writerBuffer;
    protected volatile boolean isEnd = false;

    public AbstractSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
        this.writeList = new LinkedBlockingQueue<>();
        this.readBuffer = ByteBuffer.allocate(1024);
        this.readBuffer.clear();
    }

    @Override
    public ConnectionStatus whenReading() {
        if (this.getOther().isEnd) {
            return ConnectionStatus.CLOSE;
        }
        try {
            int read = this.getConnectionMessage().getSocket().read(readBuffer);
            if (read < 0) {
                return ConnectionStatus.CLOSE;
            }

            readBuffer.flip();
            byte[] now = new byte[readBuffer.limit()];
            for (int i = 0; i < readBuffer.limit(); i++) {
                now[i] = readBuffer.get();
            }
            readBuffer.compact();
            this.getOther().addBytes(now);

            if (read != 0) {
                return ConnectionStatus.READING;
            } else {
                return afterIO();
            }
        } catch (IOException e) {
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenWriting() {
        if (this.getOther().isEnd) {
            return ConnectionStatus.CLOSE;
        }
        try {
            if (this.getConnectionMessage().getSocket().write(writerBuffer) < 0) {
                return ConnectionStatus.CLOSE;
            }
            return afterIO();
        } catch (IOException e) {
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenError() {
        return ConnectionStatus.CLOSE;
    }

    public void addBytes(byte[] bytes) {
        if (bytes.length == 0) {
            return ;
        }
        writeList.add(bytes);
        if (this.getConnectionMessage().getSelectionKey() != null && this.getConnectionMessage().getSelectionKey().interestOps() != SelectionKey.OP_CONNECT) {
            toWriting();
            this.getConnectionMessage().getSelectionKey().selector().wakeup();
        }
    }

    protected void toReading() {
        this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_READ);
    }

    protected void toWriting() {
        this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public ConnectionStatus whenClosing() {
        this.isEnd = true;
        ConnectionManager.getSolverManager().removeConnection(this.getConnectionMessage().getSocket().socket());
        this.getConnectionMessage().closeSocket();
        return ConnectionStatus.WAITING;
    }

    @Override
    public ConnectionStatus whenWaiting() {
        if (this.getConnectionMessage().getSocket().socket().isClosed()) {
            ConnectionManager.getSolverManager().removeConnection(this.getConnectionMessage().getSocket().socket());
            return ConnectionStatus.WAITING;
        }
        return getIOStatus();
    }

    protected ConnectionStatus getIOStatus() {
        updateWriteBuffer();
        if (writerBuffer == null && writeList.size() == 0) {
            return ConnectionStatus.READING;
        }

        if (writerBuffer == null) {
            writerBuffer = ByteBuffer.wrap(writeList.poll());
        }

        return ConnectionStatus.WRITING;
    }

    protected ConnectionStatus afterIO() {
        updateWriteBuffer();

        if (writerBuffer == null && writeList.size() == 0) {
            toReading();
            return ConnectionStatus.WAITING;
        } else {
            toWriting();
            return ConnectionStatus.WAITING;
        }
    }

    protected void updateWriteBuffer() {
        if (writerBuffer != null && writerBuffer.position() == writerBuffer.limit()) {
            writerBuffer = null;
        }
    }

    protected abstract AbstractSolver getOther();
}
