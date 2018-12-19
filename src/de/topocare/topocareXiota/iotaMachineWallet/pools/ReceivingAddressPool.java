package de.topocare.topocareXiota.iotaMachineWallet.pools;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;
import jota.dto.response.GetBalancesResponse;

/**
 * A pool of IotaAddress(es), used to receive payments to the wallet.
 * The balance of IotaAddress(es) in this pool are updated each time .updateFromTangle() is called.
 * If addresses are used for transactions via PoolTransactions they will only update once returned with commitTake or rollbackTake.
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class ReceivingAddressPool implements TransactionInputSource {

	private List<IotaAddress> addressesAll;
	private List<IotaAddress> addressesWithBalance = new ArrayList<IotaAddress>();

	private long balanceAvailable = 0;
	private PoolMetaData outgoing = new PoolMetaData();

	/**
	 * Constructor. 
	 * @param keyIndexFirst The keyIndex of the first address to be included in the pool.
	 * @param keyIndexLast The keyIndex of the last address to be included in the pool.
	 */
	public ReceivingAddressPool(int keyIndexFirst, int keyIndexLast)
	{
		addressesAll = IotaAddress.newListOfUncheckedAddresses(keyIndexFirst, keyIndexLast);
	}
	
	/**
	 * Constructor, using existing List<IotaAddress>.
	 * @param addresses IotaAddresses to be included.
	 */
	public ReceivingAddressPool(List<IotaAddress> addresses)
	{
		this(addresses, 0, 0);
	}
	
	/**
	 * Constructor, using existing List<IotaAddress> and predefined starting outgoingBalance and outgoingAddressCount.
	 * Meant to be used to reload from persistence-layer (not yet implemented).
	 * 
	 * @param addresses IotaAddresses to be included.
	 * @param outgoingBalance Balance of PoolTransactions already using addresses of this pool.
	 * @param outgoingAddressCount AddressCount of PoolTransactions already using addresses of this pool.
	 */
	public ReceivingAddressPool(List<IotaAddress> addresses, long outgoingBalance, int outgoingAddressCount)
	{
		addressesAll = new ArrayList<IotaAddress>(addresses);
		outgoing = new PoolMetaData(outgoingBalance, outgoingAddressCount);
	}
	
	
	/** Updates the balance of all IotaAddresses in the pool with their confirmed values on the iota-tangle.
	 *  Addresses currently used in PoolTransactions are not part of the pool until returned (commit/rollback) and not updated.
	 *  
	 * @return <code>true</code> if balances of addresses where changes, <code>false</code> if not.
	 */
	public synchronized boolean updateFromTangle()
	{
		boolean changes = false;
		try {
			
			GetBalancesResponse balanceResponse = api.getBalances(100, IotaAddress.asStringList_getAddress(addressesAll));
			String tangleBalances[] = balanceResponse.getBalances();
			
			
			for(int i = 0; i<tangleBalances.length; i++)
			{
					IotaAddress addr = addressesAll.get(i);
					long balance = Long.parseLong(tangleBalances[i]);
					if (balance != addr.getBalance())
					{
						addr.setBalance(balance);
						changes = true;
					}
					
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		addressesWithBalance = addressesAll.stream().filter(e -> e.getBalance() > 0).collect(Collectors.<IotaAddress>toList());
		balanceAvailable = addressesWithBalance.stream().mapToLong(e -> e.getBalance()).sum();
		return changes;
	}
	



	//take iota for other pools
	

	/** Takes addresses from this pool, with a combined balance of at least the one requested.
	 * Because all balance on an address must be spend at once, the result might contain more IOTA token than expected.
	 * 
	 * @param balance How much balance should be taken into the PoolTransaction.
	 * @return a PoolTransaction of at least the requested balance, or null if not possible.
	 */
	public synchronized PoolTransaction takeBalance(long balance) {
		if (balanceAvailable >= balance) {
			List<IotaAddress> result = new ArrayList<IotaAddress>();
			long resultBalance = 0;

			for (ListIterator<IotaAddress> i = addressesWithBalance.listIterator(); i.hasNext();) {
				IotaAddress ia = i.next();
				if (ia.getBalance() > 0) {
					result.add(ia);
					resultBalance += ia.getBalance();
					if (resultBalance >= balance) {
						break;
					}
				}
			}
			return makePoolTransaction(result, resultBalance);
		} else
			return null;
	}

	/** Takes addresses from this pool, aiming for a combined balance of at least the one requested.
	 * Because all balance on an address must be spend at once, the result might contain more IOTA token than expected.
	 * if the pools balance is less then requested, the PoolTransaction will contain as much as possible.
	 * 
	 * @param balance How much balance should be taken into the PoolTransaction.
	 * @return PoolTransaction
	 */
	public synchronized PoolTransaction takeUpToBalance(long balance) {
		List<IotaAddress> result = new ArrayList<IotaAddress>();
		long resultBalance = 0;

		for (ListIterator<IotaAddress> i = addressesWithBalance.listIterator(); i.hasNext();) {
			IotaAddress ia = i.next();
			if (ia.getBalance() > 0) {
				result.add(ia);
				resultBalance += ia.getBalance();
				if (resultBalance >= balance) {
					break;
				}
			}
		}
		return makePoolTransaction(result, resultBalance);
	}

	/** Takes all addresses from the transaction pool.
	 * 
	 * @return A PoolTransaction with all content of the pool, or null if pool was empty.
	 */
	public synchronized PoolTransaction takeAll() {
		return makePoolTransaction(addressesWithBalance, balanceAvailable);
	}

	@Override
	/** Commits a PoolTransaction generated by one of this object's take-methods.
	 * Balance and addressCount of the PoolTransaction will be removed from the outgoing values.
	 * The addresses will be returned to the pool.
	 */
	public synchronized void commitTake(PoolTransaction poolTransaction) {
		undoReservation(poolTransaction);
	}

	@Override
	/** Rolls back a PoolTransaction generated by one of this object's take-methods.
	 * Balance and addressCount of the PoolTransaction will be removed from the outgoing values.
	 * The addresses will be returned to the pool.
	 */
	public synchronized void rollbackTake(PoolTransaction poolTransaction) {
		undoReservation(poolTransaction);
	}
	
	private void undoReservation(PoolTransaction poolTransaction)
	{
		outgoing.remove(poolTransaction);
		addressesAll.addAll(poolTransaction.iotaAddresses);
	}
	
	private PoolTransaction makePoolTransaction(List<IotaAddress> iotaAddresses, long balance) {

		addressesWithBalance.removeAll(iotaAddresses);
		balanceAvailable -= balance;

		return new PoolTransaction(iotaAddresses, balance, this, false);
	}

	public synchronized long getAvailableBalance()
	{
		return this.balanceAvailable;
	}
	public synchronized int getAvailableBalance_addresses()
	{
		return this.addressesWithBalance.size();
	}
	
	public synchronized long getOutgoingBalance()
	{
		return this.outgoing.getBalance();
	}
	public synchronized int getOutgoingAddressCount()
	{
		return this.outgoing.getAddressCount();
	}
}
