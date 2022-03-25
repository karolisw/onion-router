package cells;

import java.nio.ByteBuffer;

public class RelayCell extends Cell{
    // Only headers of relay cells contain


    ByteBuffer byteBuffer = ByteBuffer.wrap(totalMessage);


    public RelayCell(byte relayCommand, byte[] streamID, byte[] payloadLength, byte[] circuitId) {
        try{
            setStreamId(streamID);
            setPayloadLength(payloadLength);
            setRelayCommand(relayCommand);
            setCircuitId(circuitId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Command is always 0 in relay cells
        setCommand((byte)0);
    }

    /**
     * Payload length must be 2 bytes long and has an offset in the cell of 5 (position = 5)
     *
     * @param payloadLength is how many bytes long the payload is
     * @throws Exception is the payloadLength's length is more than 2 bytes long
     */
    public void setPayloadLength(byte[] payloadLength) throws Exception {
        if(payloadLength.length == 2) {
            byteBuffer.position(5).put(payloadLength);
        }
        else {
            throw new Exception("Payload length was not 2 bytes, but " + payloadLength.length + " bytes.");
        }
    }

    /**
     * Writes the payload to the totalMessage
     * The payload offset is 8 (position = 8)
     *
     * @param payload is the body of the cell (data). Ex: GET request
     */
    @Override
    public void setPayload(byte[] payload) {
        // Payload can be maximum 504 bytes
        if(payload.length <= 504) {
            super.setPayload(payload);
            byteBuffer.position(7);

            // Put the payload there
            byteBuffer.put(payload);
        }
        else {
            throw new IllegalArgumentException("Payload can not be more than 504 bytes long");
        }
    }

    /**
     * The stream ID must be generated when the first cell of the stream is created
     */
    public void setStreamId(byte[] streamID) throws IllegalArgumentException {
        // stream id must be 2 bytes long and should be placed at offset 4
        if(streamID.length == 2) {
            byteBuffer.position(3);
            byteBuffer.put(streamID);
        }
        else {
            throw new IllegalArgumentException("Stream id must be 2 bytes long, not " + streamID.length + " bytes long");
        }
    }

    /**
     * Puts the command (0) into the byte buffer that is the Cell
     * @param command is a byte where only 0 is acceptable for this type of cell
     *
     */
    public void setCommand(byte command) {
        // If the relayCommand == 0 (which it should)
        if(0 == command) {
            this.command = command;
            byteBuffer.position(2).put(command);
        }
        else{
            throw new IllegalArgumentException("Relay cell must have a command header == 0");
        }
    }

    /**
     * @param relayCommand is the final header-element in front of the payload
     *                     and can contain values 1-5, where:
     *                     0x1 = relayBegin
     *                     0x2 = relayExtend
     *                     0x3 = relayExtended
     *                     0x4 = relayData
     *                     0x5 = relayConnected
     *                     0x6 = streamClosing
     */
    public void setRelayCommand(byte relayCommand) {
        if(relayCommand > 6) {
            throw new IllegalArgumentException("The relay command must be a byte representing an integer value 1-5");
        }
        else{
            // The value may be inserted into the message
            // Interpretation of the relay-commands happen in the client proxy server (ProxyServer)
            byteBuffer.position(7).put(relayCommand);
        }
    }

    /**
     * The stream id has position 4 in the cell
     *
     * @return a new array with the streamId
     */
    public byte[] getStreamID() {
        byte[] streamId = new byte[2];
        byteBuffer.position(3);
        byteBuffer.get(streamId,0,2);
        return streamId;
    }

    /**
     * The relay command resides at totalMessage[7]
     *
     * @returns the relay command
     * @throws Exception – If index is negative or not smaller than the buffer's limit,
                           or if the relay command was not correct
     */
    public byte getRelayCommand() throws Exception {
        byte relayCommand = byteBuffer.get(7);
        if(relayCommand == 0 || relayCommand > 5) {
            throw new Exception("The relay command could not be interpreted!");
        }
        else {
            return relayCommand;
        }
    }

    /**
     * Positioned at bytes with pos 6-7
     *
     * @return int with the size of the payload in bytes
     */
    public int getPayloadLength() {
        byte[] payloadLength = new byte[2];
        byteBuffer.position(5);

        byteBuffer.get(payloadLength,0,2);

        // putting the two bytes together
        byte mostSignificant = payloadLength[0];
        byte leastSignificant = payloadLength[1];

        // both bytes are converted to unsigned int
        return (Byte.toUnsignedInt(mostSignificant) << 8) | Byte.toUnsignedInt(leastSignificant);
    }

    /**
     * Positioned at bytes 9-512
     *
     * @return the payload in bytes
     */
    public byte[] getPayload() {
        byte[] payload = new byte[504];
        byteBuffer.position(7);
        byteBuffer.get(payload,0,504);
        return payload;
    }

    @Override
    public byte[] getTotalMessage() {
        return super.getTotalMessage();
    }
}
