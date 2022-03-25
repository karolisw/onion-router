package cells;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelayCellTest {

    byte relayCommand = 1;
    byte[] payloadLength = createRandomBytes(2);
    byte[] streamID = createRandomBytes(2);
    byte[] circuitID = createRandomBytes(2);
    byte[] payload = createRandomBytes(504);

    /**
     * For testing purposes, a RelayCell object is made
     */
    RelayCell cell = new RelayCell(relayCommand,streamID,payloadLength, circuitID);


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
    public void constructorTest() {
        assertEquals(2,streamID.length);
        assertEquals(2,payloadLength.length);
        RelayCell cell = new RelayCell(relayCommand,streamID,payloadLength, circuitID);
    }



    @Test
    public void getStreamIDTest() {
        byte[] newStreamID = createRandomBytes(2);
        cell.setStreamId(newStreamID);
        assertEquals(Arrays.toString(newStreamID), Arrays.toString(cell.getStreamID()));
    }

    @Test
    public void getRelayCommandTest() {
        try {
            assertEquals(1, cell.getRelayCommand());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getPayloadLengthTest() {
        // putting the two bytes together
        byte mostSignificant = payloadLength[0];
        byte leastSignificant = payloadLength[1];

        // both bytes are converted to unsigned int
        int bothBytes = (Byte.toUnsignedInt(mostSignificant) << 8) | Byte.toUnsignedInt(leastSignificant);
        assertEquals(bothBytes, cell.getPayloadLength());
    }

    @Test
    public void setPayloadTest(){
        cell.setPayload(payload);
    }
    @Test
    public void getPayloadTest() {
        cell.setPayload(payload);
        assertEquals(Arrays.toString(payload), Arrays.toString(cell.getPayload()));
    }

}
