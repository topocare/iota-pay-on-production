package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.productionUnitSize;

import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransaction;
import de.topocare.topocareXiota.iotaMachineWallet.pools.*;

/**
 * A TangleTransaction used to add a specific number of production units to the productionPool, with IOTA taken from the useable- or receivingPool.
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class TangleTransactionSpreadUnits extends TangleTransaction {

	int amountInUnits;
	long balanceTakenFromInput = 0;

	ReceivingAddressPool receivingPool;
	IotaAddressPool useablePool;
	IotaAddressPool productionPool;


	/**
	 * Constructor.
	 * 
	 * @param transactionManager The TransactionManager to run and manage this TangleTransaction.
	 * @param amountInUnits The number of new units.
	 * @param receivingPool The pool to take the IOTA from if useablePool has not enough.
	 * @param useablePool The pool where to take the IOTA from.
	 * @param productionPool The pool where the units should be added.
	 */
	public TangleTransactionSpreadUnits(TransactionManager transactionManager, int amountInUnits,
			ReceivingAddressPool receivingPool, IotaAddressPool useablePool, IotaAddressPool productionPool) {
		super(transactionManager);
		this.amountInUnits = amountInUnits;
		this.receivingPool = receivingPool;
		this.useablePool = useablePool;
		this.productionPool = productionPool;
	}

	/**
	 * Collect the used data, will be run when this TangleTransaction has it's own thread.
	 */
	@Override
	void collectAddresses() {
		long targetBalance = amountInUnits * productionUnitSize;
		// get Inputs
		PoolTransaction fromIotaPool = useablePool.takeUpToBalance(targetBalance);
		PoolTransaction fromInputPool = null;

		long missingBalance;
		if (fromIotaPool != null)
			missingBalance = targetBalance - fromIotaPool.balance;
		else
			missingBalance = targetBalance;

		if (missingBalance > 0) {
			fromInputPool = receivingPool.takeBalance(missingBalance);
			if (fromInputPool == null) {
				fromIotaPool.rollback();
				return;
			} else {
				missingBalance -= fromInputPool.balance;
			}
		}

		if (fromIotaPool != null)
			inputs.add(fromIotaPool);
		if (fromInputPool != null)
			inputs.add(fromInputPool);

		
		if (missingBalance < 0)
			outputs.add(useablePool.giveBalance(-missingBalance));

		outputs.add(productionPool.giveUnits(amountInUnits));
	}

	@Override
	void whenDone() {
		// not needed

	}
}
