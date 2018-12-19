package de.topocare.topocareXiota.iotaMachineWallet.address;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.api;

import java.util.ArrayList;
import java.util.List;

import jota.dto.response.GetBalancesResponse;
import jota.error.ArgumentException;

/**
 * Searches for unused IotaAddress(es) on the seed and security defined in
 * IotaConfig, starting at the provided keyIndex.
 * <p>
 * will use the <code>jota.IotaAPI</code> methods
 * <code>checkWereAddressSpentFrom</code> and <code>getBalances</code> to
 * determine if address is free (unused), and never return the same address
 * twice
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class IotaFreeAddressFactory {

	/**
	 * Constructor, first search will begin at keyIndex 0.
	 */
	public IotaFreeAddressFactory() {
		this(0);
	}

	/**
	 * Constructor
	 * 
	 * @param startingKeyIndexCount first search will begin here
	 */
	public IotaFreeAddressFactory(int startingKeyIndexCount) {
		this.keyIndexCount = startingKeyIndexCount;
	}

	private int keyIndexCount;

	/**
	 * @return keyIndex, where next search will start
	 */
	public int getKeyIndexCount() {
		return keyIndexCount;
	}

	/**
	 * set keyIndex, where next search will start
	 */
	public void setKeyIndexCount(int keyIndexCount) {
		this.keyIndexCount = keyIndexCount;
	}

	/**
	 * @return next free (unused) IotaAddress
	 */
	public synchronized IotaAddress getNextFreeAddress() {
		return getNextFreeAddresses(1).get(0);
	}

	/**
	 * @param amount how many addresses
	 * @return List containing the next free (unused) IotaAddress(es)
	 */
	public synchronized List<IotaAddress> getNextFreeAddresses(int amount) {
		try {
			List<IotaAddress> generatedAddresses = new ArrayList<IotaAddress>();
			for (int i = 0; i < amount; i++)
				generatedAddresses.add(new IotaAddress(keyIndexCount + i));

			boolean wereSpendFrom[] = api
					.checkWereAddressSpentFrom(IotaAddress.asStringArray_getAddress(generatedAddresses));
			GetBalancesResponse res2_object = api.getBalances(100,
					IotaAddress.asStringList_getAddress(generatedAddresses));
			String balance_asString[] = res2_object.getBalances();

			List<IotaAddress> usedAddresses = new ArrayList<IotaAddress>();
			for (int i = 0; i < amount; i++) {
				if (wereSpendFrom[i] || !(balance_asString[i].equals("0")))
					usedAddresses.add(generatedAddresses.get(i));
			}

			generatedAddresses.removeAll(usedAddresses);
			keyIndexCount += amount;
			if (generatedAddresses.size() < amount)
				generatedAddresses.addAll(getNextFreeAddresses(amount - generatedAddresses.size()));
			return generatedAddresses;

		} catch (ArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

}
