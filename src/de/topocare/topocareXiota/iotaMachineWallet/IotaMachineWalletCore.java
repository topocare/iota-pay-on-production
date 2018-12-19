package de.topocare.topocareXiota.iotaMachineWallet;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.address.IotaFreeAddressFactory;
import de.topocare.topocareXiota.iotaMachineWallet.pools.IotaAddressPool;
import de.topocare.topocareXiota.iotaMachineWallet.pools.ReceivingAddressPool;
import de.topocare.topocareXiota.iotaMachineWallet.pools.UnmanagedTransactionTarget;
import de.topocare.topocareXiota.iotaMachineWallet.tangleMonitoring.ConfirmOnTangle;
import de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions.TangleTransaction;
import de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions.TransactionFactory;
import de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions.TransactionManager;
import jota.dto.response.GetBalancesResponse;
import jota.error.ArgumentException;

/**
 * Core-class of the machine-wallet. To be supplemented by a frontend combining
 * the information and methods needed by machines using the wallet.
 * 
 * Handles the state of the wallet.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class IotaMachineWalletCore {

	// Pools and payment targets
	public ReceivingAddressPool receivingPool;
	public IotaAddressPool useablePool;
	public IotaAddressPool productionPool;

	public UnmanagedTransactionTarget paymentTarget;
	public UnmanagedTransactionTarget refundingTarget;

	// factory for unused iota addresses
	public IotaFreeAddressFactory freeAddressGenerator;

	// Transaction-Handling
	public TransactionManager transactionManager;
	public TransactionFactory transactionFactory;

	/**
	 * Constructor. Creates a wallet using the IotaConfig, using all the options
	 * there.
	 * 
	 * @param inputAddress_first keyIndex of the first address in the receiving pool
	 * @param inputAddress_last  keyIndex of the last address in the receiving pool
	 * @param initialKeyIndex    First keyIndex used for the search for new, unused
	 *                           addresses on the seed. Used for the data in the
	 *                           pools (not receiving pool).
	 * @param outputAddress      String containing the 90-trytes iota-address where
	 *                           payments will be send to. If null: an unused
	 *                           address on the own seed will be used (only for
	 *                           testing).
	 * @param returnAddress      String containing the 90-trytes iota-address where
	 *                           unused funding will be returned to. If null: an
	 *                           unused address on the own seed will be used (only
	 *                           for testing).
	 */
	public IotaMachineWalletCore(int inputAddress_first, int inputAddress_last, int initialKeyIndex,
			String outputAddress, String returnAddress) {

		freeAddressGenerator = new IotaFreeAddressFactory(initialKeyIndex);

		useablePool = new IotaAddressPool(freeAddressGenerator);
		productionPool = new IotaAddressPool(freeAddressGenerator);

		constructorCommons(freeAddressGenerator, inputAddress_first, inputAddress_last, outputAddress, returnAddress);

	}

	/**
	 * Constructor, putting addresses with balances in the given keyIndex range
	 * directly to the pools, ignoring possible ongoing transactions.
	 * <p>
	 * Creates a wallet using the IotaConfig, using all the options there. <br>
	 * All addresses between searchKeyIndexFirst and searchKeyIndexLast (both
	 * including) will be checked for balance and added to the Pools (production
	 * pool if containing exactly one production unit, usable pool otherwise). Any
	 * previously initiated transactions on the tangle regarding these addresses,
	 * that confirm after program start, will result in errors. To be replaced by
	 * persistence layer.
	 * 
	 * @param inputAddress_first  keyIndex of the first address in the receiving
	 *                            pool
	 * @param inputAddress_last   keyIndex of the last address in the receiving pool
	 * @param searchKeyIndexFirst KeyIndex of the first address to be checked for
	 *                            pool inclusion.
	 * @param searchKeyIndexLast  KeyIndex of the last address to be checked for
	 *                            pool inclusion.
	 * @param outputAddress       String containing the 90-trytes IOTA-address where
	 *                            payments will be send to. If null: an unused
	 *                            address on the own seed will be used (only for
	 *                            testing).
	 * @param returnAddress       String containing the 90-trytes IOTA-address where
	 *                            unused funding will be returned to. If null: an
	 *                            unused address on the own seed will be used (only
	 *                            for testing).
	 */
	public IotaMachineWalletCore(int inputAddress_first, int inputAddress_last, int searchKeyIndexFirst,
			int searchKeyIndexLast, String outputAddress, String returnAddress) {

		List<IotaAddress> addressesToCheck = IotaAddress.newListOfUncheckedAddresses(searchKeyIndexFirst,
				searchKeyIndexLast);

		// remove input-addresses from search
		addressesToCheck.removeIf(e -> e.getKeyIndex() >= inputAddress_first && e.getKeyIndex() <= inputAddress_last);

		List<IotaAddress> addressesForProductionPool = new ArrayList<IotaAddress>();
		List<IotaAddress> addressesForUseablePool = new ArrayList<IotaAddress>();
		int highestKeyIndexWithBalance = 0;
		int highestKeyIndexSendFrom = 0;

		// split large api call in smaller requests, 500 elements each
		int requests = addressesToCheck.size() / 500;
		if (addressesToCheck.size() % 500 != 0)
			requests++;
		int maxEndValue = addressesToCheck.size() - 1;

		for (int i = 0; i < requests; i++) {
			int start = i * 500;
			int end = (i + 1) * 500 - 1;
			if (end > maxEndValue)
				end = maxEndValue;
			List<IotaAddress> subList = addressesToCheck.subList(start, end);

			try {
				GetBalancesResponse resBalance = api.getBalances(100, IotaAddress.asStringList_getAddress(subList));
				String[] balances = resBalance.getBalances();
				boolean[] resSpendFrom = api.checkWereAddressSpentFrom(IotaAddress.asStringArray_getAddress(subList));

				for (int b = 0; b < balances.length; b++) {
					long balance = Long.parseLong(balances[b]);
					if (balance != 0) {
						subList.get(b).setBalance(balance);
						if (balance == productionUnitSize) {
							addressesForProductionPool.add(subList.get(b));
						} else {
							addressesForUseablePool.add(subList.get(b));
						}
						highestKeyIndexWithBalance = subList.get(b).getKeyIndex();

					}
					if (resSpendFrom[b]) {
						highestKeyIndexSendFrom = subList.get(b).getKeyIndex();
					}
				}
			} catch (ArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// find starting point for new addresses
		int initialKeyIndex;
		if (highestKeyIndexWithBalance > highestKeyIndexSendFrom)
			initialKeyIndex = highestKeyIndexWithBalance + 1;
		else
			initialKeyIndex = highestKeyIndexSendFrom + 1;
		
		if (initialKeyIndex == 1)
			initialKeyIndex = searchKeyIndexFirst;

		freeAddressGenerator = new IotaFreeAddressFactory(initialKeyIndex);

		useablePool = new IotaAddressPool(freeAddressGenerator, addressesForUseablePool);
		productionPool = new IotaAddressPool(freeAddressGenerator, addressesForProductionPool);

		constructorCommons(freeAddressGenerator, inputAddress_first, inputAddress_last, outputAddress, returnAddress);
	}

	// helper-method for common constructor parts
	private void constructorCommons(IotaFreeAddressFactory freeAddressFactory, int inputAddress_first,
			int inputAddress_last, String outputAddress, String returnAddress) {
		transactionManager = new TransactionManager(this);
		transactionFactory = new TransactionFactory(this, transactionManager);

		if (outputAddress == null)
			outputAddress = freeAddressGenerator.getNextFreeAddress().getAddressWithChecksum();
		this.paymentTarget = new UnmanagedTransactionTarget(outputAddress);

		if (returnAddress == null)
			returnAddress = freeAddressGenerator.getNextFreeAddress().getAddressWithChecksum();
		this.refundingTarget = new UnmanagedTransactionTarget(returnAddress);

		receivingPool = new ReceivingAddressPool(inputAddress_first, inputAddress_last);
		stateUpdate();
	}

	// state management
	public volatile WalletState walletState = WalletState.noFunding;

	/**
	 * Tiggers a stateUpdate() if old or new value is 0, used to check if
	 * transaction counters in transactionManager change the machine state.
	 */
	public void stateUpdate(int oldValue, int newValue) {
		if (oldValue == 0 || newValue == 0)
			stateUpdate();
	}

	/**
	 * Can be set to get updates on the machine state.
	 */
	public Consumer<WalletState> walletStateUpdateConsumer;

	/**
	 * Defines the machine state based on transactions and pools and lock for
	 * refunding.
	 */
	public synchronized void stateUpdate() {
		boolean transaction = false;
		int transactionCount = transactionManager.transactionCounterAtServer.get()
				+ transactionManager.transactionCounterPreServer.get()
				+ transactionManager.transactionsAtConfirmation.size();
		if (transactionCount > 0)
			transaction = true;

		if (transactionManager.isRefundingLock()) {
			if (transactionManager.isRefundingNow())
				walletState = WalletState.refunding;
			else {
				if (transactionCount > 1) {
					walletState = WalletState.preparingRefunding;
				} else {
					walletState = WalletState.refunding;
					transactionManager.setRefundingNow(true);
				}
			}
		} else if (productionPool.getAddressCount() == 0) {
			if (productionPool.getExpectedAddressCount() > 0) {
				walletState = WalletState.allFundingInConfirmation;
			} else if (transaction) {
				walletState = WalletState.noFundingAndTransactionOngoing;
			} else {
				walletState = WalletState.noFunding;
			}
		} else {
			if (transaction)
				walletState = WalletState.availableAndTransactionOngoing;
			else
				walletState = WalletState.available;
		}

		// consumer
		if (walletStateUpdateConsumer != null)
			walletStateUpdateConsumer.accept(walletState);
	}

}
