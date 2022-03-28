package proxy;

import Interface.ICellMethods;
import cells.Cell;
import cells.RelayCell;
import circuit.Circuit;
import nodes.Node;
import security.Cryptography;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;


/**
 * The client communicates through the ClientProxy class.
 * This class is where the proxy connection point starts.
 * Upon connection, the ClientProxy will attempt to connect to
 * the end node; then the intermediary node; then the guard node.
 * After that, messages and requests can be relayed
 */
public class ClientProxy {

    Socket socket;
    DataOutputStream outboundMessage;

    /**
     *
     * Upon receipt of the relay connected method from the exit node,
     * the proxy sends a SOCKS reply to notify the application of its success.
     */
    public void connectionSuccess() {

    }

    /**
     * Method uses the create() -and created() control-cell methods to handshake (generate symmetric keys)
     * with the ClientProxy (server)
     */
    public void fullCircuitHandshake(Circuit circuit) throws Exception {
        // For all the nodes in the circuit, running the create method
        for (int i = 0; i < circuit.getLength(); i++) {
            ICellMethods.create(circuit.getId(), socket);
        }
        // Listen for a packet with the created method

        // Creating symmetric key with the payload of the created-cell
        // Adding the symmetric key (secret key) and the node IP to the ProxyKeyStore
    }


    /**
     * Using this constructor will create a SOCKS proxy that is able to:
                    - Connect to the nodes using TLS (create circuits) and key exchange
                    - Communicate with servers through the nodes using AES encryption
                    - Send/receive cells using the built-in SOCKS library

     * The proxy instance will be created through the Java PROXY APIs SOCKS-type
     * The connection is created through the use of the openConnection(proxy) method
     *
     * @param proxyPort is the port that the will run on
     * @param proxyAddress is the address URL where the proxy will reside
     */
    public Proxy createProxy(String proxyAddress, int proxyPort) {
        try {
            // Creating proxy instance "proxy"
            SocketAddress address = new InetSocketAddress(proxyAddress, proxyPort);
            return new Proxy(Proxy.Type.SOCKS, address);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

        /**
     * Instantiates (creates) the actual connection for the proxy created in the constructor
     * // TODO maybe InetAddress instead of InetSocketAddress in order to use localhost
     * @param proxy the proxy created in the constructor
     * @param destinationIp the ip address of the guard node
     * @return
     */
    public void createConnection(Proxy proxy, InetAddress destinationIp, int destinationPort) {
        if(proxy.type() == Proxy.Type.SOCKS) {
            try {
                // Having the proxy as a parameter means the socket will connect to its destination through the proxy
                // figure out how to display the buffer with the collected message from the internet retrieved
                socket = new Socket(proxy);
                socket.setSendBufferSize(512);
                socket.setReceiveBufferSize(512);
                InetSocketAddress dest = new InetSocketAddress(destinationIp, destinationPort);
                socket.connect(dest);
                // The socket is ready to receive cells!
                outboundMessage = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // todo create a listening thread and a sending thread perhaps?
    }

    /**
     * When reading a stream of cells, each cell is decrypted.
     * After that, this method is called for each of the cell.
     * When the final cell is received, the total message is sent to the browser.
     *
     * @param cell is the cell received from the onion router at the end (endNode).
     */
    public void retrievePayload(RelayCell cell) {
        // method reads the payload of the incoming cells and adds it to the buffer
        // when the relayEnd method is received, the bytes are transferred from
        // the buffer to the browser
        // If relayExtended
        try {
            outboundMessage.write(cell.getPayload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method created for high code-composition.
     * Method takes care of encryption depending on how many hops the
     * specified cell needs
     *
     * @param routers are all the nodes in the current circuit
     * @param hops are how many nodes ahead this cell should be sent (aka. the number of times we encrypt)
     * @param cell the cell we wish to send could be both relay -and command cell)
     */
    public void sendCell(ArrayList<Node> routers, int hops, Cell cell) {
        Cryptography cryptography = new Cryptography();

        try {
            // First the cell is encrypted the specified amount of times (hops)
            cryptography.encryptSpecifiedNumberOfTimes(cell,routers, hops, true);

            // The cell is written to the DataOutputStream
            outboundMessage.write(cell.getTotalMessage());

            // The cell is sent
            outboundMessage.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Support-method created to make handling of received cells easier
     * Method interprets the type of cell (command), and returns it
     * Method can be used in general receie-method in order to
     * use the correct method for handling the cell (ex: created() or relayExtended())
     *
     * @param cell is the cell that was received
     * @param routers are all the routers in the circuit
     *
     * @return the command present in the cell after decryption (must be numbers 0-3)
     */
    public byte receiveCell(Cell cell, ArrayList<Node> routers) throws Exception {
        // The cell was just received and must be decrypted
        Cryptography cryptography = new Cryptography();
        byte[] decryptedCell = cryptography.decryptionClientSide(cell, routers);

        // After decryption, the cell must be interpreted
        byte command = decryptedCell[2];
        // After interpretation, the cell
        if (command > 3) {
            throw new Exception(" The command contained a number greater than 3, decryption error!");
        } else {
            return command;
        }
    }

    /**
     * When it is clear that a cell is a relay-cell, this method
     * aids in interpreting what type of relay cell it is
     * @return the relayCommand
     */
    public byte interpretRelayCell(RelayCell relayCell) {
        byte[] cellAsByteArray = relayCell.getTotalMessage();
        return cellAsByteArray[7];
    }
    /**
     * When it is clear that a cell is a relay-cell, this method
     * aids in interpreting what type of relay cell it is
     * @return the relayCommand
     */
    public byte interpretRelayCell(byte[] relayCell) {
        return relayCell[7];

    }
}
