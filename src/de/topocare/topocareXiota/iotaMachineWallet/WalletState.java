package de.topocare.topocareXiota.iotaMachineWallet;

/**
 * possible states of the iotaMachineWallet
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public enum WalletState {
	noFunding, noFundingAndTransactionOngoing, allFundingInConfirmation, preparingRefunding, refunding, available,
	availableAndTransactionOngoing;
}
