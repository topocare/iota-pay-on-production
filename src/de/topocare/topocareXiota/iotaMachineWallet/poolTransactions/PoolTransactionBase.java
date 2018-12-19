package de.topocare.topocareXiota.iotaMachineWallet.poolTransactions;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;


/**
 * Common base for transaction-parts. 
 * <p>
 * Can be called to commit or roll back itself (by knowing its related pool).
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public interface PoolTransactionBase {

	void commit();

	void rollback();

	/**
	 * Get an address which can be used to confirm a transaction. It must be an address which is used only by the pool mechanics, not by external users. If this Object can't provide one return null.
	 * @return
	 */
	IotaAddress getManagedAddress();

}