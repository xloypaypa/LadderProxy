package net;

import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;

import java.nio.ByteBuffer;

/**
 * Created by xlo on 15-12-13.
 * it's the browser reader solver
 */
public class BrowserConnectionSolver extends AbstractSolver {

    private volatile ProxyServerConnectionSolver proxyServerConnectionSolver;

    public BrowserConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    protected AbstractSolver getOther() {
        return this.proxyServerConnectionSolver;
    }

    @Override
    public ConnectionStatus whenInit() {
        this.proxyServerConnectionSolver = new ProxyServerConnectionSolver(this);
        return ConnectionStatus.READING;
    }

    @Override
    public ConnectionStatus whenConnecting() {
        return null;
    }

}
