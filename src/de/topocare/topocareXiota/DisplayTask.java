package de.topocare.topocareXiota;

import de.topocare.topocareXiota.Mediator;
import de.topocare.topocareXiota.iotaMachineWallet.*;

/**
 * Console-output of the current state of wallet, pools, transactions and GPS.
 * 
 * Takes data directly from the <code>IotaMachineWalletCore</code> and underlying classes.
 * Also takes some information from the <code>mediator</code>.
 * 
 * Meant to be run periodic, like every X seconds by an SingleThreadScheduledExecutor.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class DisplayTask implements Runnable {

	IotaMachineWalletCore wallet;
	Mediator mediator;

	public DisplayTask(IotaMachineWalletCore iotaMachineWallet, Mediator mediator) {
		this.wallet = iotaMachineWallet;
		this.mediator = mediator;
	}

	@Override
	public void run() {

		String str = ("\n\n\n");

		str += "Wallet-Status: " + wallet.walletState + "\n";
		str += "GPS: " + mediator.gpsData + "\n";
		str += "Production-units since program-start: " + mediator.unitsSinceProgramStart + "\n\n";

		str += "Pools:\n";

		str += "  Input-Pool:\n";
		str += "   Available: " + wallet.receivingPool.getAvailableBalance() + " Iota on "
				+ wallet.receivingPool.getAvailableBalance_addresses() + " addresses\n";
		str += "  Available-Pool:\n";
		str += "   Available: " + wallet.useablePool.getBalance() + " Iota on " + wallet.useablePool.getAddressCount()
				+ " addresses\n";
		str += "   Expected: " + wallet.useablePool.getExpectedBalance() + " Iota on "
				+ wallet.useablePool.getExpectedAddressCount() + " addresses\n";
		str += "  Production-Pool:\n";
		str += "   Available: " + wallet.productionPool.getBalance() + " Iota as "
				+ wallet.productionPool.getAddressCount() + " Units\n";
		str += "   Expected: " + wallet.productionPool.getExpectedBalance() + " Iota as "
				+ wallet.productionPool.getExpectedAddressCount() + " Units\n";

		str += "\n";

		str += "Outstanding Output\n";
		str += "  Payment in confirmation: " + wallet.paymentTarget.getIncomingBalance() + "\n";
		str += "  Refunding in confirmation: " + wallet.refundingTarget.getIncomingBalance() + "\n";

		str += "\n";
		str += "Transaction-Bundles\n";
		str += "Promotions: " + wallet.transactionManager.promotions + "   Reattachments: "
				+ wallet.transactionManager.reattachments + "\n";
		str += " Pre-POW: " + wallet.transactionManager.transactionCounterPreServer.get() + "\n";
		str += "     POW: " + wallet.transactionManager.transactionCounterAtServer.get() + "\n";
		str += " waiting for confirmation: " + wallet.transactionManager.transsactionCounterAtConfirmation.get()
				+ "\n";

		System.out.println(str);

	}
}
