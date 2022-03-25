package security;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Arrays;
import java.util.Random;

public class KeyGeneration implements Destroyable {
    static KeyInformation currentKey;
    private static final int AES_KEY_SIZE = 128;


    /**
     * XDH: The API uses the string "XDH" to refer to Diffie-Hellman key agreement
     *      using the operations and representation described in RFC 7748.
     * NamedParameterSpec: XDH parameters may be specified by a single standard name which
     *      identifies the curve and other parameters that will be used in the key agreement operation.
     *      The new class NamedParameterSpec is used to specify this name to the provider.
     * U (BigInteger): A u-coordinate is an element of the field of integers modulo some
     *      value that is determined by the algorithm parameters.
     *      This field element is represented by a BigInteger which may hold any value.
     *      That is, the BigInteger is not restricted to the range of canonical field elements.
     *
     * @throws NoSuchAlgorithmException if KeyFactory cannot get instance "XDH"
     * @throws InvalidAlgorithmParameterException if the given parameters are inappropriate for this key pair generator
     * @throws InvalidKeySpecException if the given key specification is inappropriate for this key factory to produce a public key
     */
    public void generateKeyPair(byte[] circuitId, InetAddress ip) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        //  KeyPairGenerator class is used to generate pairs of public and private keys.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("XDH");
        // Setting the key-size
        // kpg.initialize(255);

        // This class is used to specify any algorithm parameters that are determined by a standard name.
        NamedParameterSpec paramSpec = new NamedParameterSpec("X25519");

        // Initializes the key pair generator using the specified parameter set (paramSpec) and the SecureRandom
        // implementation of the highest-priority installed provider as the source of randomness.

        kpg.initialize(paramSpec); // equivalent to kpg.initialize(255)
        //alternatively: kpg = KeyPairGenerator.getInstance("X25519")

        // Generates a key pair.
        // If this KeyPairGenerator has not been initialized explicitly, provider-specific
        // defaults will be used for the size and other (algorithm-specific) values of the generated keys.
        KeyPair kp = kpg.generateKeyPair();

        // Key factories are used to convert keys (opaque cryptographic keys of type Key)
        // into key specifications (transparent representations of the underlying key material), and vice versa.
        KeyFactory kf = KeyFactory.getInstance("XDH");

        // u may hold any value (16 bit in my implementation
        BigInteger u = generateRandomBigInteger();

        // A class representing elliptic curve public keys as defined in RFC 7748,
        // including the curve and other algorithm parameters.
        XECPublicKeySpec pubSpec = new XECPublicKeySpec(paramSpec, u);
        PublicKey pubKey = kf.generatePublic(pubSpec);

        // The current key is updated with the information needed to move on in the handshake and future routing
        currentKey = new KeyInformation(circuitId, ip, kp.getPrivate(),pubKey,u);
    }

    /**
     * Using the public key received through the use of control cell methods "create()" and "created()",
     * each node generates their own shared secret using each other's public keys AND their own private keys :-)
     *
     * @param firstPhase For each of the correspondents in the key exchange, doPhase needs to be called.
     *                   For example, because this key exchange is with another party,
     *                   doPhase needs to be called twice. The first time setting the
     *                   lastPhase flag to false, and the second time setting it to true
     *
     * @param foreignPublicKey is the primary key that the connecting node sends to the client proxy (ProxyServer)
     *                         and vise versa
     */
    //todo it is worth a try to set firstphase == true on both of the tries because someone did it on stack
    private byte[] generateSharedSecret(PublicKey foreignPublicKey, boolean firstPhase) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement ka = KeyAgreement.getInstance("XDH");
        ka.init(currentKey.getPrivateKey());

        // For each of the correspondents in the key exchange, doPhase needs to be called.
        // For example, because this key exchange is with another party,
        // doPhase needs to be called twice.
        // The first time setting the lastPhase flag to false, and the second time setting it to true
        ka.doPhase(foreignPublicKey, firstPhase);

        // Generates the key for the key agreement, and places it into the buffer secret
        // Passing the algorithm name to generateSecret will give you a key with
        // the appropriate size for your algorithm (i.e. shortened to 256 bits for AES).
        byte[] secret = ka.generateSecret(); // todo generate using Aes

        // Finish the current key-information object
        currentKey.setSharedSecret(secret);

        return secret;

        // get foreign public key --> throw exception if it is not there (the create method has not yet been called)
    }

    /**
     * Method must be used by each of the parts participating in the handshake
     * A Diffie - Hellman curve 25519 secret is created, and from that,
     * an AES key is created through usage of the hashing function SHA-256.
     * Requirements are that both parts have each other's public key, and each own private key
     *
     * @param firstPhase is a boolean that must be true the first time the method is run, and false the second time
     * @param foreignPublicKey is the public key of the connectingNode.
     * @return a SecretKey instance (an interface that groups together secret keys)
     * @throws NoSuchAlgorithmException when a particular cryptographic algorithm
     *                                  is requested but is not available in the environment.
     */
    public SecretKey generateSecretKey(PublicKey foreignPublicKey, boolean firstPhase) throws NoSuchAlgorithmException {
        try {
            // The curve 25519 secret is created
            byte[] secret = generateSharedSecret(foreignPublicKey, firstPhase);
            // Creating a message digest to hash the secret
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            // Performs a final update on the digest using the specified array of bytes, then completes the digest computation.
            byte[] hashedSecret = Arrays.copyOf(sha256.digest(secret), AES_KEY_SIZE / Byte.SIZE);
            // Set the secret key in the key-information
            // A secret (symmetric) key created using AES algorithm
            SecretKey secretKey = new SecretKeySpec(hashedSecret, "AES");
            currentKey.setSecretKey(secretKey);
            return secretKey;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static BigInteger generateRandomBigInteger() {
        return new BigInteger(31,new Random());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Destroy this Object.
     * Sensitive information associated with this Object is destroyed or cleared.
     * Subsequent calls to certain methods on this Object will result in an IllegalStateException being thrown.
     *
     * @throws IllegalStateException  if subsequent calls to certain methods on this Object are called
     * @throws DestroyFailedException signals that a destroy operation failed.
     *                                This exception is thrown by credentials implementing the
     *                                Destroyable interface when the destroy method fails.
     */
    @Override
    public void destroy() throws DestroyFailedException {
        Destroyable.super.destroy();
    }

    /**
     * Destroyable determine if this Object has been destroyed.
     * Overrides isDestroyed in interface Destroyable
     *
     * @return true if this Object has been destroyed, false otherwise.
     */
    @Override
    public boolean isDestroyed() {
        return Destroyable.super.isDestroyed();
    }

    public KeyInformation getCurrentKey() {
        return currentKey;
    }
}
