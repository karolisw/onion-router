package nodes;
import Interface.ConvertInterface;
import cells.Cell;
import cells.ControlCell;
import security.Cryptography;
import security.KeyGeneration;
import security.KeyInformation;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.security.PublicKey;
import java.util.*;

import static Interface.ConvertInterface.setRandomId;


/**
 * The nodes communicate with the ClientProxy through the use of TLS
 */
public class Node{

    private int id = 0;
    private InetAddress ipAddress;
    private long identityKey; // for authentication (TLS)
    private InetAddress previousNode; // previous node's IP-address
    private InetAddress nextNode; // next node's IP-address
    private Boolean online = false; // Offline/online (in use or not)
    private KeyInformation currentKey = new KeyInformation();
    private HashSet<KeyInformation> keys; // contains all the keys and node information necessary for routing

    // For the server-part
    DataOutputStream outgoingMessage;
    DataInputStream receivedMessage;
    ServerSocket serverSocket;

    Socket currentSocket;
    private static HashMap<InetAddress, Socket> connectionMap = new HashMap<>();

    public Node() {
    }

    /**
     * The Node-server (serverSocket) accepts the connection from the client, binds a new socket to the same local port,
     * and sets its remote endpoint to the client's address and port.
     * It needs a new socket (clientSocket) so that it can continue to listen to the original socket for connection
     * requests when the attention needs for the connected client.
     */
    public Node(InetAddress nodeIp, int portNumber) {
        try {
            serverSocket = new ServerSocket(portNumber);
            setRandomId();
            ipAddress = nodeIp;
            createConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The accept ()method waits until a client requests a connection on the host and port of this server.
     * When a connection is requested and successfully established, the accept()method
     * returns a new Socket object (clientSocket)
     */
    public void createConnection() {
        try {
            // For each new incoming connection, a new socket (clientSocket) is created with a new port number
            Socket clientSocket = serverSocket.accept();

            // A new DataOutput -and DataInputStream are instantiated for each socket
            receivedMessage = new DataInputStream(clientSocket.getInputStream());
            outgoingMessage = new DataOutputStream(clientSocket.getOutputStream());

            // Add the newly created socket to the connection map
            addNewConnection(clientSocket, clientSocket.getInetAddress());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create new NodeThread to run the new socket
        new NodeThread(currentSocket, previousNode, nextNode, id);

    }

    /**
     * To have control of all connections in the running threads, this list is upheld
     * Sockets are removed when destroy cells are received
     *
     * @param socket the new socket created upon a new connection
     * @param connectedNode the node that just connected, causing the ServerSocket to create a new connection (socket)
     */
    public static void addNewConnection(Socket socket, InetAddress connectedNode) throws Exception {
        if(connectionMap.containsKey(connectedNode)) {
            throw new Exception("You need to implement handling for this " +
                    "(delete the old socket and implement this instead)");
        }
        else {
            connectionMap.put(connectedNode, socket);
        }
    }

    /**
     * Access all connections that are connected to a certain IpAddress
     *
     * @param address is the ipAddress
     * @return the corresponding socket
     */
    public static Socket getSocket(InetAddress address) {
        return connectionMap.get(address);
    }


    // GETTERS
    public Boolean getOnline() {
        return online;
    }

    public int getId() {
        return id;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public InetAddress getNextNode() {
        return nextNode;
    }

    public InetAddress getPreviousNode() {
        return previousNode;
    }

    // SETTERS
    public void setNextNode(InetAddress nextNode) {
        this.nextNode = nextNode;
    }

    public void setPreviousNode(InetAddress previousNode) {
        this.previousNode = previousNode;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public void setId(int id) {
        this.id = id;
    }

}
