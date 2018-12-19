package de.topocare.topocareXiota.iotaMachineWallet.poolTransactions;
import java.util.List;

import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransactionBase;
import jota.model.Transfer;

/**
 * Can provide a List of jota.model.Transfer, as needed by the jota.jota.IotaAPI.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public interface PoolTransactionTransfer extends PoolTransactionBase {
	public List<Transfer> getAsJotaTransfer(String message, String tag);
}
