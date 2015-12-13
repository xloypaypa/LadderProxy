package net;

import net.server.AbstractServer;
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
    protected ByteBuffer readBuffer, writerBuffer;

    public AbstractSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
        this.writeList = new LinkedBlockingQueue<>();
        this.readBuffer = ByteBuffer.allocate(1024);
        this.readBuffer.clear();
    }

    @Override
    public ConnectionStatus whenReading() {
        try {
            if (this.getConnectionMessage().getSocket().read(readBuffer) < 0) {
                return ConnectionStatus.CLOSE;
            }

            readBuffer.flip();
            byte[] now = new byte[readBuffer.limit()];
            for (int i = 0; i < readBuffer.limit(); i++) {
                now[i] = readBuffer.get();
            }
            readBuffer.compact();
            this.getOther().addBytes(now);

            return ConnectionStatus.WAITING;
        } catch (IOException e) {
            System.out.println(this.getClass());
            e.printStackTrace();
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenWriting() {
        try {
            this.getConnectionMessage().getSocket().write(writerBuffer);
            return afterIO();
        } catch (IOException e) {
            System.out.println(this.getClass());
            e.printStackTrace();
            return ConnectionStatus.ERROR;
        }
    }

    @Override
    public ConnectionStatus whenClosing() {
        this.getConnectionMessage().closeSocket();
        return null;
    }

    @Override
    public ConnectionStatus whenError() {
        return ConnectionStatus.CLOSE;
    }

    @Override
    public ConnectionStatus whenWaiting() {
        return getIOStatus();
    }

    public void addBytes(byte[] bytes) {
        writeList.add(bytes);
        if (this.getConnectionMessage().getSelectionKey() != null) {
            toWriting();
        }
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

    protected void toReading() {
        this.connectionStatus = ConnectionStatus.READING;
        this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_READ);
    }

    protected void toWriting() {
        this.connectionStatus = ConnectionStatus.WRITING;
        this.getConnectionMessage().getSelectionKey().interestOps(SelectionKey.OP_WRITE);
    }

    protected abstract AbstractSolver getOther();
}
