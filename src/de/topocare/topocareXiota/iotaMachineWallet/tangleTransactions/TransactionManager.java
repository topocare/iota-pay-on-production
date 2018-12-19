package de.topocare.topocareXiota.iotaMachineWallet.tangleTransactions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.pools.TransactionInputSource;
import de.topocare.topocareXiota.iotaMachineWallet.tangleMonitoring.ConfirmOnTangle;


/**
 * Manages the TangleTransactions of the wallet, including states, counters and locks. 
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class TransactionManager {
	IotaMachineWalletCore core;
	
	private ExecutorService transactionThreadPool;
	
	/**
	 * Constructor.
	 * 
	 * @param iotaMachineWallet IotaMachineWalletCore using this TransactionManager. 
	 */
	public TransactionManager(IotaMachineWalletCore iotaMachineWallet) {
		this.core = iotaMachineWallet;
		transactionThreadPool = Executors.newFixedThreadPool(10);
	}
	
	/**
	 * Adds a new TangleTransaction to be run. Ignored if the refundingLock is set.
	 * @param runnable The new TangleTransaction.
	 */
	public synchronized void submit(Runnable runnable)
	{
		if(isRefundingLock())
		{
			return;
		}
		transactionCounterPreServer.increment();
		transactionThreadPool.submit(runnable);
	}
	
	/**
	 * Adds a new TangleTransaction to be run, even if refindingLock is set.
	 * Used for promoting and reattaching.
	 * @param runnable
	 */
	public synchronized void submitIgnoreRefundingLock(Runnable runnable)
	{
		transactionCounterPreServer.increment();
		transactionThreadPool.submit(runnable);
	}


	/**
	 * The ConfirmOnTangle object used for confirmations of TangeTransactions managed by this TransactionManager.
	 */
	public ConfirmOnTangle confirmOnTangle = new ConfirmOnTangle();
	
	
	/**
	 * How many transactions are currently waiting for proof of work and attachment.
	 */
	public SynchronizedCounterWithConsumer transactionCounterPreServer = new SynchronizedCounterWithConsumer(
			(a, b) -> core.stateUpdate(a, b));
	/**
	 * How many transactions are currently in proof of work and attachment.
	 */
	public SynchronizedCounterWithConsumer transactionCounterAtServer = new SynchronizedCounterWithConsumer(
			(a, b) -> core.stateUpdate(a, b));
	/**
	 * How many transactions are currently waiting confirmation.
	 */
	public SynchronizedCounterWithConsumer transsactionCounterAtConfirmation = new SynchronizedCounterWithConsumer(
			(a, b) -> core.stateUpdate(a, b));

	/**
	 * How many reattachments were necessary since program start.
	 */
	public AtomicInteger reattachments = new AtomicInteger(0);
	
	/**
	 * How many promotions were necessary since program start.
	 */
	public AtomicInteger promotions = new AtomicInteger(0);

	/**
	 * List of all TangleTransactions waiting for confirmation.
	 */
	public List<TangleTransaction> transactionsAtConfirmation = Collections
			.synchronizedList(new ArrayList<TangleTransaction>());

	
	/**
	 * object used for synchronization of the proof of work (only one at the same time)
	 */
	public Object powLock = new Object();
	
	
	//locks for refunding
	private volatile boolean refundingLock = false;
	private volatile boolean refundingNow = false;

	public boolean isRefundingLock() {
		return refundingLock;
	}

	public void setRefundingLock(boolean refundingLock) {
		this.refundingLock = refundingLock;
		core.stateUpdate();
	}

	public boolean isRefundingNow() {
		return refundingNow;
	}

	public void setRefundingNow(boolean refundingNow) {
		this.refundingNow = refundingNow;
	}
	
	public class SynchronizedCounterWithConsumer {
		private int counter = 0;
		private BiConsumer<Integer, Integer> callOnUpdate;

		public SynchronizedCounterWithConsumer(BiConsumer<Integer, Integer> callOnUpdate) {
			this.callOnUpdate = callOnUpdate;
		}

		public synchronized void increment() {
			int oldValue = counter;
			counter++;
			callOnUpdate.accept(oldValue, counter);
		}

		public synchronized void decrement() {
			int oldValue = counter;
			counter--;
			callOnUpdate.accept(oldValue, counter);
		}

		public synchronized int get() {
			return counter;
		}
	}
}
