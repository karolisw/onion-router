package nodes;

import circuit.Circuit;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DirectoryNode {
    static List<Node> allNodes = new ArrayList<>();
    // Needed in order to check if a circuit id exists
    static ArrayList<Circuit> circuits = new ArrayList<>();

    public static List<Node> getAllNodes() {
        return allNodes;
    }

    /**
     * Remove the specified circuit from the list of circuits
     *
     * @param circuit is the circuit to remove
     */
    public static void removeCircuit(Circuit circuit) {
        circuits.remove(circuit);
    }

    public static boolean nodeIdExists(int id) {
        for (Node node : allNodes) {
            return node.getId() == (id);
        }
        return false;
    }

    public static boolean nodeWithIpExists(InetAddress ipAddress) {
        for (Node node : allNodes) {
            return node.getIpAddress().equals(ipAddress);
        }
        return false;
    }

    public static Node findNodeWithIpAddress(InetAddress ipAddress) throws Exception {
        for (Node node : allNodes) {
            if(node.getIpAddress().equals(ipAddress)){
                return node;
            }
        }
        throw new Exception("There was no node with the requested IP address in the directory.");
    }

    public static Circuit getCircuitWithId(int circuitId) throws Exception {
        for (Circuit circuit : circuits) {
            if(circuit.getId() == circuitId) {
                return circuit;
            }
        }
        throw new Exception("There was no circuit with the specified id");
    }

    public static boolean ipExistsInCircuit(int circuitId, InetAddress address) {
        try {
            Circuit circuit = getCircuitWithId(circuitId);
            for(InetAddress ip : circuit.getNodes()){
                if(ip.equals(address)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addCircuit(Circuit circuit) {
        circuits.add(circuit);
    }


    public static InetAddress findIpAddress(int id) throws Exception {
        for (Node node : allNodes) {
            if (node.getId() == (id)) {
                return node.getIpAddress();
            }
        }
        throw new Exception("There was no node with the given ID in the directory.");
    }

    /**
     * First, verify that the node exists
     *
     * @param id the id used to search for the node
     * @return the node if it exists
     */
    public static Node getNode(int id) throws Exception {
        if(nodeIdExists(id)) {
            for(Node node: allNodes) {
                if(Objects.equals(node.getId(), id)) { //<-- Null-safe way to ask for equal id
                    return node;
                }
            }
        }
        throw new Exception("No node with that ID present in the directory.");
    }




    public boolean isOnline(int id) {
        try {
            Node node = getNode(id);
            return node.getOnline();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addNode(Node node) {
        allNodes.add(node);
    }
}
