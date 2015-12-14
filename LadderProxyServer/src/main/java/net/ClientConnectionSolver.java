package net;

import data.Data;
import net.tool.connectionSolver.ConnectionMessage;
import net.tool.connectionSolver.ConnectionStatus;
import net.tool.packageSolver.PackageStatus;
import net.tool.packageSolver.headSolver.HttpRequestHeadSolver;
import net.tool.packageSolver.packageReader.HttpPackageReader;
import net.tool.packageSolver.packageReader.PackageReader;

import java.io.IOException;

/**
 * Created by xlo on 15-12-13.
 * it's the browser reader solver
 */
public class ClientConnectionSolver extends AbstractSolver {

    private boolean isFirst = true, isCheck = true;
    private PackageReader packageReader;

    private HostServerConnectionSolver hostServerConnectionSolver;

    public ClientConnectionSolver(ConnectionMessage connectionMessage) {
        super(connectionMessage);
    }

    @Override
    protected AbstractSolver getOther() {
        return this.hostServerConnectionSolver;
    }

    @Override
    public ConnectionStatus whenInit() {
        packageReader = new HttpPackageReader(this.getConnectionMessage().getSocket());
        return ConnectionStatus.READING;
    }

    @Override
    public ConnectionStatus whenReading() {
        if (isCheck) {
            try {
                PackageStatus packageStatus = this.packageReader.read();
                if (packageStatus.equals(PackageStatus.END)) {
                    isCheck = false;
                    HttpRequestHeadSolver httpRequestHeadSolver = (HttpRequestHeadSolver) this.packageReader.getHeadPart();
                    if (Data.getPassword().equals(httpRequestHeadSolver.getMessage("Password"))) {
                        return ConnectionStatus.READING;
                    } else {
                        return ConnectionStatus.CLOSE;
                    }
                } else if (packageStatus.equals(PackageStatus.WAITING)) {
                    return ConnectionStatus.WAITING;
                } else if (packageStatus.equals(PackageStatus.CLOSED) || packageStatus.equals(PackageStatus.ERROR)) {
                    return ConnectionStatus.ERROR;
                } else {
                    return ConnectionStatus.WAITING;
                }
            } catch (IOException e) {
                return ConnectionStatus.ERROR;
            }
        } else if (isFirst) {
            try {
                PackageStatus packageStatus = this.packageReader.read();
                if (packageStatus.equals(PackageStatus.END)) {
                    isFirst = false;
                    HttpRequestHeadSolver httpRequestHeadSolver = (HttpRequestHeadSolver) this.packageReader.getHeadPart();
                    this.hostServerConnectionSolver = new HostServerConnectionSolver(this, httpRequestHeadSolver.getHost(), httpRequestHeadSolver.getPort());
                    this.hostServerConnectionSolver.addBytes(this.packageReader.getPackage());
                    this.hostServerConnectionSolver.addBytes(this.packageReader.stop());
                    return ConnectionStatus.WAITING;
                } else if (packageStatus.equals(PackageStatus.WAITING)) {
                    return ConnectionStatus.WAITING;
                } else if (packageStatus.equals(PackageStatus.CLOSED) || packageStatus.equals(PackageStatus.ERROR)) {
                    return ConnectionStatus.ERROR;
                } else {
                    return ConnectionStatus.WAITING;
                }
            } catch (IOException e) {
                return ConnectionStatus.ERROR;
            }
        } else {
            return super.whenReading();
        }
    }

    @Override
    public ConnectionStatus whenWaiting() {
        return super.whenWaiting();
    }

    @Override
    public ConnectionStatus whenConnecting() {
        return null;
    }

}
