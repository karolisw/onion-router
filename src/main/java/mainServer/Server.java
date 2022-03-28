package mainServer;


import circuit.Circuit;
import nodes.Node;
import threads.ServerReaderThread;
import threads.WriterThread;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {

    ServerSocket server;
    Socket socket;
    DataOutputStream outboundMessage;
    InetSocketAddress address;
    static InetSocketAddress ipAddress = new InetSocketAddress("localhost", 8888);
    // Capacity: 1024 * 512 bytes = 524288 bytes = 0.5 MB
    static BlockingQueue<byte[]> blockingQueue;

    public Server(int portNumber) {
        try{
            server = new ServerSocket(portNumber);
            blockingQueue = new ArrayBlockingQueue<byte[]>(1024);
        }catch(Exception e){
            System.out.println(e);
        }
    }



    /**
     * All circuit must have a socket connection to each other!
     * Server connects to the guard node --> server has 1 socket
     * Guard node connects to the server and node 2 --> guard node has 2 sockets
     * This happens for all intermediate circuit as well --> intermediate node has 2 sockets
     * The end node connects to the previous intermediate node
     * (and to the remote server upon stream start) --> end node has 2 sockets
     *
     * @param circuit are the nodes in the circuit
     */
    public void connectCircuit(Circuit circuit) {
        try {
            ArrayList<Node> circuitNodes = circuit.getNodes();
            // Get the guard node

            SocketAddress guardNode = circuitNodes.get(0).getServerSocket().getLocalSocketAddress();

            for (int i = 0; i < circuitNodes.size(); i++) {
                // The server must connect to the first node
                // Each node must open up their circuit to the previous node
                Node node = circuitNodes.get(i);
                // Creating a connection starts up a thread that reads information from and sends to
                node.createConnection();
            }

            // For the first node (guard node), this server must connect to it!
            socket = new Socket();
            System.out.println("trying to connect to guard node" + guardNode);
            socket.connect(guardNode);
            // Hand the socket over to the writer thread

            WriterThread writerThread = new WriterThread(socket, blockingQueue);
            writerThread.start();
            System.out.println("created writer thread");

            ServerReaderThread readerThread = new ServerReaderThread(socket,circuitNodes, blockingQueue);
            readerThread.start();
            System.out.println("created reader thread");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Upon receipt of the relay connected method from the exit node,
     * the proxy sends a SOCKS reply to notify the application of its success.
     */
    public void connectionSuccess() {

    }

    /**
     * Method that uses the begin relay method through worker threads
     */
    public byte[] initiateRelay(Circuit circuit) {
        WriterThread writerThread = new WriterThread(socket, blockingQueue, 2,circuit);
        writerThread.start();
        return writerThread.getStreamId();
    }

    public void sendRequest(Circuit circuit, String request, byte[] streamId) {
        WriterThread writerThread = new WriterThread(socket, blockingQueue, 2,
                circuit, request, streamId);
        writerThread.start();
    }


    /**
     * Method uses the create() -and created() control-cell methods to handshake (generate symmetric keys)
     * with the ClientProxy (server)
     */
    public void fullCircuitHandshake(Circuit circuit) throws Exception {
        circuit.getNodes().get(0).fullCircuitHandshake(circuit);
    }

    public static InetSocketAddress getSocketAddress() {
        return ipAddress;
    }

    public static BlockingQueue<byte[]> getBlockingQueue() {
        return blockingQueue;
    }
}
