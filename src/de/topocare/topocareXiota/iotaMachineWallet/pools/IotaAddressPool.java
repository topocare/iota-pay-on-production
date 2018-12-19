package de.topocare.topocareXiota.iotaMachineWallet.pools;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.productionUnitSize;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.address.IotaFreeAddressFactory;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;




/** 
 * A pool of IotaAddress(es), representing addresses used in a common role.
 * The pool is used as a local representation of the addresses/balances on the IOTA tangle, and their planned role in the iotaMachineWallet. The pool has no direct interaction with the tangle.
 * <p>
 * Adding or removing addresses (and thus their balance) is handled via PoolTransaction objects.
 * This is a two-step process. 
 * The first step defines what should be done (giving to/taking from the pool) and a PoolTransaction will be returned, containing all the needed data for this part of a transaction on the tangle.
 * After a transaction on the tangle is done, each related PoolTransaction needs to be committed (or rolled back) to enact the changes on the pools.
 * Until the PoolTransaction is committed/rolled back its balance and addressCount are part of the incoming/outgoing/expected values.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */ 
public class IotaAddressPool implements TransactionInputSource {

	protected List<IotaAddress> poolList = new ArrayList<IotaAddress>();
	protected PoolMetaData pool = new PoolMetaData();
	protected PoolMetaData incoming = new PoolMetaData();
	protected PoolMetaData outgoing = new PoolMetaData();
	protected IotaFreeAddressFactory iotaFreeAddressFactory;

	/** Initializes a PoolTransaction to give a specific balance to the pool.
	 *  All the balance will be expected on one specific unused IotaAddress, given in the transaction.
	 * @param balance how much will be added to the pool?
	 * @return A PoolTransaction to be used in a TangleTransaction and committed after its done.
	 */
	public synchronized PoolTransaction giveBalance(long balance) {
		PoolTransaction result = new PoolTransaction(iotaFreeAddressFactory, 1, balance, this, true);
		incoming.add(result);
		return result;
	}
	
	/** Initializes a PoolTransaction to give a specific number of production units to the pool.
	 *  Each production unit will be stored on it's own address and have a value specified in the IotaConfig.
	 * @param balance how much will be added to the pool?
	 * @return A PoolTransaction to be used in a TangleTransaction and committed after its done.
	 */
	public synchronized PoolTransaction giveUnits(int amountInUnits) {
		PoolTransaction result = new PoolTransaction(iotaFreeAddressFactory, amountInUnits, productionUnitSize, this, true);
		incoming.add(result);
		return result;
	}

	/**
	 * Constructor.
	 * @param iotaFreeAddressFactory The IotaFreeAddressFactory used to get unused addresses for incoming(give) PoolTransactions.
	 */
	public IotaAddressPool(IotaFreeAddressFactory iotaFreeAddressFactory) {
		this.iotaFreeAddressFactory = iotaFreeAddressFactory;
	}
	
	/**
	 * Constructor, with a starting list of contained IotaAddress objects, but no active transactions.
	 *
	 * @param iotaFreeAddressFactory The IotaFreeAddressFactory used to get unused addresses for incoming(give) PoolTransactions.
	 * @param addressesInPool List of IotaAddress-objects, that are part of this pool.
	 */
	public IotaAddressPool(IotaFreeAddressFactory iotaFreeAddressFactory, List<IotaAddress> addressesInPool)
	{
		this(iotaFreeAddressFactory, addressesInPool, 0,0,0,0);
	}
	
	/**
	 * Constructor, with a starting list of contained IotaAddress objects and balances/addressCounts of active transactions.
	 * 
	 * @param iotaFreeAddressFactory The IotaFreeAddressFactory used to get unused addresses for incoming(give) PoolTransactions.
	 * @param addressesInPool List of IotaAddress-objects, that are part of this pool.
	 * @param incomingBalance Balance of all addresses expected to be given to this pool, summed up.
	 * @param incomingAddresses Address count of all addresses expected to be given to this pool, summed up.
	 * @param outgoingBalance Balance of all addresses used in active transactions, summed up.
	 * @param outgoingAddresses Address count of all addresses used in active transactions, summed up.
	 */
	public IotaAddressPool(IotaFreeAddressFactory iotaFreeAddressFactory, List<IotaAddress> addressesInPool, long incomingBalance, int incomingAddresses, int outgoingBalance, int outgoingAddresses)
	{
		this.iotaFreeAddressFactory = iotaFreeAddressFactory;
		this.poolList = new ArrayList<IotaAddress>(addressesInPool);
		this.pool = new PoolMetaData(poolList.stream().mapToLong(e -> e.getBalance()).sum(), poolList.size());
		this.incoming = new PoolMetaData(incomingBalance, incomingAddresses);
		this.outgoing = new PoolMetaData(outgoingBalance, outgoingAddresses);
	}



	/** Commits a PoolTransaction which was generated by giveBalance or giveUnits to this Pool.
	 * Balance and addressCount will be removed from the ncoming values, the addresses will be added to the pool.
	 * 
	 * @param poolTransaction The PoolTransaction to commit.
	 */
	public synchronized void commitGive(PoolTransaction poolTransaction) {
		incoming.remove(poolTransaction);

		// add to pool
		poolList.addAll(poolTransaction.iotaAddresses);
		pool.add(poolTransaction);
	}

	/** Roll back a PoolTransaction which was generated by by one of the take-methods of this Object.
	 * Balance and addressCount will be removed from the incoming values, the addresses and their (expected) balances will not be added to the pool.
	 * 
	 * @param poolTransaction The PoolTransaction to roll back.
	 */
	public synchronized void rollbackGive(PoolTransaction poolTransaction) {
		incoming.remove(poolTransaction);
	}

	// TransactionInputSource implementation
	
	
	@Override
	/** Commits a PoolTransaction generated by one of this object's take-methods.
	 * Balance and addressCount of the PoolTransaction will be removed from the outgoing values.
	 * The addresses will be dropped as used and not returned to the pool.
	 */
	public synchronized void commitTake(PoolTransaction poolTransaction) {
		outgoing.remove(poolTransaction);
	}

	@Override
	/** Rools back a PoolTransaction generated by one of this object's take-methods.
	 * Balance and addressCount of the PoolTransaction will be removed from the outgoing values.
	 * The addresses will be returned to the pool.
	 */
	public synchronized void rollbackTake(PoolTransaction poolTransaction) {
		outgoing.remove(poolTransaction);

		poolList.addAll(poolTransaction.iotaAddresses);
		incoming.add(poolTransaction);
	}

	// taking money out of the pool

	/** Takes addresses from this pool, with a combined balance of at least the one requested.
	 * Because all balance on an address must be spend at once, the result might contain more IOTA token than expected.
	 * 
	 * @param balance How much balance should be taken into the PoolTransaction.
	 * @return a PoolTransaction of at least the requested balance, or null if not possible.
	 */
	public synchronized PoolTransaction takeBalance(long balance) {
		return takeBalanceIntern(balance, false);
	}

	/** Takes addresses from this pool, aiming for a combined balance of at least the one requested.
	 * Because all balance on an address must be spend at once, the result might contain more IOTA token than expected.
	 * if the pools balance is less then requested, the PoolTransaction will contain as much as possible.
	 * 
	 * @param balance How much balance should be taken into the PoolTransaction.
	 * @return PoolTransaction
	 */
	public synchronized PoolTransaction takeUpToBalance(long balance) {
		return takeBalanceIntern(balance, true);
	}

	/** Takes a number of addresses from the pool, only useful if pool is used for production units
	 * @param addressCount
	 * @return
	 */
	public synchronized PoolTransaction takeElements(int addressCount) {
		if (pool.getAddressCount() >= addressCount) {
			return take(poolList.subList(0, addressCount));
		} else
			return null;
	}

	/** Takes all addresses from the transaction pool.
	 * 
	 * @return A PoolTransaction with all content of the pool, or null if pool was empty.
	 */
	public synchronized PoolTransaction takeAll() {
		if (pool.getAddressCount() > 0)
			return take(poolList, pool.getBalance());
		else
			return null;
	}
	
	protected PoolTransaction takeBalanceIntern(long balance, boolean returnAvailableIfLower) {
		if (pool.getBalance() > balance) {
			List<IotaAddress> result = new ArrayList<IotaAddress>();
			long resultBalance = 0;
			boolean balanceReached = false;

			for (ListIterator<IotaAddress> i = poolList.listIterator(); i.hasNext();) {
				IotaAddress ia = i.next();
				result.add(ia);
				resultBalance += ia.getBalance();
				if (resultBalance >= balance) {
					balanceReached = true;
					break;
				}
			}
			if (balanceReached) {
				poolList.removeAll(result);

				return take(result, resultBalance);
			}
		} else if (pool.getBalance() == balance || returnAvailableIfLower) {
			return takeAll();
		}
		return null;
	}

	protected PoolTransaction take(List<IotaAddress> list) {
		return take(list, list.stream().mapToLong(e -> e.getBalance()).sum());
	}

	protected PoolTransaction take(List<IotaAddress> list, long balance) {

		PoolTransaction poolTransaction = new PoolTransaction(list, balance, this, true);
		outgoing.add(poolTransaction);

		poolList.removeAll(poolTransaction.iotaAddresses);
		pool.remove(poolTransaction);

		return poolTransaction;
	}
	
	public synchronized long getBalance()
	{
		return pool.getBalance();
	}
	public synchronized int getAddressCount()
	{
		return pool.getAddressCount();
	}
	
	public synchronized long getExpectedBalance()
	{
		return pool.getBalance() + incoming.getBalance();
	}
	public synchronized int getExpectedAddressCount()
	{
		return pool.getAddressCount() + incoming.getAddressCount();
	}
	
	public synchronized long getIncomingBalance()
	{
		return incoming.getBalance();
	}
	public synchronized int getIncomingAddresscount()
	{
		return incoming.getAddressCount();
	}
	
	public synchronized long getOutgoingBalance()
	{
		return outgoing.getBalance();
	}
	public synchronized int getOutgoingAddressCount()
	{
		return outgoing.getAddressCount();
	}

}
