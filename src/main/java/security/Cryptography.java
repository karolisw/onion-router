package security;


import Interface.ISupportMethods;
import cells.Cell;
import nodes.Node;
import proxy.ProxyKeyStore;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Class contains methods for encoding/decoding onions
 * through usage of AES. The classes who implement this class are
 * clientProxy.ProxyServer and *.nodes
 */
public class Cryptography {

    /**
     * When creating an onion, this method is called.
     *
     * @param cell is the cell to encrypt
     * @param routers are all the routers involved in the circuit
     * @param amount is the amount of times the onion is to be encrypted
     * @param ascending true if sending from ClientProxy to nodes, false if sending from a node to ClientProxy
     */
    public void encryptSpecifiedNumberOfTimes(Cell cell, ArrayList <Node> routers, int amount, boolean ascending) {
        try{
            if(amount == 0 || amount > routers.size()) {
                throw new IllegalArgumentException("The amount of encryptions can not be 0 or greater " +
                                                   "than the amount of nodes in the circuit!");
            }
            // Sending from the client
            if(ascending) {
                for (int i = 0; i < amount; i++) {
                    // Get the secret key that corresponds to the correct router
                    SecretKey key = ProxyKeyStore.getKeyFromIpAddress(routers.get(i).getIpAddress());
                    encrypt(cell,key);
                }
            }
            // Sending from a node somewhere in the circuit
            if(!ascending) {
                // Ex: If we have 5 nodes, and amount = 3, this means we start
                // at node 3. Then we encrypt node 3, node 2, then node 1
                for(int i = amount ; i > 0; i--) {
                    // This way (i-1), we will actually get the guard node too!
                    SecretKey key = ProxyKeyStore.getKeyFromIpAddress(routers.get(i - 1).getIpAddress());
                    encrypt(cell,key);
                }
            }
        } catch (NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException
                | IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is only for use in the ClientProxy
     * Method decrypts until Cell is readable, and because the guard node always
     * is the last to encrypt, that is the key the client must use to decrypt first and so forth
     * until the message is readable
     *
     * @param cell is the cell that needs to be decrypted an unknown amount of times
     * @param circuit are all the routers that exist in the current circuit
     * @return the decrypted cell as a byte-array
     */
    public byte[] decryptionClientSide(Cell cell, ArrayList<Node> circuit) {
        try {
            for (Node node : circuit) {
                byte commandBit = ISupportMethods.getCommand(cell.getTotalMessage());
                // If the cell has a readable command bit we return it
                if(commandBit < 4 && commandBit >= 0) {
                    return cell.getTotalMessage();
                }
                // Get the secret key that matches the current node
                SecretKey currentKey = ProxyKeyStore.getKeyFromIpAddress(node.getIpAddress());

                // Decrypt the current layer --> starting at the guard node
                decrypt(cell.getTotalMessage(), currentKey);
            }
        } catch (NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }


//todo make private
    public void encrypt(Cell cell, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");


        // Creating the cipher in order to encrypt the cell
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        // doFinal() encrypts the cell
        //cipher.doFinal(cell.getTotalMessage()); //TODO THIS WAS ORIGINIALLY THERE

        cipher.doFinal(cell.getTotalMessage());
        /**

        byte[] encryptedCell = cipher.doFinal(cell.getTotalMessage());
        byte[] iv = cipher.getIV();
        byte[] cellWithIv = new byte[12 + cell.getTotalMessage().length + 16];
        System.arraycopy(iv, 0, cellWithIv, 0, 12);
        System.arraycopy(encryptedCell, 0, cellWithIv, 12, encryptedCell.length);

        // This enlarges the cell, but is necessary for AES to decrypt
        //cell = new byte[cellWithIv.length];
        //cell = Arrays.copyOfRange(cellWithIv,0,cellWithIv.length); // todo Does not work because the input is not a cell

        cell.setTotalMessage(cellWithIv); // <--- original text
         */
    }

    /**
     * Decrypts the current cell (onion)
     *
     * @param cell the cell can be any type of cell, and is encrypted an unknown amount of times
     * @param secretKey the symmetric key that matches the current layer
     */
    public byte[] decrypt(byte[] cell, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // Using ECB instead of GCM in order to stop using IV and therefore not have any padding!
        //Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");

        // GCMParameterSpec params = new GCMParameterSpec(128, cell, 0, 12);
        //cipher.init(Cipher.DECRYPT_MODE, secretKey, params);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        cipher.doFinal(cell);

        // Decrypting using the cipher
        //return cipher.doFinal(cell,12,cell.length - 12);
        return cell;
    }


}
