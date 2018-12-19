package de.topocare.topocareXiota.iotaMachineWallet.pools;

import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;



/**
 * A pool with addresses used as input in TangleTransactions.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public interface TransactionInputSource {
	//public PoolTransaction takeBalance(int balance);
	//public PoolTransaction takeUpToBalance(int balance);
	//public PoolTransaction takeAddresses(int Addresses);
	public PoolTransaction takeAll();
	
	public void commitTake(PoolTransaction poolTransaction);
	public void rollbackTake(PoolTransaction poolTransaction);

}
