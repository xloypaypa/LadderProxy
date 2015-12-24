package main;

import data.Data;

import javax.swing.*;

/**
 * Created by xlo on 2015/12/14.
 * it's the client page
 */
public class MainPage {
    private JTextField ip;
    private JTextField port;
    private JButton connectButton;
    private JPanel panel;
    private JTextField localPort;

    public MainPage() {
        ip.setText(Data.getServerIp());
        port.setText(String.valueOf(Data.getServerPort()));
        localPort.setText("8080");

        connectButton.addActionListener(e -> {
            ip.setEnabled(false);
            port.setEnabled(false);
            localPort.setEnabled(false);
            connectButton.setEnabled(false);

            ClientMain.startServer(ip.getText(), port.getText(), localPort.getText());
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
