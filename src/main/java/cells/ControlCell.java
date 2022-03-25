package cells;

import java.nio.ByteBuffer;


public class ControlCell extends Cell{
    // To make insertion easier, the byte message is wrapped in a ByteBuffer
    ByteBuffer byteBuffer = ByteBuffer.wrap(totalMessage);

    /**
     * Shell of a control cell (the payload bytes are empty until altered using class methods)
     */
    public ControlCell(byte command, byte [] circuitId) {
        try {
            setCommand(command);
            setCircuitId(circuitId);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * The payload should reside in the bytes following the 3 first bytes (offset = byte #4)
     * @param payload is the body of the cell (data). Ex: the IP address of the node that received the cell
     */
    @Override
    public void setPayload(byte[] payload) throws IllegalArgumentException{
        // Payload can be maximum 504 bytes
        if(payload.length <= 509) {
            super.setPayload(payload);
            byteBuffer.position(3); //if overflow --> try position 8

            // Put the payload there
            byteBuffer.put(payload);
        }
        else {
            throw new IllegalArgumentException("Payload can not be more than 504 bytes long");
        }
    }
    /**
     * For a control cell, the command byte has to be an integer 1-3
     *                1 = create -> set up a new circuit
     *                2 = created -> circuit creation is finished
     *                3 = destroy -> destroy the circuit
     */
    public void setCommand(byte command) {
        if(command == 0) {
            throw new IllegalArgumentException("Command cannot be 0 in a command cell");
        }
        if(command > 3) {
            throw new IllegalArgumentException("Command cannot be greater than 3");
        }
        else {
            byteBuffer.position(2).put(command);
        }
    }

    @Override
    public byte getCommand() {
        return byteBuffer.get(2);
    }

    public byte[] getPayload() {
        byte[] payload = new byte[509];
        byteBuffer.position(3);
        byteBuffer.get(payload,0,509);
        return payload;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public byte[] getTotalMessage() {
        return super.getTotalMessage();
    }
}
