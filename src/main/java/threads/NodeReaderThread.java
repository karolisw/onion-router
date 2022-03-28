package threads;

import Interface.ISupportMethods;
import cells.Cell;
import cells.ControlCell;
import security.Cryptography;
import security.KeyGeneration;
import security.KeyInformation;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.BlockingQueue;


/**
 * In order to run multiple clients (connections), threads are needed for each Node-object
 */
public class NodeReaderThread extends Thread {
    //DataOutputStream outgoingMessage;
    DataInputStream receivedMessage;
    private final SocketAddress previousNode; // previous node's IP-address
    private final SocketAddress nextNode; // next node's IP-address
    protected Socket socket; // Depending on which thread this is, the socket is either next or previous node's socket
    int nodeId;
    SocketAddress ipAddress;
    protected BlockingQueue<byte[]> queueNextNode;
    protected BlockingQueue<byte[]> queuePrevNode;
    boolean socketIsNext;

    /**
     * Each time a NodeThread is instantiated, a new socket is created (socket)
     * Contains two thread queues, because one is for the writer/reader pair on one socket,
     * and the other is for the reader/writer pair on the other socket.
     * This is helpful ie. when a thread on pair 1 needs to give a cell to a thread from pair 2.
     *
     * @param socket is the socket the thread reads from. This can be any socket
     */
    public NodeReaderThread(Socket socket, SocketAddress previousNode, SocketAddress nextNode,
                            BlockingQueue<byte[]> queueNextNode, BlockingQueue<byte[]> queuePrevNode,
                            int nodeId, SocketAddress ipAddress, boolean socketIsNext) {
        this.socket = socket;
        this.previousNode = previousNode;
        this.nextNode = nextNode;
        this.queueNextNode = queueNextNode;
        this.queuePrevNode = queuePrevNode;
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.socketIsNext = socketIsNext;
    }

    /**
     * Method runs upon NodeThread.start()
     * Interprets the cell received by decoding it, and checking if it is readable
     * The cell is readable if it has a length of 512 bytes after decoding
     * If readable, the method will handle the cell depending on the command it holds
     * If not readable, the method will check the IP-address of the node who sent the
     * message, and forward it to the other node it holds a connection with
     */
    public void run() {
        // Entire cell-handling implementation follows here, inside thread's run() method
        boolean end = false;

        try {
            // outgoingMessage = new DataOutputStream(socket.getOutputStream());
            if(socket.isConnected()) {
                System.out.println("Socket connected");
                receivedMessage = new DataInputStream(socket.getInputStream());
                while (end == false) { // <-- true until destroy command is received

                    try {
                        // Gets the remote IP-address
                        InetSocketAddress cellSender = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
                        // Determine how big the array needed to hold the stream must be
                        int cellSize = receivedMessage.available();
                        // If cellSize is 0, the stream is actually empty!
                        if (cellSize != 0) {
                            System.out.println("Cellsize is now: " + cellSize);

                            // Create the byte array
                            byte[] cell = new byte[512];

                            // Read the bytes into the array
                            int bytesLeft = receivedMessage.read(cell, 0, 512);

                            // BytesLeft should be -1 (returned when stream is empty)
                            if (bytesLeft != -1) {
                                System.out.println("The bytes left were " + bytesLeft);
                            }

                            // If the sender was the next node, the cell should be encrypted and passed on to the previous
                            if (nextNode != null) {
                                System.out.println("socket address: ");
                                System.out.println("next node address:" + nextNode);
                                // If the cell was received from the next node, the cell should be sent along to the server
                                if (socketIsNext) {
                                    Cryptography cryptography = new Cryptography();

                                    // In order to encrypt, the cell must be transformed from byte array to Cell object!
                                    cryptography.encrypt(Cell.bytesToCellObject(cell), KeyInformation.getSecretKeyUsingNodeId(nodeId));

                                    queuePrevNode.put(cell);
                                    return;
                                }
                            }
                            // If the node receives a create-cell, the cell will not be encrypted!
                            if (ISupportMethods.getCommand(cell) == 1) {
                                // If the node has a symmetric key, then this packet is meant for the next node!
                                if (KeyInformation.getSecretKeyUsingNodeId(nodeId) == null) {
                                    // Read the necessary parameters
                                    byte[] circuitID = ISupportMethods.getCircuitId(cell);
                                    byte[] payload = ISupportMethods.getPayload(cell);

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
                                    KeyInformation.addSecretKeyToMap(nodeId, symmetricKey);

                                    // Create a control cell and send it back where it came from
                                    ControlCell created = new ControlCell((byte) 2, circuitID);
                                    byte[] newPayload = new byte[509];
                                    for (int i = 0; i < 4; i++) {
                                        newPayload[i] = u[i];
                                    }
                                    created.setPayload(newPayload);
                                    // Have to send it back with the public key or u
                                    queuePrevNode.put(created.getTotalMessage());

                                    System.out.println("Sending created cell back to server");
                                }
                            }
                            // If the cell was not a create cell
                            else {
                                Cryptography cryptography = new Cryptography();
                                cryptography.decrypt(cell, KeyInformation.getSecretKeyUsingNodeId(nodeId));
                                byte command = ISupportMethods.getCommand(cell);
                                // If the command bit is readable now, the cell is fully decrypted
                                if (command >= 0 && command < 4) {
                                    // Read the command to figure out if relay cell or control cell
                                    if (command == 0) {
                                        // Relay cell
                                        byte relayCommand = ISupportMethods.getRelayCommand(cell);

                                        // relayBegin
                                        if (relayCommand == 1) {
                                            // throw error, because this should only be read at the end node!
                                            throw new Exception("Relay cell with relayCommand: 1, received at regular node. " +
                                                    "This command should only be interpreted at end node!");
                                        } else if (relayCommand == 2) {
                                            // This command is not yet implemented, and involves adding a new node to the relay
                                            throw new Exception("Relay cell with relayCommand: 2, not yet supported.");
                                        } else if (relayCommand == 3) {
                                            // should not happen here!
                                            throw new Exception("Relay cell with relayCommand: 3, received at regular node. " +
                                                    "This command should only be interpreted at Client proxy " +
                                                    "(and is not yet implemented in program)!\");");
                                        } else if (relayCommand == 4) {
                                            // should not happen here!
                                            throw new Exception("Relay cell with relayCommand: 4, received at regular node. " +
                                                    "This command should only be interpreted at end node ");
                                        } else if (relayCommand == 5) {
                                            // should not happen here!
                                            throw new Exception("Relay cell with relayCommand: 5, received at regular node. " +
                                                    "This command should only be interpreted at Client proxy ");
                                        }
                                    }
                                    // If control cell not of type "create"
                                    else if (command == 2) {
                                        // Send to server
                                        queuePrevNode.put(cell);

                                    } else if (command == 3) {
                                        // The socket must close all the sockets that belong to the circuitID of the package
                                        end = true;
                                    }
                                }
                                // If neither, send the cell to the next node
                                else {
                                    System.out.println("Cell was passed on to the next socket");
                                    queueNextNode.put(cell);
                                }
                            }
                        }
                    }catch (NoSuchPaddingException ex) {
                        ex.printStackTrace();
                    } catch (IllegalBlockSizeException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (NoSuchAlgorithmException ex) {
                        ex.printStackTrace();
                    } catch (BadPaddingException ex) {
                        ex.printStackTrace();
                    } catch (InvalidKeySpecException ex) {
                        ex.printStackTrace();
                    } catch (InvalidKeyException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                // Upon end = true, we break out of the while-loop, and it is time to close the socket
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    currentThread().join(); //todo correct?
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
