package Interface;

import nodes.DirectoryNode;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Random;

public interface ConvertInterface {


    /*********************************** Cell as byte-array methods **********************************************/

    static byte[] getStreamID(byte[] cell) {
        byte[] streamId = new byte[2];
        streamId[0] = cell[3];
        streamId[1] = cell[4];
        return streamId;
    }

    static byte[] getCircuitId(byte[] cell) {
        byte[] circuitId = new byte[2];
        circuitId[0] = cell[0];
        circuitId[1] = cell[1];
        return circuitId;
    }

    static byte getCommand(byte[] cell) {
        return cell[2];
    }

    static byte[] getPayloadLength(byte[] cell) {
        byte[] payloadLength = new byte[2];
        payloadLength[0] = cell[5];
        payloadLength[1] = cell[6];
        return payloadLength;
    }

    static byte[] getPayload(byte[] cell) {
        byte[] payload = new byte[504];

        for (int i = 7; i < 504; i++) {
            payload[i-7] = cell[i];
        }
        return payload;
    }

    static byte getRelayCommand(byte[] cell) throws Exception {
        byte relayCommand = cell[7];
        if(relayCommand == 0 || relayCommand > 5) {
            throw new Exception("The relay command could not be interpreted!");
        }
        else {
            return relayCommand;
        }
    }

    /*********************************** Public key from bytes to key **********************************************/

    static PublicKey convertToPublicKey(byte[] u) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Convert u back to BigInteger
        BigInteger curvePos = new BigInteger(u);
        // Convert the bytes to a PublicKey object
        NamedParameterSpec paramSpec = new NamedParameterSpec("X25519");
        KeyFactory kf = KeyFactory.getInstance("XDH");

        return kf.generatePublic(new XECPublicKeySpec(paramSpec, curvePos));
    }


    /*********************************** Other supporting methods **********************************************/

    static int byteToInt(byte[] bytes) {
        // putting the two bytes together
        byte mostSignificant = bytes[0];
        byte leastSignificant = bytes[1];

        // both bytes are converted to unsigned int
        return (Byte.toUnsignedInt(mostSignificant) << 8) | Byte.toUnsignedInt(leastSignificant);
    }

     static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    static byte[] generateRandomBytes() {
        // creating two random bytes
        Random randomizer = new Random();
        byte[] randomBytes = new byte[2];
        randomizer.nextBytes(randomBytes);

        return randomBytes;
    }

    static byte[] setRandomId() {
        // creating two random bytes
        Random randomizer = new Random();
        byte[] randomId = new byte[2];
        randomizer.nextBytes(randomId);

        // putting the two bytes together
        byte mostSignificant = randomId[0];
        byte leastSignificant = randomId[1];

        // both bytes are converted to unsigned int
        int bothBytes = (Byte.toUnsignedInt(mostSignificant) << 8) | Byte.toUnsignedInt(leastSignificant);

        // If we create a randomId that already exists, we must create a new one until we find one that is unique
        while (DirectoryNode.nodeIdExists(bothBytes)) {
            setRandomId(); // recursive
        }
        return randomId;

    }
}
