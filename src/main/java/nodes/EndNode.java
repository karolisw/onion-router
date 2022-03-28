package nodes;

import cells.RelayCell;
import security.Cryptography;
import security.KeyInformation;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static Interface.ISupportMethods.*;

public class EndNode extends Node{
    Proxy endNodeProxy;
    Cryptography cryptography = new Cryptography();
    DataInputStream inputStream;
    DataOutputStream outputStream;
    int id;


    /**
     * @param ipAddress the Ip-address the end node will run on
     * @param portNumber the port number the node will run on
     */
    public EndNode(InetAddress ipAddress, int portNumber) {

        SocketAddress addr = new InetSocketAddress(ipAddress, portNumber);
        endNodeProxy = new Proxy(Proxy.Type.SOCKS, addr);
        setRandomId();
    }

    /**
     * We run this method when we get a relayData cell
     * This socket will be opened with the requested URL
     * When the whole web-page has been downloaded, the endNode closes this socket
     * The end node then sends a streamClosed cell
     * @param streamId is given to the method upon relayData cell received.
     *                 This is because this method is only initialized when such cells
     *                 are opened. The streamIds are needed at the end of the method in order
     *                 to send a streamClosed-cell using them
     *
     * @param destinationURL
     * @param destinationPort
     * @return
     * @throws Exception
     */
    public URLConnection createConnection(String destinationURL, int destinationPort,
                                          byte[] streamId, byte[] circuitId) throws Exception {
        if(endNodeProxy != null) {
            try {
                // Having the proxy as a parameter means the socket will connect to its destination through the proxy
                Socket socket = new Socket(endNodeProxy);
                InetSocketAddress dest = new InetSocketAddress(destinationURL, destinationPort);
                socket.connect(dest);

                // Now, we set up a new output and input stream
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream()); // todo will this send the information back?
                byte[] payload;

                // The total number of bytes read into the buffer, or -1 if there
                // is no more data because the end of the stream has been reached.
                int read = 0;
                while (read != -1) {
                    // Initialize a new chunk of memory for payload
                    payload = new byte[512];

                    // Read 504 bytes and put it into the payload
                    read = inputStream.read(payload, 0, payload.length);

                    // Create a data relay cell
                    RelayCell relayCell = new RelayCell((byte) 4,
                            streamId, intToByteArray(payload.length),circuitId);

                    // Encrypt the cell
                    cryptography.encrypt(relayCell, KeyInformation.getSecretKeyUsingNodeId(id));

                    // Send the relay cell created back
                    outputStream.write(relayCell.getTotalMessage());
                    outputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // At this point, the whole website is sent to the client, and the stream should end
            RelayCell relayCell = streamClosed(streamId,circuitId);

            // Encrypt the cell
            Cryptography cryptography = new Cryptography();
            cryptography.encrypt(relayCell, KeyInformation.getSecretKeyUsingNodeId(id));

            // Send the relay cell marking the stream end
            outputStream.write(relayCell.getTotalMessage());
            outputStream.flush();
        }
        throw new Exception("The end node has not been initialized!");
    }

    /**
     * Relay command #5
     *
     * When the end node receives a relayBegin cell, the end node must
     * generate a relayConnected cell to signal that it is ready
     *
     *
     */
    private void relayConnected(byte[] cell) {
        // Get a hold of all the variables needed to create the cell
        byte[] streamId = getStreamID(cell);
        byte[] circuitId = getCircuitId(cell);
        byte[] payloadLength = getPayloadLength(cell);

        try{
            // Create the cell
            RelayCell relayCell = new RelayCell((byte) 5, streamId,
                    payloadLength, circuitId );

            // Encrypt the cell
            Cryptography cryptography = new Cryptography();
            cryptography.encrypt(relayCell, KeyInformation.getSecretKeyUsingNodeId(id));

            // Send the cell back to the client proxy
            outputStream.write(relayCell.getTotalMessage());
            outputStream.flush();

        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                | BadPaddingException | IOException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    /**
     * Relay method #6
     *
     * This cell is sent when a stream is fully read from the specified webpage
     * The cell is interpreted as a relay stream closed cell in the client proxy.
     * Then, the clientProxy must serve up the whole downloaded page for the client
     * to see.
     */
    public RelayCell streamClosed(byte[] streamID, byte[] circuitId) {
        // Creating a relay cell which is to be sent to the previous node
        RelayCell cell = new RelayCell((byte) 6,streamID,intToByteArray(0),circuitId);

        // Add an empty payload
        cell.setPayload(new byte[504]);

        return cell;
    }
}
