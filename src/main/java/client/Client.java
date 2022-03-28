package client;

import circuit.Circuit;
import mainServer.Server;
import nodes.DirectoryNode;
import nodes.Node;
import proxy.ClientProxy;


import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
    static private ArrayList<Node> chosenRouters;
    static private Circuit circuit;

    public static void main(String[] args) {
        // Welcome the user
        System.out.println("\nWelcome to the onion router! This router kan be used to fetch websites. \n" +
                "How many nodes would you like to use between yourself and the destination server?");

        // Give the user a question asking how many nodes they would like in their circuit
        Scanner input = new Scanner(System.in);

        try{
            // The user selects a number
            int amount = input.nextInt();

            // Instantiate the proxy
            Server server = new Server(8888);

            // The Directory node grants the user that number of nodes
            ArrayList<Node> nodes = DirectoryNode.setNodeSelection(amount);

            // The nodes are displayed to the user w/ address and port
            System.out.println("\nYour node selection consists of these nodes: ");
            for(Node node : nodes) {
                System.out.println(node.getIpAddress());
            }
            // sleep in order for the nodes to initialize their server sockets
            TimeUnit.SECONDS.sleep(2);
            // The circuit is created (the last element is the endNode)
            circuit = new Circuit(nodes);

            System.out.println("\nCreated circuit");

            // Connect the nodes and their sockets
            server.connectCircuit(circuit);

            System.out.println("\nConnected circuit");

            TimeUnit.SECONDS.sleep(6);


            // Handshake with the nodes
            server.fullCircuitHandshake(circuit);

            TimeUnit.SECONDS.sleep(3);


            // Make the server ready to relay data
            byte[] streamID = server.initiateRelay(circuit);

            // The server can now relay data, meaning that the client
            // must specify what web page they want to fetch
            System.out.println("All nodes connected, you may enter the page you wish to visit\n" +
                    "An example is 'vg.no'"
            );
            String request = input.nextLine();

            // The users request must now be forwarded throughout the circuit
            server.sendRequest(circuit,request, streamID);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    /**
     *
     * @param allNodes are all nodes registered in the DirectoryNode
     * @return All the nodes chosen to participate by the client
     *         The endNode is the last node in the array
     */
    public static ArrayList<Node> chooseRouters(ArrayList<Node> allNodes) {

        return null;
    }

    public static ArrayList<Node> getChosenRouters() {
        return chosenRouters;
    }


}
