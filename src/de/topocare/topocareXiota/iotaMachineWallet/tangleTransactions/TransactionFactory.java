package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import java.util.ArrayList;
import java.util.List;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.pools.TransactionInputSource;


/**
 * Creates new TangleTransactions as needed. Should be integrated in a frontend class later.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class TransactionFactory {
	IotaMachineWalletCore core;
	TransactionManager transactionManager;

	public TransactionFactory(IotaMachineWalletCore core, TransactionManager transactionManager) {
		this.core = core;
		this.transactionManager = transactionManager;
	}
	
	public synchronized void SpendUnits(int amount)
	{
		SpendUnits(amount, null);
	}
	
	public synchronized void SpendUnits(int amount, String message)
	{
		TangleTransactionPay task = new TangleTransactionPay(transactionManager, core.productionPool, core.paymentTarget, amount, message);
		transactionManager.submit(task);
	}
	
	public synchronized void spreadToUnits(int amount)
	{
		TangleTransactionSpreadUnits task = new TangleTransactionSpreadUnits(transactionManager, amount, core.receivingPool, core.useablePool, core.productionPool);
		transactionManager.submit(task);
	}
	
	public synchronized void returnToCustomer()
	{
		if (!transactionManager.isRefundingLock())
		{
			List<TransactionInputSource> sources = new ArrayList<TransactionInputSource>(3);
			sources.add(core.receivingPool);
			sources.add(core.useablePool);
			sources.add(core.productionPool);
			
			
			TangleTransaction task = new TangleTransactionRefunding(transactionManager, sources, core.refundingTarget);
			transactionManager.submit(task);
			
		}
		
	}
}
