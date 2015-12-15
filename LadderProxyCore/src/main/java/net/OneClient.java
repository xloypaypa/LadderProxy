package net;

import net.server.Client;

/**
 * Created by xlo on 2015/12/15.
 * it's the one client
 */
public class OneClient {

    private volatile static Client client;

    public static Client getClient() {
        if (client == null) {
            synchronized (OneClient.class) {
                if (client == null) {
                    client = Client.getNewClient("client");
                }
            }
        }
        return client;
    }
}
