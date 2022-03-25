package circuit;

import nodes.DirectoryNode;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Circuit {
    List<InetAddress> nodes = new ArrayList<>();
    int circuitId; // circuitId.size = 2 bytes --> int is big enough to hold 2^16

    public Circuit() { // todo add the previous and next node to all the nodes
        setRandomId();
    }

    /**
     * Every circuit should have their own private randomized id! This id must not exist in the DirectoryNode
     */
    private void setRandomId() {
        // creating two random bytes
        Random randomizer = new Random();
        byte[] array = new byte[2];
        randomizer.nextBytes(array);
        System.out.println(Arrays.toString(array));

        // putting the two bytes together
        byte mostSignificant = array[0];
        byte leastSignificant = array[1];

        // both bytes are converted to unsigned int
        int bothBytes = (Byte.toUnsignedInt(mostSignificant) << 8) | Byte.toUnsignedInt(leastSignificant);

        // If we create a randomId that already exists, we must create a new one until we find one that is unique
        while (DirectoryNode.nodeIdExists(bothBytes)) {
            setRandomId(); // recursive
        }

        //If we get here, the id creates is unique, and we can assign it to circuitId
        circuitId = bothBytes;

        // Now the bits one needs can be extracted through bitwise operations (such as xor or &) later on
        System.out.println("bothBytes: " + bothBytes);

        // if converted to binary and calculated to int from there, the result will be the same as in bothBytes
        String binary = toBinary(array);
        System.out.println("binary: " + Integer.parseInt(binary, 2)); // <-- radix: number system base (binary = 2)
    }

    String toBinary( byte[] bytes ) { // https://stackoverflow.com/questions/11528898/convert-byte-to-binary-in-java
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }


    public void addNode(InetAddress node) {
        nodes.add(node);
    }

    public int getId() {
        return circuitId;
    }

    public List<InetAddress> getNodes() {
        return nodes;
    }

    public InetAddress getExitNode() throws Exception{
        if(nodes.size() > 0){
            return nodes.get(nodes.size() - 1);
        }
        else {
            throw new Exception("The circuit is empty!");
        }
    }

    public int getLength() throws Exception {
        if(nodes.size() > 0){
            return nodes.size();
        }
        throw new Exception("The circuit is empty!");
    }

    public static void main(String[] args) {
        Circuit circuit = new Circuit();
        circuit.setRandomId();
    }
}
