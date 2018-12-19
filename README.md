# iota-pay-on-production

Pay-on-production (PoP) model:
In PoP model a a manufacturer gives a machine to an operator basically 
without a  leasing or renting fee nor the machine will be sold. 
All payments are done directly through the use of the machine. 
Reasons for this model from a client’s perspective are a lower upfront 
capital investment and that the people who actually developed and produced 
the machine will keep an eye on maintenance and equipment optimization.

We realized the PoP model on a prototypical demonstration level.
All results are open source (see licence).

-> The cryptocurency IOTA is used to transfer values per production step achieved.
-> Also sensor data is stored into the IOTA tangle

The following webside gives a first overview:
https://medium.com/topocare-x-iota

The repositiory contains the java project which is described in the article. 

First step:
wallet_template.properties: Is a template config  file. 
1) copy file, rename the copy to wallet.properties
2) Insert an IOTA seed (machines wallet)
3) Insert an outputAddress, where the IOTA token shall be received when a production step is achieved
4) Insert a returnAddress, where unspend token are send in return 
