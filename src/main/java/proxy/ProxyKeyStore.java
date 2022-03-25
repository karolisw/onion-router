package proxy;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.security.Key;
import java.util.HashMap;

/**
 * A keystore is created for each circuit
 * The keyStore contains all the sharedKeys of the client proxy
 *
 */
public class ProxyKeyStore {
    int circuitId;
    // Key = shared key; InetAddress = the address of the node the key is shared with
    static HashMap <InetAddress, SecretKey> sharedKeyMapping = new HashMap<>();

    /**
     * Retrieves the InetAddress of the node the proxy shares the specified key with
     *
     * @param address is the specified IP-address
     * @return the key that corresponds to the specific IP-address
     */
    public static SecretKey getKeyFromIpAddress(InetAddress address) {
        return sharedKeyMapping.get(address);
    }

    public static void addKey(InetAddress address, SecretKey key) {
        sharedKeyMapping.put(address, key);
    }
}
