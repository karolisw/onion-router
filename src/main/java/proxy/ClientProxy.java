package proxy;

import Interface.ConvertInterface;
import cells.Cell;
import cells.ControlCell;
import cells.RelayCell;
import circuit.Circuit;
import client.Client;
import nodes.DirectoryNode;
import nodes.Node;
import security.Cryptography;
import security.KeyGeneration;
import security.KeyInformation;

import javax.crypto.SecretKey;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.ArrayList;
import java.util.Random;

import static Interface.ConvertInterface.generateRandomBytes;

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
     * For usability, this method combines relayBegin (relay command #1)
     * relay extend (relay command #2) and relay extended (relay command #3)
     */
    public Circuit establishCircuit() {
        // Getting all the nodes that must be added
        ArrayList<Node> routers = Client.getChosenRouters();
        try {
            Circuit circuit = new Circuit();

            for (int i = 0; i < routers.size(); i++) {
                Node currentNode = routers.get(i);

                // If last node, there is no need to set next node
                if (routers.size() == i+1) {
                    currentNode.setPreviousNode(routers.get(i-1).getIpAddress());

                    // Adding the node to the circuit
                    circuit.addNode(currentNode.getIpAddress());

                    // Nodes should open up their connection to their
                    //currentNode.
                }

                // If guard node, there is no need to set previous node
                else if (i == 0) {
                    currentNode.setNextNode(routers.get(i+1).getIpAddress());

                    // Adding the node to the circuit
                    circuit.addNode(currentNode.getIpAddress());

                }

                // If intermediate node, both previous -and next node must be set
                else {
                    currentNode.setPreviousNode(routers.get(i-1).getIpAddress());
                    currentNode.setNextNode(routers.get(i+1).getIpAddress());

                    // Adding the node to the circuit
                    circuit.addNode(currentNode.getIpAddress());
                }
            }
            // Add the circuit to the lists of all circuits
            DirectoryNode.addCircuit(circuit);

            // Return the circuit
            return circuit;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method uses the create() -and created() control-cell methods to handshake (generate symmetric keys)
     * with the ClientProxy
     */
    public void fullCircuitHandshake(int circuitId) {
        // For all the nodes in the circuit, running the create method
        // Listen for a packet with the created method
        // Creating symmetric key with the payload of the created-cell
        // Adding the symmetric key (secret key) and the node IP to the ProxyKeyStore
    }
    //TODO should i have a method that retrieves the nodes to be set by the user?
    /**
     * Relay command #1
     *
     * Opens a stream and does this by sending a cell all the way to the exit node
     * The exit node will respond with "relay connected" - cell
     * The guardNode will receive this cell first, before it passes the cell on
     */
    public void relayBegin(byte[] circuitId, ArrayList<InetAddress> circuit) {
        try {
            byte[] streamId = generateRandomBytes();

            // Empty array as there is no payload
            byte[] payloadLength = new byte[2];

            // Creating the cell to be relayed
            RelayCell cell = new RelayCell((byte) 1, streamId,
                    payloadLength, circuitId);

            // Encrypt once for each element in the circuit
            Cryptography cryptography = new Cryptography();
            cryptography.encryptSpecifiedNumberOfTimes(cell, circuit, circuit.size(), true);

            // Send the cell
            outboundMessage.write(cell.getTotalMessage());
            outboundMessage.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Relay command #2
     *
     * Extend to another node selected by the client
     * Involves a handshake (key agreement) between client-side proxy server and newNode,
     * which will be placed in between the exit node and the proxy
     *
     * @param circuitId is the circuit the node should be added to
     * @param newNode is either a GuardNode or an IntermediaryNode (exitNode is already chosen) --> todo type check
     */
    public void relayExtend(int circuitId, InetAddress newNode, boolean lastNode) {
        // the user has decided how many nodes should be present --> they also chose the end node
        // this happens outside this method, meaning that this method will continue putting nodes in order
        // until what has to be an outside loop is complete

        // This method must have an if at the start, where it is determined whether the end node is already
        // added or not (at the last position of the circuit). If yes --> the circuit is complete --> throw exception
        // This method must assign nextNode and previousNode as nodes are added,
        // as well as perform handshakes between new nodes and the ClientProxy
                // If last node, the node added must have an EndNode class check, and are then added last
                // if 1 --> the new node is connected to the guard node (previous node) and the ClientProxy (handshake)
                // if 2 --> the new node is connected to the intermediary node (previous node) and
                //          the intermediary node marks the new node as its (next node)
                // if 3 --> This continued
                // last --> the new node is connected to the previous intermediary node and the proxy

        try {
            Circuit circuit = DirectoryNode.getCircuitWithId(circuitId);

            assert circuit != null;
            int nodesInCircuit = circuit.getLength();


        } catch (Exception e) {
            e.printStackTrace();
        }

        // todo give both nodes each other's IP address (in their KeyInformation)
    }

    /**
     * Relay command #3
     *
     * From extended node to the proxy to acknowledge that the circuit was extended
     * @param circuitId the circuit id
     * @param node the node that was just added to the connection
     */
    public void relayExtended(int circuitId, InetAddress node) throws Exception {
        // get circuit using the get circuitMethod
        DirectoryNode.getCircuitWithId(circuitId).addNode(node);

    }



    /**
     *
     * Upon receipt of the relay connected method from the exit node,
     * the proxy sends a SOCKS reply to notify the application of its success.
     */
    public void connectionSuccess() {


    }

    /**
     * Relay command #4
     *
     * The proxy now accepts data from the application’s TCP stream,
     * packaging it into relay data cells and sending those cells along the circuit to the end node
     * This type of Relay-cell is always encrypted all the way (circuit.size() times)
     * As this is the
     * @param data is the data to be transferred to the end node
     */
    public void relayData(byte[] data, byte[] streamId, byte[] circuitId, ArrayList<InetAddress> nodes) throws Exception {

        // Create a relay cell is encrypted
        byte[] payloadLength = ConvertInterface.intToByteArray(data.length);

        if(payloadLength.length != 2) {
            throw new Exception("Payload length cannot be " + payloadLength.length + " bytes long. The limit is 2 bytes.");
        }
        else {
            RelayCell cell = new RelayCell((byte) 4,streamId,payloadLength, circuitId);

            // The cell is encrypted
            Cryptography cryptography = new Cryptography();
            cryptography.encryptSpecifiedNumberOfTimes(cell,nodes,nodes.size(), true);

            // Sending the cell
            outboundMessage.write(cell.getTotalMessage());
            outboundMessage.flush();
        }
    }


    /**
     * Control command #1
     *
     * A new circuit has already been made. This method performs a handshake with a node.
     * The handshake happens through the use of Curve 25519 in this way:
                The node andClientProxy each create their own private key.
                The node and ClientProxy each create their own public key.
                Both send their public keys to each other. It is okay if
                someone is sniffing on the public keys.
                Using their own private key in conjunction with the received
                public key, they both create a symmetric key (shared secret)
                without ever having exchanged it.

     * This method must be called for each node in the circuit
     *
     */
    public void create(byte[] circuitId) {
        try{
            // The ClientProxy creates its private and public keys
            KeyGeneration generator = new KeyGeneration();

            // Creating the key pairs
            generator.generateKeyPair(circuitId, socket.getLocalAddress());

            // The keyPair is available through the getCurrentKey() function
            KeyInformation information = generator.getCurrentKey();

            // Retrieving the public-key from the KeyInformation object
            Key publicKey = information.getLocalPublicKey();

            // Retrieve u as well, it is needed for the receiving node to transform the payload bytes to PublicKey
            BigInteger bigIntegerU = information.getU();

            // Convert bigIntegerU to byte array
            byte[] u = bigIntegerU.toByteArray();

            // The clientProxy creates a control cell containing the public key
            ControlCell newCell = new ControlCell((byte)1, circuitId);

            // u + publicKey = 4 bytes + 16 bytes = 20 bytes
            byte[] payload = new byte[20];

            // Convert publicKey to byteArray
            byte[] publicKeyByteArray = publicKey.getEncoded();

            // Add u to the payload
            for (int i = 0; i < 4; i++) {
                payload[i] = u[i];
            }

            // Add the pk to the payload
            for (int i = 4; i < 20; i++) {
                payload[i] = publicKeyByteArray[i];
            }

            // Adding the payload to the new cell
            newCell.setPayload(payload);

            // The clientProxy sends the control-cell to the connected socket (guard node)
            outboundMessage.write(newCell.getTotalMessage());
            outboundMessage.flush();

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Control command #2
     *
     * When the received() method reads a #2 control command, this method is run
     * Response sent from the node added to the path using create method (control command #1)
     * The response is a created-cell containing g^y along with the negotiated key K = g^xy
     *
     * @param node is the node the proxy is handshaking with at the time
     * @param cell is the cell we just received (control cell)
     */
    public void created(byte[] cell, InetAddress node) {
        try{
            byte[] uAsBytes = new byte[4];
            //ByteBuffer byteBuffer = ByteBuffer.wrap(cell);
            // Get u from the payload (resides in the first 4 bytes)
            for (int i = 3; i < 7 ; i++) {
                uAsBytes[i-3] = cell[i];
            }

            // Convert the bytes to a public key
            PublicKey publicKey = ConvertInterface.convertToPublicKey(uAsBytes);

            // Create the symmetric key
            KeyGeneration keyGeneration = new KeyGeneration();
            SecretKey symmetricKey = keyGeneration.generateSecretKey(publicKey,true);

            // Add the symmetric key and the IP to the store
            ProxyKeyStore.addKey(node,symmetricKey);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Control command #3
     * Tear down a circuit
     */
    public void destroy(ArrayList<InetAddress> circuit, byte[] circuitId) {
        try {
            // For all nodes in circuit, starting at the end node
            for (int i = circuit.size(); i > 0; i--) {
                // Create a destroy() control cell
                ControlCell cell = new ControlCell((byte) 3, circuitId);

                // Encrypt the amount of times needed --> i
                Cryptography cryptography = new Cryptography();
                cryptography.encryptSpecifiedNumberOfTimes(cell, circuit, i, true);

                // Send the node to the node guard node and let it handle the rest
                outboundMessage.write(cell.getTotalMessage());
                outboundMessage.flush();
            }

            // After all nodes are destroyed (closed), the circuit is removed from the directory node
            Circuit circuitToRemove = DirectoryNode.getCircuitWithId(ConvertInterface.byteToInt(circuitId));
            DirectoryNode.removeCircuit(circuitToRemove);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

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


            // 1. initiate a SOCKS proxy server (here: "socks.mydomain.com")
            // 2. write in the commandline that:
            // $ java -DsocksProxyHost=socks.mydomain.com GetURL
            // Now, every outgoing TCP socket will go through the SOCKS proxy server at socks.mydomain.com:1080.
            // 1080 is the default port if no other port is specified
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
    public void sendCell(ArrayList<InetAddress> routers, int hops, Cell cell) {
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
    public byte receiveCell(Cell cell, ArrayList<InetAddress> routers) throws Exception {
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
