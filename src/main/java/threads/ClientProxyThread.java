package threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public class ClientProxyThread extends Thread {
    InetSocketAddress destination;
    Proxy proxy;
    Socket socket;
    DataOutputStream outboundMessage; // to guard node
    DataInputStream inboundMessage; // from guard node


    /**
     * This thread instance is called upon in the ClientProxy constructor
     * whenever a new connection is made --> meaning that since this
     * proxy is in contact with only the Guard node through sockets,
     * there will only be one instantiation per circuit
     *
     * @param proxy is the proxy connection for the server
     * @param destinationIp is the ip address of the first node in the circuit (guard node)
     * @param destinationPort is the port the guard node uses
     */
    public ClientProxyThread (Proxy proxy, InetAddress destinationIp, int destinationPort) {
        try{
            socket = new Socket(proxy);
            this.proxy = proxy;
            destination = new InetSocketAddress(destinationIp,destinationPort);

            // Connecting the proxy socket to the destination
            socket.connect(destination); // todo this should not be here, but is just part of the setup

            outboundMessage = new DataOutputStream(socket.getOutputStream());
            inboundMessage = new DataInputStream(socket.getInputStream());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
