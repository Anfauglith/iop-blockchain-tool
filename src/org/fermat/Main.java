package org.fermat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.apache.commons.cli.*;
import org.fermat.blockchain.MinerWhiteListTransaction;
import org.fermat.blockchain.TransactionSummary;
import org.fermatj.core.*;
import org.fermatj.params.MainNetParams;
import org.fermatj.params.RegTestParams;
import org.fermatj.params.TestNet3Params;

import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.Console;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;


public class Main {
    // class variables
    private static Options options;
    private static MinerWhiteListTransaction.Action action;
    private static String masterPrivKey;
    private static String[] minerStrAddresses;
    private static List<Address> minerAddresses;
    public static NetworkParameters networkParameters;
    public static Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    public static int factor;


    public static void main(String[] args)  {
        // create Options object
        options = addCommandLineOptions();

        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine commandLineline = parser.parse(options, args);

            //show only the version
            if (commandLineline.hasOption("v")){
                showVersion();
                System.exit(0);
            }

            // show only the help
            if (commandLineline.hasOption("h")){
                showHelp();
                System.exit(0);
            }

            // define debugging mode.
            if (commandLineline.hasOption("d"))
                logger.setLevel(Level.DEBUG);
            else
                //sets default logging level to ERROR
                logger.setLevel(Level.ERROR);


            //assign the rest of the mandatory arguments
            defineArguments(commandLineline);

        }
        catch( Exception e) {
            // oops, something went wrong
            System.err.println(e.getMessage());
            showHelp();
            System.exit(-1);
        }

        // make sure the private key is valid
        if (!isMasterPrivateKeyValid(masterPrivKey)){
            // oops, something went wrong
            System.err.println("Master private key is not valid on network " + networkParameters.getPaymentProtocolId() + ".");
            System.exit(-1);

        }

        // make sure the miner addresses are valid
        if (minerStrAddresses != null){
            for (String minerAddress : minerStrAddresses){
                if (!isMinerAddressValid(minerAddress)){
                    // oops, something went wrong
                    System.err.println("Miner's address " + minerAddress + " is not valid on network " + networkParameters.getPaymentProtocolId() + ".");
                    System.exit(-1);

                }
            }
        }



        try{
            /**
             * generates the transaction
             */
            MinerWhiteListTransaction generator = new MinerWhiteListTransaction(masterPrivKey, action, minerAddresses);
            Transaction transaction = generator.build();

            // shows the summary and waits for confirmation
            showSummary(transaction);

            waitForEnter();
            generator.broadcast();
            System.exit(0);
        } catch (Exception exception){
            System.err.println(exception.getMessage());
            System.exit(-1);
        }
    }

    private static boolean isMinerAddressValid(String minerAddress) {
        Preconditions.checkNotNull(minerAddress);

        Address address = null;
        try {
            address = new Address(networkParameters, minerAddress);
        } catch (AddressFormatException e) {
            return false;
        }

        //I will add this address into minerAddresses class
        if (minerAddresses == null)
            minerAddresses = new ArrayList<>();

        minerAddresses.add(address);
        return true;
    }

    /**
     * validate the private key is valid.
     * @param masterPrivKey the hex string of the private key
     * @return true if valid.
     */
    private static boolean isMasterPrivateKeyValid(String masterPrivKey) {
        try {
            DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, masterPrivKey);
            ECKey privateKey = dumpedPrivateKey.getKey();
            if (privateKey.isPubKeyOnly())
                return false;
        } catch (AddressFormatException e) {
            return false;
        }
        return true;
    }


    /**
     * shows on screen the summary of the output transaction
     * @param transaction
     */
    private static void showSummary(Transaction transaction) {
        TransactionSummary summary = new TransactionSummary(transaction);
        summary.showSummary();
    }


    private static void defineArguments(CommandLine commandLineline) {
        // make sure required parameters have data.
        if (!commandLineline.hasOption("p") || !commandLineline.hasOption("a")){
            String output = "Required parameter missing.\n Private Key [p] and Action [a] are required parameters.";
            throw new RuntimeException(output);
        }
        //master private key
        masterPrivKey = commandLineline.getOptionValue("p");

        
        // action
        switch (commandLineline.getOptionValue("a").toLowerCase()){
            case "add":
                action = MinerWhiteListTransaction.Action.ADD;
                break;
            case "rem":
                action = MinerWhiteListTransaction.Action.REM;
                break;
            case "enable_cap":
                action = MinerWhiteListTransaction.Action.ENABLE_CAP;
                break;
            case "disable_cap":
                action = MinerWhiteListTransaction.Action.DISABLE_CAP;
                break;
            default:
                throw new InvalidParameterException(commandLineline.getOptionValue("a") + " is not a valid action parameter.");
        }

        // miner's public key
        minerStrAddresses = commandLineline.getOptionValues("m");
        // make sure no address provided if we are enabling or disabled miner cap
        if (action == MinerWhiteListTransaction.Action.DISABLE_CAP || action == MinerWhiteListTransaction.Action.ENABLE_CAP){
            if (minerStrAddresses != null)
                throw new RuntimeException("Can't provide miner addresses when enabling or disabling cap.");
        } else {
            if (minerStrAddresses == null)
                throw new RuntimeException("Miner's addresses not provided.");
        }

        // get the factor if provided
        try{
            if (commandLineline.hasOption("f"))
                factor = Integer.parseInt(commandLineline.getOptionValue("f"));
        } catch (Exception e){
            throw new RuntimeException("Factor provided not parsable into int.");
        }



        // define the network, if any. RegTest by default.
        if (commandLineline.hasOption("n")) {
            switch (commandLineline.getOptionValue("n").toUpperCase()){
                case "MAIN":
                    networkParameters = MainNetParams.get();
                    break;
                case "TEST":
                    networkParameters = TestNet3Params.get();
                    break;
                case "REGTEST":
                    networkParameters = RegTestParams.get();
                    break;
                default:
                    throw new InvalidParameterException(commandLineline.getOptionValue("n") + " is not a valid parameter for Network.");
            }
        } else
            networkParameters = MainNetParams.get();

    }

    private static void showVersion(){
        System.out.println("IoP-Blockchain-Tool version 1.0");
    }

    /**
     * Setups all possible command arguments-
     * @return the Options with each configured option available
     */
    private static Options addCommandLineOptions (){
        Options options = new Options();

        // add Action option
        Option action = new Option("a", "action", true, "ADD, REM, ENABLE_CAP or DISABLE_CAP.");
        action.setOptionalArg(false);
        options.addOption(action);

        // add private key option
        Option privateKey = new Option("p", "private", true, "Master private key");
        privateKey.setOptionalArg(false);
        options.addOption(privateKey);

        // add public key option
        Option minerAddresses = new Option("m", "minerAddresses", true, "Miner's addresses to include into whitelist. Max 20 addresses");
        minerAddresses.setOptionalArg(true);
        minerAddresses.setArgs(20);
        options.addOption(minerAddresses);

        // add network option
        Option network = new Option("n", "network", true, "MAIN, TEST, REGTEST networks. Default is MAIN");
        network.setOptionalArg(false);
        options.addOption(network);

        // add network option
        Option factor = new Option("f", "factor", true, "Multiplying factor for minerCap limit when used with enable_cap action. Default is 2.");
        network.setOptionalArg(false);
        options.addOption(factor);


        //add version option
        Option version = new Option("v", "version", false, "Print the version information and exit" );
        version.setRequired(false);
        options.addOption(version);

        //add debug option
        Option debug = new Option( "d", "debug", false, "Print debugging information. Disabled by default." );
        debug.setRequired(false);
        options.addOption(debug);

        // add help option
        Option help = new Option( "h", "help", false, "Print this message" );
        help.setRequired(false);
        options.addOption(help);

        return options;
    }

    /**
     * shows the commands help
     */
    private static void showHelp(){
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("IoP-Blockchain-Tool", options, true);
    }


    public static void waitForEnter() {
        Console c = System.console();
        if (c != null) {
            // printf-like arguments
            c.format("\nPress ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.\n");
            c.readLine();
        }
    }
}
