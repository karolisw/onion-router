package nodes;

import cells.Cell;
import cells.ControlCell;
import cells.RelayCell;
import org.junit.jupiter.api.Test;
import security.Cryptography;
import security.KeyGeneration;
import security.KeyInformation;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    // RelayCell info
    byte relayCommand = 1;
    byte[] payloadLength = createRandomBytes(2);
    byte[] streamID = createRandomBytes(2);
    byte[] circuitID = createRandomBytes(2);
    byte[] payload = createRandomBytes(504);
    // Creating the RelayCell for testing
    Cell cell = new RelayCell(relayCommand,streamID,payloadLength,circuitID);

    // CommandCell info
    byte command = 2;
    // Creating key-generation class instance
    KeyGeneration keyGeneration1 = new KeyGeneration();
    KeyGeneration keyGeneration2 = new KeyGeneration();


    // Creating crypto class instance
    Cryptography cryptography = new Cryptography();

    // Node 1
    Key publicKey1;
    Key privateKey1;
    InetAddress address1;

    // Node 2
    Key publicKey2;
    Key privateKey2;
    InetAddress address2;

    /**
     * Method needed in order to effectively conduct tests where byte arrays of a certain size are needed
     *
     * @param size is a measure of how many bytes to be returned
     * @return a byte array containing the specified amount of random bytes
     */
    public byte[] createRandomBytes(int size) {
        Random randomizer = new Random();
        byte[] bytes = new byte[size];
        randomizer.nextBytes(bytes);
        return bytes;
    }

    @Test
    public void nodeCreatedSuccessfully() {
        Node node = new Node();
    }

    @Test
    public void privateKeyCreatedIs48BytesTest() {
        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the keys
            keyGeneration1.generateKeyPair(circuitID, address1 );

            // Checking the length
            assertEquals(48, keyGeneration1.getCurrentKey().getPrivateKey().getEncoded().length);

        } catch (UnknownHostException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void publicKeyCreatedIs44BytesTest() {
        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the keys
            keyGeneration1.generateKeyPair(circuitID, address1 );

            // Checking the length
            assertEquals(44, keyGeneration1.getCurrentKey().getLocalPublicKey().getEncoded().length);

        } catch (UnknownHostException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sharedKeyCreatedIs16BytesTest() {
        // make sure the keys are created
        //todo it is worth a try to set firstphase == true on both of the tries because someone did it on stack
        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Simulating creating keys at another node
            keyGeneration2.generateKeyPair(circuitID, address2);

            // Getting the key information for both of the nodes
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            KeyInformation information2 = keyGeneration2.getCurrentKey();

            // Generating the key at the foreign node first (simulating that they got the create-cell)
            keyGeneration2.generateSecretKey(information1.getLocalPublicKey(), true);
            keyGeneration1.generateSecretKey(information2.getLocalPublicKey(), true);

            // The keys should be 512 bytes long
            assertEquals(16, keyGeneration1.getCurrentKey().getSecretKey().getEncoded().length);
            assertEquals(16, keyGeneration2.getCurrentKey().getSecretKey().getEncoded().length);
        } catch (UnknownHostException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sharedKeysAreEqualTest() {
        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Simulating creating keys at another node
            keyGeneration2.generateKeyPair(circuitID, address2);

            // Getting the key information for both of the nodes
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            KeyInformation information2 = keyGeneration2.getCurrentKey();

            // Generating the key at the foreign node first (simulating that they got the create-cell)
            keyGeneration2.generateSecretKey(information1.getLocalPublicKey(), true);
            keyGeneration1.generateSecretKey(information2.getLocalPublicKey(), true);
        } catch (InvalidAlgorithmParameterException | UnknownHostException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        // The symmetric keys are already created
        SecretKey key1 = keyGeneration1.getCurrentKey().getSecretKey();
        SecretKey key2 = keyGeneration2.getCurrentKey().getSecretKey();

        // Test the keys to see if they are actually equal
        assertEquals(Arrays.toString(key1.getEncoded()), Arrays.toString(key2.getEncoded()));
    }


    @Test
    public void isSizeOfEncryptedCell512() {
        // Simulating a circuit
        ArrayList<InetAddress> nodes = new ArrayList<>();
        nodes.add(address1);
        nodes.add(address2);
        try{
            Cell originalCell = cell;
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Simulating creating keys at another node
            keyGeneration2.generateKeyPair(circuitID, address2);

            // Getting the key information for both of the nodes
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            KeyInformation information2 = keyGeneration2.getCurrentKey();

            // Generating the key at the foreign node first (simulating that they got the create-cell)
            keyGeneration2.generateSecretKey(information1.getLocalPublicKey(), true);
            keyGeneration1.generateSecretKey(information2.getLocalPublicKey(), true);
        } catch (InvalidAlgorithmParameterException | UnknownHostException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        // The symmetric keys are already created
        SecretKey key1 = keyGeneration1.getCurrentKey().getSecretKey();
        SecretKey key2 = keyGeneration2.getCurrentKey().getSecretKey();

        // Encrypting the cell
        cell.setPayload(payload);
        Cell originalCell = cell;
        try {
            cryptography.encrypt(cell, key1);
            cryptography.encrypt(cell,key2);
            assertEquals(Arrays.toString(originalCell.getTotalMessage()), Arrays.toString(cell.getTotalMessage()));
            assertEquals(512, cell.getTotalMessage().length);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void encodingWorks() {
        // Simulating a circuit
        ArrayList<InetAddress> nodes = new ArrayList<>();
        nodes.add(address1);
        nodes.add(address2);

        // Setting the payload for the cell
        cell.setPayload(payload);


        // Creating a reference to the original cell
        Cell originalCell = cell;

        try {
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Simulating creating keys at another node
            keyGeneration2.generateKeyPair(circuitID, address2);

            // Getting the key information for both of the nodes
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            KeyInformation information2 = keyGeneration2.getCurrentKey();

            // Generating the key at the foreign node first (simulating that they got the create-cell)
            keyGeneration2.generateSecretKey(information1.getLocalPublicKey(), true);
            keyGeneration1.generateSecretKey(information2.getLocalPublicKey(), true);
        } catch (InvalidAlgorithmParameterException | UnknownHostException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        // The symmetric keys are already created
        SecretKey key1 = keyGeneration1.getCurrentKey().getSecretKey();
        SecretKey key2 = keyGeneration2.getCurrentKey().getSecretKey();

        try {
            // Encrypting the cell
            cryptography.encrypt(cell, key1);
            cryptography.encrypt(cell, key2);

            assertNotEquals(originalCell.getTotalMessage().toString(), Arrays.toString(cell.getTotalMessage()));

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }
        @Test
    public void decodingWorks() {
        // Simulating a circuit
        ArrayList<InetAddress> nodes = new ArrayList<>();
        nodes.add(address1);
        nodes.add(address2);

        // Setting the payload for the cell
        cell.setPayload(payload);


        // Creating a reference to the original cell
        Cell originalCell = cell;

        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Simulating creating keys at another node
            keyGeneration2.generateKeyPair(circuitID, address2);

            // Getting the key information for both of the nodes
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            KeyInformation information2 = keyGeneration2.getCurrentKey();

            // Generating the key at the foreign node first (simulating that they got the create-cell)
            keyGeneration2.generateSecretKey(information1.getLocalPublicKey(), true);
            keyGeneration1.generateSecretKey(information2.getLocalPublicKey(), true);
        } catch (InvalidAlgorithmParameterException | UnknownHostException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        // The symmetric keys are already created
        SecretKey key1 = keyGeneration1.getCurrentKey().getSecretKey();
        SecretKey key2 = keyGeneration2.getCurrentKey().getSecretKey();

        try {
            // Encrypting the cell
            cryptography.encrypt(cell, key1);
            cryptography.encrypt(cell,key2);

            // Decrypting the cell
            cryptography.decrypt(cell.getTotalMessage(),key2);
            cryptography.decrypt(cell.getTotalMessage(), key1);

            // originalCell and cell (decryptedTwice) should be equal
            assertEquals(Arrays.toString(cell.getTotalMessage()), Arrays.toString(originalCell.getTotalMessage()));
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cellPayloadTestAfterPublicKeyAdded() {
        try{
            address1 = InetAddress.getLocalHost();
            address2 = InetAddress.getLocalHost();

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Getting the key information for the node
            KeyInformation information1 = keyGeneration1.getCurrentKey();
            ControlCell cell = new ControlCell(command, circuitID);

            cell.setPayload(information1.getLocalPublicKey().getEncoded());

            // Check that the cell is still 512 bytes
            assertEquals(512, cell.getTotalMessage().length);
        } catch (InvalidAlgorithmParameterException | UnknownHostException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void keyRevertedWorks() {
        try{
            // New cell alert!
            ControlCell newCell = new ControlCell(command,circuitID);

            // Generating the local keys
            keyGeneration1.generateKeyPair(circuitID, address1);

            // Getting the key information for the node
            KeyInformation information3 = keyGeneration1.getCurrentKey();
            PublicKey thePublicKey = information3.getLocalPublicKey();

            // Getting u
            BigInteger u = information3.getU();
            byte[] uToByteArray = u.toByteArray();

            // Getting the public key
            byte[] publicKey = information3.getLocalPublicKey().getEncoded();

            // Setting the first 4 bytes of the payload
            byte[] payload2 = new byte[20];
            for (int i = 0; i < 4; i++) { //
                payload2[i] = uToByteArray[i];
            }

            // Setting the next 16 bytes of the payload
            for (int i = 4; i < 20; i++) {
                payload2[i] = publicKey[i-3];
            }

            // Setting the payload of the cell
            newCell.setPayload(payload2);

            // Now that we have the cell, we test if it is possible to revert the public key
            byte[] trying = newCell.getTotalMessage();

            // Get u from the payload (resides in the first 4 bytes)
            byte[] uByte = new byte[4];

            for (int i = 3; i < 7 ; i++) {
                uByte[i-3] = trying[i];
            }

            // Convert u back to BigInteger
            BigInteger u2 = new BigInteger(uByte);

            // Convert the bytes to a PublicKey object
            NamedParameterSpec paramSpec = new NamedParameterSpec("X25519");
            KeyFactory kf = KeyFactory.getInstance("XDH");
            PublicKey regeneratedPublicKey = kf.generatePublic(new XECPublicKeySpec(paramSpec, u2 ));

            assertEquals(Arrays.toString(uByte), Arrays.toString(uToByteArray));
            // Check if BigIntegers are equal
            assertEquals(u2, u);

            // Check if keys are equal
            assertEquals(Arrays.toString(regeneratedPublicKey.getEncoded()),
                    Arrays.toString(thePublicKey.getEncoded()));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }
}
