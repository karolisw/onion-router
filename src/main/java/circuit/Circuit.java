package circuit;

import mainServer.Server;
import nodes.DirectoryNode;
import nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

public class Circuit {
    ArrayList<Node> nodes = new ArrayList<Node>();
    byte[] circuitId = new byte[2];// circuitId.size = 2 bytes --> int is big enough to hold 2^16
    boolean relayingMessage = false;


    /**
     * For usability, this method combines relayBegin (relay command #1) todo...
     * relay extend (relay command #2) and relay extended (relay command #3)
     */
    public Circuit (ArrayList<Node> routers){
        try {
            // Assigning this circuit an id
            setRandomId();

            for (int i = 0; i < routers.size(); i++) {
                Node currentNode = routers.get(i);

                // If first node, set the server as the previous node
                if (i == 0) {
                    currentNode.setPreviousNode(Server.getSocketAddress());
                    currentNode.setNextNode(routers.get(i+1).getServerSocket().getLocalSocketAddress());

                    // Also, set the queue!
                    currentNode.setQueuePrevNode(Server.getBlockingQueue());
                    currentNode.setQueueNextNode(new ArrayBlockingQueue<>(1024));
                }

                // If last node, there is no need to set next node
                else if (i == routers.size() - 1) {
                    currentNode.setPreviousNode(routers.get(i-1).getServerSocket().getLocalSocketAddress());

                    // Adding the node to the circuit
                    addNode(currentNode);

                    // Set the queue to the previous node
                    currentNode.setQueuePrevNode(routers.get(i-1).getQueueNextNode());
                    // Nodes should open up their connection to their
                    //currentNode.
                }

                // If intermediate node, both previous -and next node must be set
                else {
                    currentNode.setPreviousNode(routers.get(i-1).getServerSocket().getLocalSocketAddress());
                    currentNode.setNextNode(routers.get(i+1).getServerSocket().getLocalSocketAddress());

                    // Adding the node to the circuit
                    addNode(currentNode);

                    // Set the queue!
                    currentNode.setQueuePrevNode(routers.get(i-1).getQueueNextNode());
                    currentNode.setQueueNextNode(new ArrayBlockingQueue<>(1024));
                }
            }
            // Add the circuit to the lists of all circuits
            DirectoryNode.addCircuit(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        circuitId = array;

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


    public void addNode(Node node) {
        nodes.add(node);
    }

    public byte[] getId() {
        return circuitId;
    }



    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public Node getExitNode() throws Exception{
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

    public void setRelayingMessage(boolean relayingMessage) {
        this.relayingMessage = relayingMessage;
    }
}
