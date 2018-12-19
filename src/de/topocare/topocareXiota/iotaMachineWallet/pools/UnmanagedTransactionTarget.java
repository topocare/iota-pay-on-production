package de.topocare.topocareXiota.iotaMachineWallet.pools;

import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransactionUnmanagedOutput;


/**
 * A target for TangleTransactions on a foreign seed, given only as 90-trytes address-string.
 * <p>
 * Is used as a transaction target in TangleTransactions by providing a PoolTransactionUnmanagedOutput instead of a PoolTransaction.
 * The PoolTransactionUnmanagedOutput is returned by the give method, and must be committed after the TangleTransactions.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class UnmanagedTransactionTarget {

	protected String address;
	private long incomingBalance=0;
	
	protected PoolMetaData outstandingTransactions = new PoolMetaData();
	
	public UnmanagedTransactionTarget(String address) {
		this.address = address;
	}

	
	/** 
	 * Returns a PoolTransactionUnmanagedOutput with the given balance and the address of the UnmanagedTransactionTarget.
	 * Also adds the balance to the incomingBalance until it is committed.
	 * 
	 * @param balance
	 * @return
	 */
	public synchronized PoolTransactionUnmanagedOutput give(long balance) {
		this.incomingBalance+=balance;
		return new PoolTransactionUnmanagedOutput(this, balance, address);
	}


	/** 
	 * Commits a PoolTransactionUnmanagedOutput provided previously by the give-method and removes the balance from the incomingBalance.
	 */
	public synchronized void commit(PoolTransactionUnmanagedOutput poolTransactionUnmanagedOutput) {
		incomingBalance -= poolTransactionUnmanagedOutput.getBalance();

	}
	
	/**
	 * The balance send to this address by transactions yet to be confirmed.
	 */
	public synchronized long getIncomingBalance()
	{
		return incomingBalance;
	}


}
