package cells;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.util.Arrays;

/**
 * The cell contains no Constructor because this superclass
 * should not be instantiated as there exists only two types of cell
 *
 * Size of header + payload always equals 512 bytes
 */
public class Cell {
    // Entire cell (header + payload)
    byte[] totalMessage = new byte[512];

    // Both cell-types' header contains
    byte[] circuitId; // <-- size = 2 bytes
    byte command; // <-- totalMessage = 1 byte; 0 = relay cell; 1-3 = command cell commands
    byte[] streamId;
    byte[] payloadLength;
    byte relayCommand;

    // The payload
    byte [] payload;

    public Cell() {
    }


    /**
     * This method MUST be handled differently depending on the type of Cell
     * This is because:
             * RelayCell.payloadLength == 504 bytes
             * ControlCell.payloadLength == 509 bytes
     *
     * @param payload is the body of the cell (data). Ex: GET request
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Cell{" +
                "totalMessage=" + Arrays.toString(totalMessage) +
                ", circuitId=" + Arrays.toString(circuitId) +
                ", command=" + command +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

    public void setCommand(byte command) {
        this.command = command;
    }

    public void setPayloadLength(byte[] payloadLength) throws Exception {
        this.payloadLength = payloadLength;
    }

    public void setRelayCommand(byte relayCommand) {
        this.relayCommand = relayCommand;
    }

    public void setStreamId(byte[] streamId) {
        this.streamId = streamId;
    }

    public byte[] getCircuitId() {
        return circuitId;
    }

    private Cell(int length, byte[] cell) {
        totalMessage = new byte[length];

        // Write the cell in bytes over to the actual cell
        for (int i = 0; i < length; i++) {
            totalMessage[i] = cell[i];
        }
    }

    /**
     * Can be properly made here, with no need for the subclasses to @override,
     * because both methods need to be able to set the circuit id inside
     * their own headers (at the same spot --> first two bytes)
     *
     * @param circuitId is a byte array that has to be the size of two bytes
     */
    public void setCircuitId(byte[] circuitId) throws IllegalArgumentException{
        if(circuitId.length == 2) {
            // We add the id to the message
            totalMessage[0] = circuitId[0];
            totalMessage[1] = circuitId[1];
            this.circuitId = circuitId;
        }
        else {
            throw new IllegalArgumentException("The circuit id is not 2 bytes long!");
        }
    }

    public byte getCommand() {
        return command;
    }

    /**
     * Used to decrypt/encrypt
     *
     * @return the whole encrypted/decrypted cell
     */
    public byte[] getTotalMessage() {
        return totalMessage;
    }

    private void setTotalMessageUsingByteArray(byte[] array) {

    }

    public static Cell makeByteCellToObjectCell(byte[] cell) {
        // Create a cell object with that size and transfer the cell onto that object

        return new Cell(cell.length, cell);

    }

    public byte[] createIv (Cell cell, Cipher cipher) {
        try {
            byte[] encryptedText = cipher.doFinal(cell.getTotalMessage());
            byte[] iv = cipher.getIV();
            byte[] message = new byte[12 + cell.totalMessage.length + 16];
            System.arraycopy(iv, 0, message, 0, 12);
            System.arraycopy(encryptedText, 0, message, 12, encryptedText.length);
            return message;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public void setTotalMessage(byte[] totalMessage) {
        this.totalMessage = totalMessage;
    }
}
