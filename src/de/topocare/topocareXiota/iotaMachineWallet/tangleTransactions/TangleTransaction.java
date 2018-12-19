package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.*;

import java.time.*;
import java.util.*;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransactionInput;
import de.topocare.topocareXiota.iotaMachineWallet.poolTransactions.PoolTransactionTransfer;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.*;




/**
 * Abstract core of an TangleTransaction (Transaction-Bundle on the Iota-tangle). 
 * <p>
 * Each TangleTransaction-Object will be used at three different points, by different threads:<br>
 * external thread: <br>
 *  - just constructor with required data, should not block the thread for long <br>
 * ExecutionPool of the Transaction-Manager: <br>
 * 	- collect addresses and balances (as defined by specific sub-class) <br>
 *  - attach the transaction-bundle to the IOTA-tangle, including proof of work <br>
 * IotaLoopTask: <br>
 *  - checkTransaction() will be called to validate if transaction has been confirmed or needs to be promoted/reattached after a a specified time <br>
 *  - whenDone() if something else is needed after confirmation
 * <p>
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public abstract class TangleTransaction implements Runnable {
	public TransactionManager transactionManager;

	public TangleTransaction(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	
	/**
	 * Determines what the run() method does next.
	 */
	private NextRun nextrun = NextRun.newAttach;

	

	// dataForTransaction
	
	
	/**
	 * List of PoolTransactionInput Objects, contains the PoolTransactions of all the "IOTA-Input", providing the funding. To be filled in collectAddresses.
	 */
	List<PoolTransactionInput> inputs = new ArrayList<PoolTransactionInput>();
	
	/**
	 * List of PoolTransactionTransfer Objects, contains the PoolTransactions of all the "IOTA-Transfer", providing the target-addresses where the funding goes. To be filled in collectAddresses.
	 */
	List<PoolTransactionTransfer> outputs = new ArrayList<PoolTransactionTransfer>();

	/**
	 * If IOTA-Transactions need specific messages they can be set here (during collectAddresses()).
	 */
	Map<PoolTransactionTransfer, String> message = new HashMap<PoolTransactionTransfer, String>();
	
	/**
	 * If IOTA-Transactions need specific tags they can be set here (during collectAddresses()).
	 */
	Map<PoolTransactionTransfer, String> tag = new HashMap<PoolTransactionTransfer, String>();

	
	//data for confirmation, promote and reattach
	SendTransferResponse sendTransferResponse;
	IotaAddress refForConfirmation;
	long refValue;
	LocalDateTime attachTime_latest;

	
	/**
	 * Collects all the needed data for a transaction bundle.
	 * <p>
	 * Required data is gained by using take- and give-methods of pools.
	 * <P>
	 * a non-abstract child of this class needs to fill: <br>
	 * List<PoolTransactionInput> inputs <br>
	 * List<PoolTransactionTransfer> outputs <br>
	 * Optional:
	 * Map<PoolTransactionTransfer, String> message <br>
	 * Map<PoolTransactionTransfer, String> tag <br>
	 */
	abstract void collectAddresses();

	/**
	 * If after the confirmation of the bundle something special needs to be done, it goes here. Example: TangleTransactionPay
	 */
	abstract void whenDone();

	
	/**
	 * normally starts createTransactionsAndSend(), but also used for promotion or reattachment when needed.
	 */
	@Override
	public void run() {
		if (nextrun == NextRun.newAttach)
			createTransactionsAndSend();
		else if (nextrun == NextRun.promoteOrReattach)
			promoteOrReattach();
		else {
			Error e = new Error("TangeTransaction.run without defined nextrun-state");
			e.printStackTrace();
		}
		nextrun = NextRun.undefined;
	}

	// Transaction-Monitor-Functionality
	boolean isConfirmed = false;

	/**
	 * Attaches the transaction-bundles to the Iota-tangle, build from the data collected in collectAddresses()
	 */
	private void createTransactionsAndSend() {
		// register as pre-pow
		// now in Transaction Manager

		collectAddresses();

		// prepare attach

		// get Input
		List<Input> inputAPI = new ArrayList<Input>();
		for (int pt = 0; pt < inputs.size(); pt++)
			inputAPI.addAll(inputs.get(pt).getAsJotaInputs());

		// get Transfer
		List<Transfer> outputAPI = new ArrayList<Transfer>();
		for (int pt = 0; pt < outputs.size(); pt++)
			outputAPI.addAll(outputs.get(pt).getAsJotaTransfer(message.get(outputs.get(pt)), tag.get(outputs.get(pt))));

		// get managed address for confirmation
		// first tries inputs
		for (int i = 0; i < inputs.size(); i++) {
			refForConfirmation = inputs.get(i).getManagedAddress();
			refValue = 0;
			if (refForConfirmation != null)
				break;
		}
		if (refForConfirmation == null) {

			for (int i = 0; i < outputs.size(); i++) {
				refForConfirmation = outputs.get(i).getManagedAddress();
				refValue = refForConfirmation.getBalance();
				if (refForConfirmation != null)
					break;
			}
		}
		// TODO what if still no address found?!

			

		// attach
		synchronized (transactionManager.powLock) {
			transactionManager.transactionCounterPreServer.decrement();
			transactionManager.transactionCounterAtServer.increment();

			try {
				if (!inputAPI.isEmpty() && !outputAPI.isEmpty()) {
					int depthLocal = depth;
					boolean done = false;
					// attach, retries for "random" "reference transaction is too old"-error
					while (!done && depthLocal <= maxDepth) {
						try {
							sendTransferResponse = api.sendTransfer(seed, security, depthLocal, minWeightMagnitude,
									outputAPI, inputAPI, null, false, false, null);

							boolean allSuccessful = true;
							Boolean successfully[] = sendTransferResponse.getSuccessfully();
							if (successfully != null)
								for (int i = 0; i < successfully.length; i++)
									if (!successfully[i]) {
										allSuccessful = false;
										break;
									}
							if (!allSuccessful)
								throw new Exception("Transaction not successful");

							done = true;
							attachTime_latest = LocalDateTime.now();
						} catch (ArgumentException e) {
							System.err.println("Transaction failed with depth:" + depthLocal);
							if (depthLocal == maxDepth) // lastRetry
							{
								e.printStackTrace();
							}

						}
						depthLocal++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				transactionManager.transactionCounterAtServer.decrement();
			}
		}

		// register Expected values
		transactionManager.confirmOnTangle.registerForConfirmation(refForConfirmation, refValue);

		transactionManager.transactionsAtConfirmation.add(this);
		transactionManager.transsactionCounterAtConfirmation.increment();
	}

	/**
	 * checks if the transaction can be promoted or must be reattached
	 */
	private void promoteOrReattach() {
		synchronized (transactionManager.powLock) {
			transactionManager.transactionCounterPreServer.decrement();
			transactionManager.transactionCounterAtServer.increment();

			try {
				Bundle bundle = new Bundle(sendTransferResponse.getTransactions(),
						sendTransferResponse.getTransactions().size());
				String tail = bundle.getTransactions().get(bundle.getLength() - 1).getHash();

				if (api.checkConsistency(new String[] { tail }).getState()) {

					api.promoteTransaction(tail, depth, minWeightMagnitude, bundle);
					transactionManager.promotions.incrementAndGet();
				} else {
					api.replayBundle(tail, depth, minWeightMagnitude, null);
					transactionManager.reattachments.incrementAndGet();
				}
				attachTime_latest = LocalDateTime.now();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				transactionManager.transactionCounterAtServer.decrement();

			}
		}
		transactionManager.transactionsAtConfirmation.add(this);
		transactionManager.transsactionCounterAtConfirmation.increment();
	}


	/**
	 * Checks if the transaction-bundle was confirmed on the tangle.
	 * If confirmed, then all used PoolTransactions are committed.
	 * 
	 * If false and a specific time has passed since attachment, then promote/reattach.
	 * 
	 * 
	 * @return If the TangleTransaction can be removed from the List of transactions to be confirmed. This happens if it is confirmed or if it must be go back to the ThreadPool for a new Proof of work.
	 */
	public boolean checkTransaction() {
		if (!isConfirmed) {
			//only stays true if all checks are positive
			Boolean nothingUnconfirmed = true;

			// check ref-address
			nothingUnconfirmed = transactionManager.confirmOnTangle.isConfirmed(refForConfirmation);


			
			if (nothingUnconfirmed) {
				inputs.forEach(e -> e.commit());
				outputs.forEach(e -> e.commit());
				isConfirmed = true;
				whenDone();
			}

		}
		if (!isConfirmed)
			if (attachTime_latest.isBefore(LocalDateTime.now().minusMinutes(promoteOrReattachAfterMinutes))) {
				System.out.println("promoteOrReattach needed...");
				nextrun = NextRun.promoteOrReattach;
				transactionManager.submitIgnoreRefundingLock(this);
				return true;
			}
		return isConfirmed;
	}

	/**
	 * Determines what the run() method does next. See: nextRun
	 */
	private enum NextRun {
		undefined, newAttach, promoteOrReattach
	}

}
