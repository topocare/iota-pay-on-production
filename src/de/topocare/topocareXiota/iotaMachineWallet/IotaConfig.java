package de.topocare.topocareXiota.iotaMachineWallet;

import jota.IotaAPI;
import jota.pow.pearldiver.PearlDiverLocalPoW;

/**
 * static configuration of the iotaMachineWallet.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class IotaConfig {

	/**
	 * The seed to be used by the wallet.
	 */
	public static String seed;

	/**
	 * try to attach using this value
	 */
	public static int depth;

	/**
	 * retry with higher depth until you reach
	 */
	public static int maxDepth;

	/**
	 * IOTA constant
	 */
	public static int minWeightMagnitude;

	/**
	 * IOTA security level used by the wallet
	 */
	public static int security;

	/**
	 * The jota.IotaAPI to be used by the wallet.
	 */
	public static IotaAPI api;

	/**
	 * IOTA token per production unit.
	 */
	public static int productionUnitSize;

	/**
	 * refill production-pool if under x production units.
	 */
	public static int productionPoolLowerBorder;

	/**
	 * refill production-pool up to this value.
	 */
	public static int productionPoolUpperBorder;

	/**
	 * transaction-bundles on the tangle will be promoted (reattached if nessessery)
	 * after X minutes.
	 */
	public static int promoteOrReattachAfterMinutes;
}
