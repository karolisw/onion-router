package nodes;
import Interface.ICellMethods;
import Interface.ISupportMethods;
import cells.ControlCell;
import circuit.Circuit;
import security.KeyGeneration;
import security.KeyInformation;
import threads.NodeReaderThread;
import threads.WriterThread;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.security.PublicKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static Interface.ISupportMethods.setRandomId;


/**
 * The nodes communicate with the ClientProxy through the use of TLS
 */
public class Node {

    private int id = 0;
    private int portNumber;
    private SocketAddress ipAddress;
    private SocketAddress previousNode; // previous node's IP-address
    private SocketAddress nextNode; // next node's IP-address
    private Boolean online = false; // Offline/online (in use or not)
    BlockingQueue<byte[]> queuePrevNode;
    BlockingQueue<byte[]> queueNextNode;



    // For the server-part
    DataOutputStream outgoingMessage1;
    DataInputStream receivedMessage1;
    DataOutputStream outgoingMessage2;
    DataInputStream receivedMessage2;
    ServerSocket serverSocket;
    Socket previousNodeSocket;
    Socket nextNodeSocket;
    //private static final HashMap<InetAddress, Socket> connectionMap = new HashMap<>();

    public Node() {
    }

    /**
     * The Node-server (serverSocket) accepts the connection from the client, binds a new socket to the same local port,
     * and sets its remote endpoint to the client's address and port.
     * It needs a new socket (clientSocket) so that it can continue to listen to the original socket for connection
     * requests when the attention needs for the connected client.
     */
    public Node(InetSocketAddress socketAddress, int portNumber) {
        try {
            serverSocket = new ServerSocket(portNumber);
            this.portNumber = portNumber;
            setRandomId();
            ipAddress = socketAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createConnection() {
        try {
            if(serverSocket == null) {
                serverSocket = new ServerSocket(portNumber);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Connecting to the previous node first
                    try {
                        System.out.println("waiting for a connection on serversocket" + serverSocket.getLocalSocketAddress());
                        boolean connected = false;
                        while(!connected) {
                            // This is the socket created through connecting with the previous node
                            previousNodeSocket = serverSocket.accept();

                            System.out.println("node with node id " + " connected to new socket with ip address: \n" +
                                    previousNodeSocket.getInetAddress() + ":" + previousNodeSocket.getPort());

                            // Afterwards, initiating the second socket
                            nextNodeSocket = new Socket();

                            System.out.println("next node looks like this: " + nextNode);

                            // Connecting the second socket to the next node
                            if(nextNode != null) {
                                nextNodeSocket.connect(nextNode);
                                if(previousNodeSocket != null || nextNodeSocket != null) {
                                    connected = true;
                                }
                            }
                            // In the endnode the next socket is null as of right now
                            else {
                                if (previousNodeSocket != null) {
                                    connected = true;
                                }
                            }
                        }
                        System.out.println("Trying to set the input and output streams");
                        TimeUnit.SECONDS.sleep(2);

                        if(nextNodeSocket != null && nextNode != null) {
                            // A new DataOutput -and DataInputStream are instantiated for each socket
                            receivedMessage1 = new DataInputStream(previousNodeSocket.getInputStream());
                            outgoingMessage1 = new DataOutputStream(previousNodeSocket.getOutputStream());
                            receivedMessage2 = new DataInputStream(nextNodeSocket.getInputStream());
                            outgoingMessage2 = new DataOutputStream(nextNodeSocket.getOutputStream());
                        }
                        else {
                            receivedMessage1 = new DataInputStream(previousNodeSocket.getInputStream());
                            outgoingMessage1 = new DataOutputStream(previousNodeSocket.getOutputStream());
                        }

                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Create new threads to run the connection to the previous node
                    if(nextNode == null) {
                        new NodeReaderThread(previousNodeSocket, previousNode, null,
                                null,queuePrevNode,id, ipAddress, false).start();
                        new WriterThread(previousNodeSocket,queuePrevNode).start();
                        System.out.println("Created reader and writer thread");
                    }
                    else {
                        new NodeReaderThread(previousNodeSocket, previousNode, nextNode,
                                queueNextNode,queuePrevNode,id, ipAddress, false).start();
                        new WriterThread(previousNodeSocket,queuePrevNode).start();

                        // Create new threads to run the connection to the next node
                        new NodeReaderThread(nextNodeSocket, previousNode,nextNode,
                                queueNextNode,queuePrevNode,id,ipAddress, true).start();
                        new WriterThread(nextNodeSocket,queueNextNode).start();
                        System.out.println("Created reader and writer thread");
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fullCircuitHandshake(Circuit circuit) throws Exception {
        // Node must pass its own create thread as well
        ControlCell created = ICellMethods.create(circuit.getId(),nextNodeSocket);

        byte[] payload = ISupportMethods.getPayload(created.getTotalMessage());

        // The cell is a create cell, meaning this cell must respond with a created cell
        byte[] u = new byte[4];

        // Reading in the BigInteger u as bytes
        for (int i = 0; i < 4; i++) {
            u[i] = payload[i];
        }

        // Convert the bytes to a public key
        PublicKey pk = ISupportMethods.convertToPublicKey(u);

        // Create the symmetric key
        KeyGeneration keyGeneration = new KeyGeneration();
        SecretKey symmetricKey = keyGeneration.generateSecretKey(pk, true);

        // Add the symmetric key and the IP to the store
        KeyInformation.addSecretKeyToMap(id, symmetricKey);

        // Create a control cell and send it back where it came from
        ControlCell createdCell = new ControlCell((byte) 2, circuit.getId());
        byte[] newPayload = new byte[509];
        for (int i = 0; i < 4; i++) {
            newPayload[i] = u[i];
        }
        created.setPayload(newPayload);
        // Have to send it back with the public key or u
        queuePrevNode.put(created.getTotalMessage());
        System.out.println("Sending created cell back to server");
        // Run a worker thread that to pass the create cells
        // todo this writer thread is not part of the same blockingqueue as the server
        WriterThread writerThread = new WriterThread(nextNodeSocket, queueNextNode, 1,circuit);
        // Start the thread
        writerThread.start();
    }


    // GETTERS
    public Boolean getOnline() {
        return online;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public int getId() {
        return id;
    }

    public SocketAddress getIpAddress() {
        return ipAddress;
    }

    public SocketAddress getNextNode() {
        return nextNode;
    }

    public SocketAddress getPreviousNode() {
        return previousNode;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    // SETTERS
    public void setNextNode(SocketAddress nextNode) {
        this.nextNode = nextNode;
    }

    public void setPreviousNode(SocketAddress previousNode) {
        this.previousNode = previousNode;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * This method sets the queue that this node shares with the next node
     * @param queueNextNode is the queue to be shared
     */
    public void setQueueNextNode(BlockingQueue<byte[]> queueNextNode) {
        this.queueNextNode = queueNextNode;
    }

    /**
     * This methods sets the queue that this node shares with the previous node
     * @param queuePrevNode is the queue to be shared
     */
    public void setQueuePrevNode(BlockingQueue<byte[]> queuePrevNode) {
        this.queuePrevNode = queuePrevNode;
    }

    public BlockingQueue<byte[]> getQueueNextNode() {
        return queueNextNode;
    }

    public BlockingQueue<byte[]> getQueuePrevNode() {
        return queuePrevNode;
    }
}
