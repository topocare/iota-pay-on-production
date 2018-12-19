package de.topocare.topocareXiota.iotaMachineWallet.pools;

import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;

/**
 * Stores balance and addressCount of a pool (different instances used for current, incoming or outgoing).
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
class PoolMetaData {
	private long balance = 0;
	private int addressCount = 0;
	
	public PoolMetaData()
	{
		
	}
	
	public PoolMetaData(long balance, int addressCount)
	{
		this.balance = balance;
		this.addressCount = addressCount;
	}

	/**
	 * adds the balance(sum) and addressCount of a PoolTransaction to those of this object.
	 * @param poolTransaction
	 */
	public void add(PoolTransaction poolTransaction) {
		balance += poolTransaction.balance;
		addressCount += poolTransaction.addressCount;
	}

	/**
	 * Subtracts the balance(sum) and addressCount of a PoolTransaction from those of this object.
	 * @param poolTransaction
	 */
	public void remove(PoolTransaction poolTransaction) {
		balance -= poolTransaction.balance;
		addressCount -= poolTransaction.addressCount;
	}

	public long getBalance() {
		return balance;
	}

	public int getAddressCount() {
		return addressCount;
	}

}
