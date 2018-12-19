package de.topocare.topocareXiota.iotaMachineWallet.tangleMonitoring;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.api;

import java.util.*;

import de.topocare.topocareXiota.iotaMachineWallet.address.IotaAddress;
import jota.dto.response.GetBalancesResponse;



/**
 * Handles the confirmation of transaction-bundles on the tangle. 
 * 
 * Uses the balance of a address that needs to reach a defined value instead of getInclusionStates to be snapshot-proof when the persistence layer is implemented.
 * 
 * <p>
 * registerForConfirmation adds a address+value-pairs to check; 
 * <p>
 * updateFromTangle performs the pull and comparison; 
 * <P>
 * isConfirmed returns if a pair has been confirmed and deletes it when the result was true
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */

public class ConfirmOnTangle {

	Map<IotaAddress, ExpectationAndResult> toBeConfirmed = new HashMap<IotaAddress, ExpectationAndResult>();

	
	public synchronized void updateFromTangle() {
		if (!toBeConfirmed.isEmpty()) {
			try {

				List<IotaAddress> iotaAddresses = new ArrayList<IotaAddress>(toBeConfirmed.size());
				toBeConfirmed.forEach((a, b) -> iotaAddresses.add(a));

				GetBalancesResponse response = api.getBalances(100, IotaAddress.asStringList_getAddress(iotaAddresses));

				String balances[] = response.getBalances();

				for (int i = 0; i < balances.length; i++) {
					IotaAddress addr = iotaAddresses.get(i);
					ExpectationAndResult e = toBeConfirmed.get(addr);
					
					e.confirmed =  Long.parseLong(balances[i]) == e.expectedValue;
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * Adds a address and expected value the list of addresses to be confirmed.
	 * 
	 * @param address
	 * @param expectedValue
	 */
	public synchronized void registerForConfirmation(IotaAddress address, long expectedValue) {
		toBeConfirmed.putIfAbsent(address, new ExpectationAndResult(expectedValue));
	}

	public synchronized boolean isConfirmed(IotaAddress address) {
		if (toBeConfirmed.containsKey(address))
			if (toBeConfirmed.get(address).confirmed)
			{
				toBeConfirmed.remove(address);
				return true;
			}
		return false;
	}
	
	private class ExpectationAndResult
	{
		long expectedValue;
		boolean confirmed = false;
		
		public ExpectationAndResult(long expectedValue)
		{
			this.expectedValue = expectedValue;
		}
	}
}
