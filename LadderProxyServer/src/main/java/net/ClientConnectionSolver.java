package net;

import data.Data;
import net.server.proxy.ProxyReadServer;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.headSolver.HttpRequestHeadSolver;
import net.tool.packageSolver.packageReader.PackageReader;

import java.io.IOException;

/**
 * Created by xlo on 15-12-13.
 * it's the client connection solver
 */
public class ClientConnectionSolver extends ProxyReadServer {
    private boolean checked;

    public ClientConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
        this.checked = false;
    }

    @Override
    public ConnectionStatus whenReading() {
        if (!checked) {
            return readPasswordPackage();
        }
        return super.whenReading();
    }

    @Override
    public ConnectionStatus whenError() {
        return super.whenError();
    }

    protected ConnectionStatus readPasswordPackage() {
        try {
            PackageStatus packageStatus = PackageReader.readPackage(this.packageReader);
            if (packageStatus.equals(PackageStatus.WAITING)) {
                return ConnectionStatus.WAITING;
            } else if (!packageStatus.equals(PackageStatus.END)) {
                return ConnectionStatus.ERROR;
            } else {
                checked = true;
                HttpRequestHeadSolver requestHeadSolver = (HttpRequestHeadSolver) packageReader.getHeadPart();
                if (requestHeadSolver.getMessage("Password").equals(Data.getPassword())) {
                    return ConnectionStatus.WAITING;
                } else {
                    return ConnectionStatus.ERROR;
                }
            }
        } catch (IOException e) {
            return ConnectionStatus.ERROR;
        }
    }
}
