package security;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.Key;
import java.security.PublicKey;
import java.util.HashMap;

public class KeyInformation {
    private byte[] sharedSecret; // shared  secret
    private Key privateKey;
    private PublicKey localPublicKey;
    private PublicKey foreignPublicKey;
    private SecretKey secretKey;
    private BigInteger u;

    private byte[] circuitId;

    // The ip address of the onion router
    private InetAddress ipAddress;

    // HashMap of nodeId along with the secret key!
    private static HashMap <Integer, SecretKey> nodeSecretKeyMap = new HashMap<>();

    /**
     * Using this object, a Node can store all the information it needs about it neighbors
     * Ex: An intermediary node will have two such objects, one for the previous node, and one for the next node
     *
     * @param circuitId is the id of the circuit the two nodes are part of
     * @param ipAddress is the ip address of the node that holds this information
     */
    public KeyInformation(byte[] circuitId, InetAddress ipAddress, Key privateKey, PublicKey publicKey, BigInteger u) {
        this.circuitId = circuitId;
        this.ipAddress = ipAddress;
        this.privateKey = privateKey;
        this.localPublicKey = publicKey;
        this.u = u;
    }


    // GETTERS
    public static SecretKey getSecretKeyUsingNodeId(int nodeId) {
        return nodeSecretKeyMap.get(nodeId);
    }
    public BigInteger getU() {
        return u;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public byte[] getCircuitId() {
        return circuitId;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public PublicKey getLocalPublicKey() {
        return localPublicKey;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }


    // SETTERS
    public static void addSecretKeyToMap(int nodeId, SecretKey secretKey) {
        nodeSecretKeyMap.put(nodeId, secretKey);
    }
    public void setSharedSecret(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public void setCircuitId(byte[] circuitId) {
        this.circuitId = circuitId;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }
}
