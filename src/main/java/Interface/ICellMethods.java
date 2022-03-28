package Interface;

import cells.ControlCell;
import cells.RelayCell;
import circuit.Circuit;
import nodes.DirectoryNode;
import nodes.Node;
import proxy.ProxyKeyStore;
import security.Cryptography;
import security.KeyGeneration;
import security.KeyInformation;

import javax.crypto.SecretKey;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import static Interface.ISupportMethods.generateRandomBytes;

/**
 * Interface created in order to distribute the use of methods related to creating the
 * different types of cells across all directories/packets. These methods are
 * therefore quite general with parameters requiring using specified sockets and
 * DataOutputStreams for each individual implementation.
 *
 * This interface instance lowers the coupling of having to import classes just to use
 * their static methods.
 * This interface strengthens cohesion as well, in that there are no classes that
 * naturally could have taken these methods in as purely their own without mixing in other
 * packets. Effectively, this rules that out.
 */
public interface ICellMethods {

    /**
     * Relay command #1
     *
     * Opens a stream and does this by sending a cell all the way to the exit node
     * The exit node will respond with "relay connected" - cell
     * The guardNode will receive this cell first, before it passes the cell on
     */
    static RelayCell relayBegin(byte[] circuitId, ArrayList<Node> circuit) {
        byte[] streamId = generateRandomBytes();

        // Empty array as there is no payload
        byte[] payloadLength = new byte[2];

        // Creating the cell to be relayed
        RelayCell cell = new RelayCell((byte) 1, streamId,
                payloadLength, circuitId);

        // Encrypt once for each element in the circuit
        Cryptography cryptography = new Cryptography();
        cryptography.encryptSpecifiedNumberOfTimes(cell, circuit, circuit.size(), true);

        return cell;
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
    static void relayExtend(byte[] circuitId, InetAddress newNode, boolean lastNode) {
        try {
            Circuit circuit = DirectoryNode.getCircuitWithId(circuitId);

            assert circuit != null;
            int nodesInCircuit = circuit.getLength();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Relay command #3
     *
     * From extended node to the proxy to acknowledge that the circuit was extended
     * @param circuitId the circuit id
     * @param node the node that was just added to the connection
     */
    static void relayExtended(byte[] circuitId, Node node) throws Exception {
        // get circuit using the get circuitMethod
        DirectoryNode.getCircuitWithId(circuitId).addNode(node);

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
    static RelayCell relayData(byte[] data, byte[] streamId, byte[] circuitId, ArrayList<Node> nodes) throws Exception {

        // Create a relay cell is encrypted
        byte[] payloadLength = ISupportMethods.intToByteArray(data.length);

        if(payloadLength.length != 2) {
            throw new Exception("Payload length cannot be " + payloadLength.length + " bytes long. The limit is 2 bytes.");
        }
        else {
            RelayCell cell = new RelayCell((byte) 4,streamId,payloadLength, circuitId);

            // The cell is encrypted
            Cryptography cryptography = new Cryptography();
            cryptography.encryptSpecifiedNumberOfTimes(cell,nodes,nodes.size(), true);

            // Return the cell
            return cell;
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
    static ControlCell create(byte[] circuitId, Socket socket) {
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
            System.arraycopy(u, 0, payload, 0, 3);

            // Add the pk to the payload
            System.arraycopy(publicKeyByteArray, 4, payload, 4, 16);

            // Adding the payload to the new cell
            newCell.setPayload(payload);

            return newCell;
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
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
    static void created(byte[] cell, InetAddress node) {
        try{
            byte[] uAsBytes = new byte[4];
            // Get u from the payload (resides in the first 4 bytes)
            System.arraycopy(cell, 3, uAsBytes, 0, 4);

            // Convert the bytes to a public key
            PublicKey publicKey = ISupportMethods.convertToPublicKey(uAsBytes);

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
    static void destroy(ArrayList<Node> circuit, byte[] circuitId, DataOutputStream outboundMessage) {
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
            Circuit circuitToRemove = DirectoryNode.getCircuitWithId(circuitId);
            DirectoryNode.removeCircuit(circuitToRemove);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
