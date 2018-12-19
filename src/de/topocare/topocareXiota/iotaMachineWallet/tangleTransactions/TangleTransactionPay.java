package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.productionUnitSize;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransactionTransfer;
import de.topocare.topocareXiota.iotaMachineWallet.pools.IotaAddressPool;
import de.topocare.topocareXiota.iotaMachineWallet.pools.UnmanagedTransactionTarget;

	

/**
 * A TangleTransaction used to pay a single production unit to a UnmanagedTransactionTarget.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class TangleTransactionPay extends TangleTransaction {

	int amountInUnits;
	String payment_message;
	
	IotaAddressPool productionPool;
	UnmanagedTransactionTarget paymentTarget;
	
	/**
	 * Constructor.
	 * 
	 * @param transactionManager The TransactionManager to run and manage this TangleTransaction.
	 * @param productionPool The pool where the needed production unit(s) is/are taken from.
	 * @param paymentTarget The target where the production unit(s) is/are send.
	 * @param amountInUnits The number of units to send.
	 * @param message The on the IOTA-Transaction.
	 */
	public TangleTransactionPay(TransactionManager transactionManager, IotaAddressPool productionPool, UnmanagedTransactionTarget paymentTarget, int amountInUnits, String message)
	{
		super(transactionManager);
		this.amountInUnits = amountInUnits;
		
		this.productionPool = productionPool;
		this.paymentTarget = paymentTarget;
		this.payment_message = message;
		
	}
	
	/**
	 * Collect the used data, will be run when this TangleTransaction has it's own thread.
	 */
	@Override
	void collectAddresses() {
		//take the money from the productionPool, if not possible cancel.
		PoolTransaction input = productionPool.takeElements(amountInUnits);
		if (input == null)
			return;
		this.inputs.add(input);
		
		//give the payment to the paymentTarget
		PoolTransactionTransfer target = paymentTarget.give(amountInUnits*productionUnitSize);
		this.outputs.add(target);
		
		//add the message
		message.put(target, payment_message);
		
}

	@Override
	void whenDone() {
		// not needed
		
	}
	

}
