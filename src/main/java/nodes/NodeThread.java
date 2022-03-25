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
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;

import static nodes.Node.getSocket;

/**
 * In order to run multiple clients (connections), threads are needed for each Node-object
 */
public class NodeThread extends Thread {
    // For the server-part
    DataOutputStream outgoingMessage;
    DataInputStream receivedMessage;
    private InetAddress previousNode; // previous node's IP-address
    private InetAddress nextNode; // next node's IP-address
    protected Socket socket;
    int nodeId;

    /**
     * Each time a NodeThread is instantiated, a new socket is created (socket)
     *
     * @param socket
     */
    public NodeThread(Socket socket, InetAddress previousNode, InetAddress nextNode, int nodeId) {
        this.socket = socket;
        this.previousNode = previousNode;
        this.nextNode = nextNode;
        this.nodeId = nodeId;
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
            outgoingMessage = new DataOutputStream(socket.getOutputStream());
            receivedMessage = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (end) { // <-- true until destroy command is received

            try {
                // Get the sender
                InetAddress cellSender = socket.getInetAddress(); // <-- returns the remote IP-address


                // Determine how big the array needed to hold the stream must be
                int cellSize = receivedMessage.available();

                // Create the byte array
                byte[] cell = new byte[cellSize];

                // Read the bytes into the array
                int bytesLeft = receivedMessage.read(cell, 0, cellSize);

                // BytesLeft should be -1 (returned when stream is empty)
                if (bytesLeft != -1) {
                    throw new Exception("Full stream might not have been read.");
                }

                // If the sender was the next node, the cell should be encrypted and passed on to the previous
                if (cellSender.equals(nextNode)) {
                    Cryptography cryptography = new Cryptography();

                    // In order to encrypt, the cell must be transformed from byte array to Cell object!
                    cryptography.encrypt(Cell.makeByteCellToObjectCell(cell), KeyInformation.getSecretKeyUsingNodeId(nodeId));

                    // Passing the node to the previous node
                    Socket previous = getSocket(cellSender);

                    // Create an output stream on that socket
                    DataOutputStream outgoingMessage = new DataOutputStream(previous.getOutputStream());

                    // Send the message
                    outgoingMessage.write(cell);
                    outgoingMessage.flush();

                    return;
                }

                // Decrypting the cell
                Cryptography cryptography = new Cryptography();
                cryptography.decrypt(cell, KeyInformation.getSecretKeyUsingNodeId(nodeId));
                // If the size of the cell is 512 bytes, the cell is decrypted fully
                if (cell.length == 512) {

                    // Read the necessary parameters
                    byte[] circuitID = ConvertInterface.getCircuitId(cell);
                    byte command = ConvertInterface.getCommand(cell);
                    byte[] payload = ConvertInterface.getPayload(cell);

                    // Read the command to figure out if relay cell or control cell
                    if (command == 0) {
                        // Relay cell
                        byte[] streamId = ConvertInterface.getStreamID(cell);
                        byte relayCommand = ConvertInterface.getRelayCommand(cell);
                        byte[] payloadLength = ConvertInterface.getPayloadLength(cell);
                        /**
                         * 0x1 = relayBegin
                         * 0x2 = relayExtend
                         * 0x3 = relayExtended
                         * 0x4 = relayData
                         * 0x5 = relayConnected
                         */

                        // relayBegin
                        if (relayCommand == 1) {
                            // throw error, because this should only be read at the end node!
                            throw new Exception("Relay cell with relayCommand: 1, received at regular node. " +
                                    "This command should only be interpreted at end node!");
                        } else if (relayCommand == 2) {
                            // This command is not yet implemented, and involves adding a new node to the relay
                            // throw error!
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
                    //If control cell
                    else if (command == 1) {
                        // The cell is a create cell, meaning this cell must respond with a created cell
                        byte[] u = new byte[4];
                        byte[] publicKey = new byte[16];

                        // Reading in the BigInteger u as bytes
                        for (int i = 0; i < 4; i++) {
                            u[i] = payload[i];
                        }

                        // Convert the bytes to a public key
                        PublicKey pk = ConvertInterface.convertToPublicKey(u);

                        // Create the symmetric key
                        KeyGeneration keyGeneration = new KeyGeneration();
                        SecretKey symmetricKey = keyGeneration.generateSecretKey(pk, true);

                        // Add the symmetric key and the IP to the store
                        KeyInformation.addSecretKeyToMap(nodeId, symmetricKey);

                        // Create a control cell and send it back where it came from
                        ControlCell created = new ControlCell((byte) 2, circuitID);
                        Socket previous = getSocket(cellSender);

                        // Create an output stream on that socket
                        DataOutputStream outgoingMessage = new DataOutputStream(previous.getOutputStream());

                        // Send the message
                        outgoingMessage.write(created.getTotalMessage());
                        outgoingMessage.flush();
                    } else if (command == 2) {
                        // This should only happen in the ClientProxy --> did something wrong happen during encryption?
                        throw new Exception("The cell received contained a control bit == 2 and was received by a node");
                    } else if (command == 3) {
                        // The socket must close all the sockets that belong to the circuitID of the package
                        end = true;
                    }
                }
                // If neither
                else {
                    // we must send it to the socket of the next node if this is the case
                    if (cellSender.equals(previousNode)) {

                        // Get the socket
                        Socket socket = getSocket(previousNode);

                        // Generate the outputStream
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                        // Send the cell
                        outputStream.write(cell);
                        outputStream.flush();
                    } else {
                        throw new Exception("The sockets next/previous nodes do not match up with the sender of this cell!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
    }
}
