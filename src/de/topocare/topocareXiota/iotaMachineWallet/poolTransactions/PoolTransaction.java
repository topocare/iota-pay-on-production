package de.topocare.topocareXiota.iotaMachineWallet.poolTransactions;

import java.util.ArrayList;
import java.util.List;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.address.IotaFreeAddressFactory;
import de.topocare.topocareXiota.iotaMachineWallet.pools.IotaAddressPool;
import de.topocare.topocareXiota.iotaMachineWallet.pools.TransactionInputSource;
import jota.model.Input;
import jota.model.Transfer;

/**
 * A collection of IotaAddress(es) containing Iota.
 * Taken from a IotaAddressPool/ReceivingPool or planned to be given to it. Used by TangleTransaction to make the transaction on the tangle and commit the data on the pools after confirmation.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class PoolTransaction implements PoolTransactionInput, PoolTransactionTransfer {
	public List<IotaAddress> iotaAddresses;
	public long balance;
	public int addressCount;
	private TransactionInputSource source;
	private IotaAddressPool target;
	private boolean managed;
	

	/** Constructor, based on existing list of IotaAddresses. Used by the take-methods of pools.
	 * 
	 * @param iotaAddresses The IotaAddresses to be included in the PoolTransaction.
	 * @param balance The total balance of the PoolTransaction, sum of the balance of all included IotaAddresses.
	 * @param source The TransactionInputSource (pool) where the transaction will be committed/rolled back.
	 * @param managed true if the addresses can be used to confirm transactions on the tangle (only known/used by the wallet), else false
	 */
	public PoolTransaction(List<IotaAddress> iotaAddresses, long balance, TransactionInputSource source, boolean managed) {
		this.iotaAddresses = new ArrayList<IotaAddress>(iotaAddresses);
		
		this.balance = balance;
		this.source = source;
		this.managed = managed;
		
		addressCount = iotaAddresses.size();
	}
	
	/** Constructor, creating a new pool of addresses. Used by the give-methods of pools.
	 * 
	 * @param iotaFreeAddressFactory The factory used to create the new Addresses.
	 * @param addressCount The amount of unused addresses to be included.
	 * @param balancePerAddress The balance of each created address.
	 * @param target The IotaAddressPool (pool) where the transaction will be committed/rolled back.
	 * @param managed true if the addresses can be used to confirm transactions on the tangle (only known/used by the wallet), else false
	 */
	public PoolTransaction(IotaFreeAddressFactory iotaFreeAddressFactory, int addressCount, long balancePerAddress, IotaAddressPool target, boolean managed)
	{
		this.iotaAddresses = iotaFreeAddressFactory.getNextFreeAddresses(addressCount);
		iotaAddresses.forEach(e -> e.setBalance(balancePerAddress));
		this.balance = addressCount * balancePerAddress;
		this.target = target;
		this.managed = managed;
		this.addressCount = addressCount;
	}
	

	/**
	 * Commits this PoolTransaction on its pool.
	 */
	@Override
	public void commit()
	{
		if (source != null)
			source.commitTake(this);
		
		if (target != null)
			target.commitGive(this);
	}
	

	/**
	 * Rolls back this PoolTransaction on its pool.
	 */
	@Override
	public void rollback()
	{
		if (source != null)
			source.rollbackTake(this);
		
		if (target != null)
			target.rollbackGive(this);
	}

	/**
	 * Provides a list with jota.model.Transfer objects to be used in
	 * TangleTransaction, one for each IotaAddress in this PoolTransaction.
	 * message and tag are the same for all objects in the list.
	 *  
	 * @param message Transaction-Message as clear test, will be converted to
	 *                trytes.
	 * @param tag     Transaction-Tag as clear test, will be converted to trytes.
	 */
	@Override
	public List<Transfer> getAsJotaTransfer(String message, String tag) {
		List<Transfer> result = new ArrayList<Transfer>(addressCount);
		iotaAddresses.forEach(e -> result.add(e.getAsJotaTransfer(message, tag)));
		return result;
	}
	
	
	/**
	 * Provides a list with jota.model.Input objects to be used in
	 * TangleTransaction, one for each IotaAddress in this PoolTransaction.
	 * 
	 */
	@Override
	public List<Input> getAsJotaInputs() {
		List<Input> result = new ArrayList<Input>(addressCount);
		iotaAddresses.forEach(e -> result.add(e.getAsJotaInput()));
		return result;
	}
	
	
	/**
	 * Returns a IotaAddress that can be used to confirm transactions on the tangle.
	 * If the addresses in this PoolTransaction are not valid for confirmation (as defined as constructor-parameter) return null.
	 */
	@Override
	public IotaAddress getManagedAddress()
	{
		if (managed && addressCount > 0)
			return iotaAddresses.get(0);
		else
			return null;
	}
	
}

