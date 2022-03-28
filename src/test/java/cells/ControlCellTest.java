package cells;

import Interface.ICellMethods;
import Interface.ISupportMethods;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ControlCellTest {
    byte failingCommand = 0;
    byte command = 1;
    byte[] circuitId = createRandomBytes(2);
    byte[] payload = createRandomBytes(509);



    //For testing purposes, a RelayCell object is made
    ControlCell cell = new ControlCell(command,circuitId);


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
    public void ControlCellSuccessfulTest() {
        ControlCell cell = new ControlCell(command,circuitId);
    }

    @Test
    public void ControlCellTestFail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ControlCell cell = new ControlCell(command,circuitId);
            cell.setCommand(failingCommand);
        });

        String expectedMessage = "Command cannot be 0 in a command cell";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void getCommandTest() {
        cell.setCommand(command);
        assertEquals(command, cell.getCommand());
    }

    @Test
    public void bytesToCellWorks() {
        cell.setPayload(payload);
        cell.setCommand(command);
        byte[] bytes = cell.getTotalMessage();

        assertEquals(Cell.bytesToCellObject(bytes).getTotalMessage().length, cell.getTotalMessage().length);
        assertEquals(ISupportMethods.getCommand(Cell.bytesToCellObject(bytes).getTotalMessage()),
                ISupportMethods.getCommand(cell.getTotalMessage()));
    }

    @Test
    public void setPayloadTest() {
        cell.setPayload(payload);
    }

    @Test
    public void getPayloadTest() {
        cell.setPayload(payload);
        assertEquals(Arrays.toString(payload), Arrays.toString(cell.getPayload()));
    }

}
