package de.topocare.topocareXiota.iotaMachineWallet.poolTransactions;

import java.util.Collections;
import java.util.List;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.pools.UnmanagedTransactionTarget;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

/**
 * Output part of a transaction to an address an a foreign seed. Used for
 * communication between TangleTransactions and UnmanagedTransactionTargets.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class PoolTransactionUnmanagedOutput implements PoolTransactionTransfer {

	private UnmanagedTransactionTarget target;
	private long balance;
	private String address;

	@Override
	/**
	 * Commits this PoolTransaction on the providing pool.
	 */
	public void commit() {
		target.commit(this);
	}

	@Override
	/**
	 * Rolls back this PoolTransaction on the providing pool.
	 */
	public void rollback() {
		// same as commit, simply removes expected balance
		target.commit(this);
	}

	public PoolTransactionUnmanagedOutput(UnmanagedTransactionTarget target, long balance, String address) {
		this.target = target;
		this.balance = balance;
		this.address = address;
	}

	/**
	 * Provides a list with a single jota.model.Transfer object to be used in
	 * TangleTransaction. Balance and address are the same as this object's, message
	 * and tag must be added.
	 * 
	 * @param message Transaction-Message as clear test, will be converted to
	 *                trytes.
	 * @param tag     Transaction-Tag as clear test, will be converted to trytes.
	 */
	@Override
	public List<Transfer> getAsJotaTransfer(String message, String tag) {
		String messageAsTrytes, tagAsTrytes;

		if (message != null)
			messageAsTrytes = TrytesConverter.asciiToTrytes(message);
		else
			messageAsTrytes = "";

		if (tag != null)
			tagAsTrytes = TrytesConverter.asciiToTrytes(message);
		else
			tagAsTrytes = "";

		return Collections.singletonList(new Transfer(address, balance, messageAsTrytes, tagAsTrytes));
	}

	public long getBalance() {
		return balance;
	}

	/**
	 * Would provide an IotaAddress in which a transaction on the IOTA-tangle can be
	 * confirmed, always return null.
	 */
	@Override
	public IotaAddress getManagedAddress() {
		// can't provide, has no managed addresses
		return null;
	}

}
