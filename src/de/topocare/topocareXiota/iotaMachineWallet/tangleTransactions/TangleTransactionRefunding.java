package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import java.util.ArrayList;
import java.util.List;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;
import de.topocare.topocareXiota.iotaMachineWallet.pools.TransactionInputSource;
import de.topocare.topocareXiota.iotaMachineWallet.pools.UnmanagedTransactionTarget;

/**
 * A TangleTransaction sending the content of all pools to one
 * UnmanagedTransactionTarget.
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class TangleTransactionRefunding extends TangleTransaction {

	List<TransactionInputSource> sources;
	UnmanagedTransactionTarget refundingTarget;

	/**
	 * @param transactionManager The TransactionManager to run and manage this
	 *                           TangleTransaction.
	 * @param sources            List of all pools used.
	 * @param refundingTarget    The target where the IOTA token are send.
	 */
	public TangleTransactionRefunding(TransactionManager transactionManager, List<TransactionInputSource> sources,
			UnmanagedTransactionTarget refundingTarget) {
		super(transactionManager);
		this.sources = sources;
		this.refundingTarget = refundingTarget;
	}

	/**
	 * Collect the used data, will be run when this TangleTransaction has it's own
	 * thread.
	 */
	@Override
	void collectAddresses() {
		// wait for all other tasks to be done
		transactionManager.setRefundingLock(true);
		try {
			while (!transactionManager.isRefundingNow()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
			}

			// collect all balances
			long balance = 0;

			PoolTransaction tmp;
			for (int i = 0; i < sources.size(); i++) {
				tmp = sources.get(i).takeAll();
				if (tmp != null) {
					inputs.add(tmp);
					balance += tmp.balance;
				}
			}

			//define target
			if (balance > 0)
				outputs.add(refundingTarget.give(balance));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	void whenDone() {
		//remove the locks
		transactionManager.setRefundingLock(false);
		transactionManager.setRefundingNow(false);

	}

}
