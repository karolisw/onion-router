package threads;

import Interface.ICellMethods;
import Interface.ISupportMethods;
import cells.Cell;
import nodes.DirectoryNode;
import nodes.Node;
import security.Cryptography;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * This thread class does all the heavy handling of reading and handling incoming cells
 * All cells that are to be sent from the client to the endNode or other nodes are
 * put into the blockingQueue. The writer thread does the sending-part.
 */
public class ServerReaderThread extends Thread {
    int connectionNumber;
    Socket socket;
    Socket browserSocket;
    DataInputStream inStream;
    DataOutputStream outStream;
    DataOutputStream outStreamBrowser;
    int maxCellSize;
    ArrayList<Node> circuit;

    protected BlockingQueue<byte[]> blockingQueue = null; // todo perhaps not correct with byte[]?
    /**
     * @param socket is the socket created upon connection to the guard node
     * @param circuit is needed in order to calculate the size of incoming cells (each node = 12+16  bytes extra) ,
     *                and in order to decrypt/encrypt
     */
    public ServerReaderThread(Socket socket, ArrayList<Node> circuit, BlockingQueue<byte[]> blockingQueue) {
        this.socket = socket;
        this.connectionNumber = connectionNumber;
        this.circuit = circuit;
        this.blockingQueue = blockingQueue;

        // No cells are allowed to be bigger than this
        maxCellSize = 512 + (circuit.size() * 28);
    }

    /**
     * All connection handling happens in the run method.
     */
    public void run() {
        try {
            // Creating the streams that are used to talk through the socket(s)
            inStream = new DataInputStream(socket.getInputStream());

            byte[] incomingCell;

            int read = 0;
            while(true) {
                // How many bytes are there available to read in the stream
                int bytesToRead = inStream.available();

                // If the bytes available contain just one cell
                if(bytesToRead <= maxCellSize) {

                    //Allocate that number of bytes for the cell
                    incomingCell = new byte[bytesToRead];
                }
                // If the bytes available contain more than one cell
                else {
                    throw new Exception("There is not sufficient handling for sending more than one cell at a time! ");
                }

                // We read the bytes into the new cell
                read = inStream.read(incomingCell,0, bytesToRead);
                blockingQueue.put(incomingCell);

                if(incomingCell.length != 0) {

                    // Check to see the command of the cell in order to know if it is encrypted (created cells are not)
                    // Only created cells are allowed to be received like this
                    if (ISupportMethods.getCommand(incomingCell) == 2) {
                        ICellMethods.created(incomingCell, socket.getInetAddress());
                        System.out.println("Created cell received in server reader thread");
                    }
                    else {
                        // Decrypt the cell
                        Cryptography cryptography = new Cryptography();
                        cryptography.decryptionClientSide(Cell.bytesToCellObject(incomingCell), circuit);


                        // The cell is now decrypted, and the next step is to figure out what type of cell it is
                        byte[] circuitID = ISupportMethods.getCircuitId(incomingCell);
                        byte command = ISupportMethods.getCommand(incomingCell);

                        // The server should not receive encrypted control cells
                        if (command > 0 && command < 4) {
                            throw new Exception("The server received a command cell with command: " + command + ".");
                        }
                        // If relay cell
                        if (command == 0) {
                            // Get the relay command
                            byte relayCommand = ISupportMethods.getRelayCommand(incomingCell);

                            // The server may handle relay commands 4, 5 and 6
                            // If relay data cell
                            if (relayCommand == 4) {
                                // All these data cells must be placed in a buffer until we receive a stream closing cell
                                byte[] cellPayload = ISupportMethods.getPayload(incomingCell);
                                blockingQueue.put(cellPayload);
                            }

                            // If relay connected cell
                            else if (relayCommand == 5) {
                                // Set the circuit in a connected state
                                DirectoryNode.getCircuitWithId(circuitID).setRelayingMessage(true);
                                // The server can now send out relay data cells!
                            }

                            // If stream closing cell
                            else if (relayCommand == 6) {
                                // We have received all the information that was requested by the client!
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
