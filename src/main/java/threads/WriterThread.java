package threads;

import Interface.ICellMethods;
import Interface.ISupportMethods;
import cells.ControlCell;
import cells.RelayCell;
import circuit.Circuit;

import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

/**
 * Generic writer thread -> can be used in any class!
 */
public class WriterThread extends Thread {
    Socket socket;
    DataOutputStream outStream;
    Circuit circuit;
    protected BlockingQueue<byte[]> blockingQueue = null;
    int code = 0;
    byte[] streamId;
    String request = ""; // The webpage the client requests to use

    /**
     * The writer thread writes cells received in the read thread to the socket it is connected to
     *
     * @param socket could be both previous and next node
     * @param blockingQueue is the queue cells are placed in when read in the reading thread
     */
    public WriterThread(Socket socket, BlockingQueue<byte[]> blockingQueue) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
    }

    /**
     * Writer threads also need to start communication at some times. This constructor is for use
     * in those cases.
     * @param socket the socket the writer thread writes to
     * @param blockingQueue the blocking queue the thread is part of
     * @param code can be:
     *             1 = send create messages to all the nodes
     *             2 = send relay begin cell
     */
    public WriterThread(Socket socket, BlockingQueue<byte[]> blockingQueue, int code, Circuit circuit) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.code = code;
        this.circuit = circuit;
    }

    /**
     * Writer threads also need to start communication at some times. This constructor is for use
     * in those cases.
     * @param socket the socket the writer thread writes to
     * @param blockingQueue the blocking queue the thread is part of
     * @param code can be:
     *             1 = send create messages to all the nodes
     *             2 = send relay begin cell
     *             3 = relay data when prompted to by the client
     */
    public WriterThread(Socket socket, BlockingQueue<byte[]> blockingQueue, int code, Circuit circuit,
                        String request, byte[] streamId) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.code = code;
        this.circuit = circuit;
        this.request = request;
        this.streamId = streamId;
    }

    public byte[] getStreamId() {
        return streamId;
    }

    /**
     * All connection handling happens in the run method.
     */
    public void run() {
        try {
            // Creating the streams that are used to talk through the socket(s)
            if(socket.isConnected()) {
                outStream = new DataOutputStream(socket.getOutputStream());

                // If create
                if(code == 1) {
                    // Send circuit.size() -1 amounts of create cells
                    for (int i = 1; i < circuit.getNodes().size(); i++) {
                        // Create the cell
                        ControlCell createCell = ICellMethods.create(circuit.getId(),socket);
                        // Get the blocking queue of the first node
                        blockingQueue.put(createCell.getTotalMessage());
                        byte[] cell = blockingQueue.take();
                        outStream.write(cell);
                        outStream.flush();
                    }
                }
                // if relayBegin
                if(code == 2) {
                    // Create relay begin cell
                    RelayCell relayBegin = ICellMethods.relayBegin(circuit.getId(),circuit.getNodes());
                    streamId = ISupportMethods.getStreamID(relayBegin.getTotalMessage());
                    // Add the cell to the queue
                    blockingQueue.put(relayBegin.getTotalMessage());
                    byte[] cell = blockingQueue.take();
                    outStream.write(cell);
                    outStream.flush();
                }

                if(code == 3) {
                    // Validate that the request is not empty!
                    if(request.length() < 1 || streamId == null) {
                        throw new Exception("There must be a request present in order to create a relay data cell!");
                    }
                    else{
                        // Create the relayData cell
                        RelayCell relayData = ICellMethods.relayData(request.getBytes(StandardCharsets.UTF_8),
                                streamId, circuit.getId(), circuit.getNodes());

                        // Add the cell to the queue
                        blockingQueue.put(relayData.getTotalMessage());
                        byte[] cell = blockingQueue.take();
                        outStream.write(cell);
                        outStream.flush();
                    }
                }

                while(true) {
                    byte[] cell = blockingQueue.take();
                    if(cell.length == 512) {
                        outStream.write(cell);
                        outStream.flush();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
