package de.topocare.topocareXiota.iotaMachineWallet.poolTransactions;

import java.util.List;

import jota.model.Input;



/**
 * Can provide a List of jota.model.Input, as needed by the jota.jota.IotaAPI.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public interface PoolTransactionInput extends PoolTransactionBase {
	public List<Input> getAsJotaInputs();
}
