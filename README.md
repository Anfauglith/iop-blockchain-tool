# IoP-Blockchain-Tool

Adds and removes  public keys into the blockchain for the miner white list control.

### Usage

Execute .jar app with -h for help on the parameters
```
$ java -jar iop-blockchain-tool/out/artifacts/iop_blockchain_tool_jar/iop-blockchain-tool.jar -h
```


```
usage: IoP-Blockchain-Tool [-a <arg>] [-d] [-h] [-n <arg>] [-P <arg>] [-p
       <arg>] [-v]
 -a,--action <arg>    ADD or REM a public key to the blockchain
 -d,--debug           Print debugging information. Disabled by default.
 -h,--help            Print this message
 -n,--network <arg>   MAIN, TEST, REGTEST networks. Default is MAIN
 -p,--private <arg>   Master private key
 -m,--MinerAddress <args>    One or up to 20 Miner addresses to add in the transaction.
 -v,--version         Print the version information and exit

```

Mandatory arguments are:

* -Action
 *ADD*:  adds a public key into the miner white list.
  *REM*:  removes a public key from the miner white list.
  
  * -private: the **master private key** allowed to execute this transactions.
  
  * -MinerAddresses: a list of up to 20 addresses that will be included in the transaction. Depending on the action, the specified addresses will be added or removed from the miner White list database.
  
Default network is Mainnet. To switch, use the -network parameter. Example:

```
iop-blockchain-tool.jar -a add -p validPrivateKey -m uZE6SzrtYnWwr2zUg7RsvQeHKSRjSH6hKJ -n Test
```

## Program description

The goal is to generate and broadcast a valid transaction on the IoP blockchain including the passed **Action** and **Miner Addresses** into an *OP_RETURN* and P2PK  outputs for the IoP core client to process it.

The supplied **private key** is used to sign the transaction. The IoP core client detects transactions from this particular private key and based on the action specified, it will add or remove the addresses from the client's white list database.

When the Miner white list control is activated in the *IoP blockchain* only blocks which include coinbase transactions signed from any of the public keys on the miner white list will be allowed to incorporate into the blockchain.

Example of an execution output:

```
iop-blockchain-tool.jar -a add -p ValidPublicKey -p uiGRiGcjoGqbnUAV7jyrMxEcTfv9SewaBs uYjEfvjRS1PZQGxWCXJuLnhVUpqT2qoDab uZE6SzrtYnWwr2zUg7RsvQeHKSRjSH6hKJ ufhSEmrYJT7xnEasSWHbxBwD83rrQmdZib -n regtest
```

```
Connecting to IoP regtest network...
Connected to peer [127.0.0.1]:14877
Action: ADD
Miner addresses: 
uiGRiGcjoGqbnUAV7jyrMxEcTfv9SewaBs
uYjEfvjRS1PZQGxWCXJuLnhVUpqT2qoDab
uZE6SzrtYnWwr2zUg7RsvQeHKSRjSH6hKJ
ufhSEmrYJT7xnEasSWHbxBwD83rrQmdZib


Press ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.

Broadcasting transaction c800802016b8fe631d733ebbd3076f6e434c44def9a1637db31941eb2bab93e1 ...
Transaction broadcasted sucessfully


```

## Authors

* **Rodrigo Acosta** - *Initial work* - [acostarodrigo](https://github.com/acostarodrigo)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details