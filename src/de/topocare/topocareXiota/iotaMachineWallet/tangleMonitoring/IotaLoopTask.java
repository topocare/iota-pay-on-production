package de.topocare.topocareXiota.iotaMachineWallet.tangleMonitoring;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.*;

import java.util.Iterator;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions.TangleTransaction;

/**
 * 
 * Pulls balances from the Tangle, checks confirmation of TangleTransactions and
 * refills production-pool.
 * 
 * Should be run in regular intervals
 * (Executors.newSingleThreadScheduledExecutor).
 * 
 * @author Stefan Kuenne [info@topocare.de]
 *
 */
public class IotaLoopTask implements Runnable {
	IotaMachineWalletCore core;

	public IotaLoopTask(IotaMachineWalletCore iotaMachineWalletCore) {
		this.core = iotaMachineWalletCore;
	}

	@Override
	public void run() {

		// pull receiving address balances
		if (core.receivingPool.updateFromTangle())
			core.stateUpdate();

		// check for confirmed transaction-bundles
		core.transactionManager.confirmOnTangle.updateFromTangle();

		// check TangleTransactions using data of step before
		synchronized (core.transactionManager.transactionsAtConfirmation) {
			for (Iterator<TangleTransaction> i = core.transactionManager.transactionsAtConfirmation.listIterator(); i
					.hasNext();) {
				if (i.next().checkTransaction()) {
					i.remove();
					core.transactionManager.transsactionCounterAtConfirmation.decrement();
				}
			}
		}

		// Manage production units
		if (core.productionPool.getExpectedAddressCount() < productionPoolLowerBorder)
			if (core.useablePool.getBalance() + core.receivingPool.getAvailableBalance() >= productionUnitSize) {
				int unitsToCreate = (int) java.lang.Math.min(
						(core.useablePool.getBalance() + core.receivingPool.getAvailableBalance()) / productionUnitSize,
						productionPoolUpperBorder - core.productionPool.getExpectedAddressCount());
				if (unitsToCreate > 0)
					core.transactionFactory.spreadToUnits(unitsToCreate);
			}

	}

}
