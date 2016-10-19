package org.fermat.blockchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermatj.core.*;
import org.fermatj.script.Script;
import org.fermatj.script.ScriptBuilder;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

/**
 * Created by rodrigo on 8/30/16.
 */
public class MinerWhiteListTransaction {
    private final String privateKey;
    private final List<Address> minerAddresses;
    private final Action action;

    private FermatNetwork fermatNetwork;
    private Transaction transaction;

    private final Logger logger = Main.logger;

    //Action Enum
    public enum Action{
        ADD , REM, ENABLE_CAP, DISABLE_CAP
    }

    // constructor
    public MinerWhiteListTransaction(String privateKey, Action action, List<Address> minerAddresses) {
        //preconditions check
        Preconditions.checkArgument(!privateKey.isEmpty());
        Preconditions.checkNotNull(action);
        //Preconditions.checkNotNull(minerAddresses);

        if (logger.getLevel() != Level.DEBUG)
            logger.setLevel(Level.OFF);

        this.privateKey = privateKey;
        this.action = action;
        this.minerAddresses= minerAddresses;
    }

    /**
     * construct the OP_Return data to include in the transaction
     * @return
     */
    private String getOP_Return (){
        int factor = Main.factor;
        if (factor == 0)
            factor = 2;

        String data;
        switch (this.action){
            case ADD:
                data =  "add";
                break;
            case REM:
                data =  "rem";
                break;
            case ENABLE_CAP:
                data = "enable_cap:" + factor; // we are including the factor of the cap
                break;
            case DISABLE_CAP:
                data = "disable_cap";
                break;
            default:
                data =  "add";
                break;
        }

        return data;
    }

    /**
     * Builds the transaction to be sent.
     * @return
     * @throws CantConnectToFermatBlockchainException
     * @throws InsufficientMoneyException
     * @throws TransactionErrorException
     * @throws AddressFormatException
     */
    public Transaction build() throws CantConnectToFermatBlockchainException, InsufficientMoneyException, TransactionErrorException, AddressFormatException {
        // start the network with the passed private key
        fermatNetwork = new FermatNetwork(this.privateKey);
        fermatNetwork.initialize();

        // import the private key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(FermatNetwork.NETWORK, this.privateKey);
        ECKey key = dumpedPrivateKey.getKey();

        /**
         * We must have positive confirmed balance in order to generate the transaction.
         */
        Wallet wallet = fermatNetwork.getFermatWallet();
        if (wallet.getBalance(Wallet.BalanceType.AVAILABLE).isZero())
            throw new CantConnectToFermatBlockchainException("Wallet balance is zero");

        transaction = new Transaction(Main.networkParameters);
        Wallet.SendRequest sendRequest = Wallet.SendRequest.forTx(transaction);
        sendRequest.shuffleOutputs = false; // can't allow shuffle because op_Return must be first output

        // we are returning any change to the same address
        sendRequest.changeAddress = key.toAddress(FermatNetwork.NETWORK);


        // add the public key into the op_Return output.
        Script op_return = ScriptBuilder.createOpReturnScript(getOP_Return().getBytes());
        TransactionOutput op_returnOutput = new TransactionOutput(FermatNetwork.NETWORK, transaction, Coin.ZERO, op_return.getProgram());
        transaction.addOutput(op_returnOutput);

        // Add all the miners addresses into new outputs.
        if (minerAddresses != null){
            for (Address minerAddress : minerAddresses){
                transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, minerAddress);
            }
        }
        // we complete the transaction
        wallet.completeTx(sendRequest);

        return transaction;
    }

    /**
     * broadcast the generated transaction
     * @throws TransactionErrorException
     */
    public void broadcast() throws TransactionErrorException {
        Preconditions.checkNotNull(this.transaction);
        fermatNetwork.broadcast(this.transaction);
    }

    /**
     * broadcast the passed transaction
     * @param transaction
     * @throws TransactionErrorException
     */
    public void broadcast(Transaction transaction) throws TransactionErrorException {
        Preconditions.checkNotNull(transaction);
        fermatNetwork.broadcast(transaction);
    }
}
