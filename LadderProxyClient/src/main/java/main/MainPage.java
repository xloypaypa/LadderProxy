package main;

import data.Data;
import net.BrowserConnectionSolver;
import net.server.Server;
import net.tool.connectionSolver.ConnectionMessageImpl;

import javax.swing.*;

/**
 * Created by xlo on 2015/12/14.
 * it's the client page
 */
public class MainPage {
    private JTextField ip;
    private JTextField port;
    private JTextField password;
    private JButton connectButton;
    private JPanel panel;
    private JTextField localPort;

    public MainPage() {
        ip.setText(Data.getServerIp());
        password.setText(Data.getPassword());
        port.setText(String.valueOf(Data.getServerPort()));

        connectButton.addActionListener(e -> {
            ip.setEnabled(false);
            port.setEnabled(false);
            password.setEnabled(false);
            localPort.setEnabled(false);
            connectButton.setEnabled(false);

            ClientMain.startServer(ip.getText(), port.getText(), localPort.getText(), password.getText());
        });
    }

    public void create() {
        JFrame frame = new JFrame("ladder proxy");
        frame.setContentPane(this.panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
