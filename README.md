# onion-router (Java)


This project contains a self made interpretation of onion routing, with influences from the Tor Project. Onion routing consists of routers (servers/nodes), 
and the onions they send. 
Each onion router (OR) runs as a normal user-level process without any special privileges. Each onion router maintains a connection to their neighboring 
routers. The program fetches nodes, establish circuits across the network, and handle connections specified by the user. The first onion router of the circuit is called
a guard node. The guard node is the only node who communicates with the main server. The main server is the class that handles client communication. 
The onion routers in the middle of the circuit only connect to their neighbors. The last onion router - on the other side of the circuit (end-node) - connects to the 
requested destinations, fetches data, and relays it back. The data fetched is the onion. 



### The onion (cell)
The onion is the data structure formed by "wrapping" a message with layers of encryption, before being sent of to be decrypted ("peeled"). 
The onion is decrypted by as many intermediary computers (servers/nodes) as there are layers before arriving at its destination. The original message remains hidden 
as it is transferred from one node to the next, and no intermediary knows both the origin and destination of the data, allowing the sender to remain anonymous. This is the
logic behind onion routing. 
The data (message) that is wrapped in this encryption, is a Cell. A cell could be either a relay cell or a control cell, but is always 512 bytes long. 

##### Control Cell
A control cell is used to :
- Establish connections (through sockets) across the circuit -> create-cells, which are sent from the main server to the nodes in the circuit.
- For the nodes to send back that a connection is established -> created-cells, which are sent from the nodes in the circuit to the main server
- To destroy connections -> destroy-cells, which are sent from the main server to the nodes in the circuit when it is time to shut down the program
     * 0x1 = create -> set up a new circuit
     * 0x2 = created -> circuit creation is finished
     * 0x3 = destroy -> destroy the circuit

##### Relay cells
Relay cells are not used until connection is fully established across the circuit, and is used for:
- Beginning relays. This is when the server sends a relay-begin cell to the nodes of the circuit, effectively starting a stream. A stream is when a user/client requests a webpage, and the end node serves it up back through the circuit in a stream of 512 byt long cells (with 504 bytes of payload).
- Connecting the relays. This is when the end-node received a relay-begin cell (which was encrypted throghout the circuit until the end node received it), and the end node states that it is ready to start receiving relay-data cells. The relay-connected cell is sent from the end-node to the main server.
- Extending relays. This is when the server sends a relay-extend cell to the nodes of the circuit. The circuit is then extended. This implementation has not yet been implemented. 
- Upon extending relays, the nodes in the circuit respond with a relay extended cell upon successfull extension. 
- Relaying data. This is used by both the main server and the end-node when relaying data. Data is always a request for a webpage from the server, and the cell-stream of bytes from the end-node. This means that all webpages fetched by the end node are sent using relay-data cells.
- Stream closing. When the end-node '
     * 0x1 = relayBegin
     * 0x2 = relayExtend
     * 0x3 = relayExtended
     * 0x4 = relayData
     * 0x5 = relayConnected
     * 0x6 = streamClosing

The cells look like this, except that relay cells in this program do not have a digest (hash):

![image](https://user-images.githubusercontent.com/71627370/160353377-fefa5a64-dad2-4d8f-b078-769a122e1284.png)




### Crypthography 
When connection is established through the interchange of control cells (create and created cells) between each individual node in the circuit and the main server,
what actually happens is this:
- The main server generates private key and a public key and puts tbe public key in the payload of a a create cell, which it sends to the first node. 
- The guard node receives the create cell, generates its own private/public key pair, and uses the public sent from the main server along with its own private key to generate a symmetric key. 
- The guard node sends its own public key back to the main server in a created-cell. 
- The main server receives the public key and uses its own private key to create a symmetric key. This secret key is what the two will use to encrypt/decrypt each others cells (onions).
- The main server keeps sending create cells to the guard node, but because the guard node already has a symmetric key, it knows to pass the cell on to its "next" node. This happens along the circuit until all nodes share a secret key with the main server.


The key generation is performed through the use of a Diffie - Hellman elliptic curve function called X25519. 
The encryption and decryption is through the use of AES. 



### Protecting the end points - proxies
The weakest point for man-in the middle attacks are the points where the main server connects to the browser, and the end node connect to a remote server. A way to not  have this happen is to use SSL, or to use proxies. 
In this project proxies were used, but sadly the proxy-classes have not yet been implemented in the final solution, even if they were ready.



### Threads
Threads are implemented in their own folder. Threads come in two shapes of reader threads, and one type of writer threads. 
Reader threads read cells and make decisions on wheter to encrypt, 
decrypt or pass them on; whilst writer threads write to their assigned socket conenction when their BlockingQueue has cells in it.

#### The thread implementation looks like this. 
The main server: Shares a blocking queue with the guard node (node 1)
Node 1         : Shares a blocking queue with the main server and the next node (node 2)
Node 2         : Shares a blocking queue with the previous node (node 1) and the next node (node 3)
  .
  .
  .
End-node       : Shares a blocking queue with the previous node ( node (circuit.size() - 1) )

An example: Node 1 has two reader threads, where one reads on the blocking queue to the previous node (the main server), 
whilst the other one reads on the blocking queue of the next node (node 2). 
The reader thread connected to the main servers queue reads a create cell, handles this (makes a secret key), and passes the created-cell
with the public key to the writer thread connected to the blocking queue connected to the main server. 

Had node 1 already had a symmetric key, then the create-cell would have been passed along through the use of the writer thread on the socket
(blocking queue towards the next node), such that the next node's reader thread could have handled it instead. 



### Testing
The cells, the node-class and the crypthography class are successfully tested and functional.



### The short-comings of the project
- The proxy server endpoints are created, but not implemented in the solution. They would have been desirable as they would have added an extra layer of security as browsers can read the host of who sent the GET-request. Using proxy servers, the host is abstracted away to become the proxy servers IP-address.
- The circuit creation is complete, and so are the connections between the servers. The handshake using create/created cells is not yet functional, deeming it impossible to start the relay.
- Threads are implemented, in the form of writer and reader threads, but are not properly assigned to their correct BlockingQueues yet. This causes the cells to be sent in an undesired direction, causing loops and exception-casting. 
- All methods for cell-creation needed to run the project are made, but the logic of the threads are not yet properly implemented in a way that they can be used.
- It would have been desirable to keep a real keystore. The keystore that is currently implemented is a java class with both accessor and mutator methods. The original plan was to keep SSL- tunnels between the endpoints and their browser/remote server, and between the nodes themnselves. This would have decreased the chance of a man in the middle attack by a lot. Aditionally, an IV was used for encryption and decryption, but had to be removed due to issues on the last day that could not be solved with the time that was left. It would have been favourable to keep the IV, as well as to maintain a digest as part of the cell structure in order to further maximize the chance of privacy. 
- Condition variables and locks would have made control of receiving/sending cells much more controllable. 
- A key to maintain the TLS sertificate needed to properly tunnel the SSL connections. 
- Lack of CI/CD.
- Methods to periodically switch out the keys created, further complicating attacks on the solution.
- A logic multiple clients can use. This is not possible yet as the program is not fully functional. The keystore is also needed for this. 
- The low cohesion of some classes due to the keystore implementation, as well as how the cryptography class is instantiated as an object in almost all classes for usage of its methods, instead of just having been implemented either as a static import or an interface. 
- Due to the nature of the project, the servers all run on localhost, and so do the proxies.



### Future work
- Correctly implement the thread safe BlockingQueue in such a way that the onions are sent in the correct direction.
- Implementing the proxy servers. 
- Fully writing the relay extend/extended methods and implement them. This would be good for aiding in multiplexing as well as for further confusion for any attacket (the switching out and in of new relay nodes in a randomized pattern).
- Implementing SSL tunnels, SSL keys and sertificates. This will aid in authentication/verification.
- Making many of the classes more thread dependent (in a synchronized way), such that multiple clients can use the program. This has been a goal, but was not reached during the time-limit. 
- A checksum for the cell sent, such that a man in the middle attack can be discovered. Upon such an attack new secret keys have to be implemented. 
- Switching out the keys used for encryption in such a way that security has a lower risk of being breached. An example would be to delete the secret keys, and send new create/created cells through the SSL-tunnel every set time-limit (ex. every 10th minute)
- Using real servers!



### External dependencies
Using Maven and pom.xml, the dependencies are:
- maven-surefire-plugin, version : 2.22.2
- junit-jupiter, version: 5.8.2

Both dependencies are used for running JUnit tests. 



### Installation instructions
- Java 17 is used in this project. 
- Maven is used.



### Running the program
The program is not runnable. To run, the only class that must be compiled is the Client class, as it is the only class with a main method.
Upon running the user will be prompted to mention how many nodes are desired in their circuit. The circuit is created with that amount of nodes.
A connection is then established between the nodes. 
After that, a handshake between the main server and nodes is attempted (create/created cells), but fails due to the lack of precision in when the BlockinQueue objects were dealt to the individual threads.
Exceptions will flare up, proving that the project at least is handling exceptions well, such that errors have stack traces and are easier to discover.



### Running tests
Tests can be run through clicking the run button on the sidebar of the test files, or by writing "mvn test" in the command line. Maven must be installed with the mentioned dependencies. 

### API - Documentation
A library containing the API documentation is uploaded, and contains all the files necessary to view the full documentation. It is recommended to either view the classes' documentation, or to open the file "allclasses-index" and browse the classes by using the search bar. 

