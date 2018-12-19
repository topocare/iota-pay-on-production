package de.topocare.topocareXiota.iotaMachineWallet.address;

import static de.topocare.topocareXiota.iotaMachineWallet.IotaConfig.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jota.error.ArgumentException;
import jota.model.Input;
import jota.utils.TrytesConverter;

/**
 * Represents the IotaAddress for the seed, security (both defined in
 * IotaConfig) and a specific keyIndex. Contains the Address-String with and
 * without checksum and the (planned) balance on the Address.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class IotaAddress {
	private int keyIndex;
	private String address;
	private String addressWithChecksum;
	private long balance;

	/**
	 * Constructor, gives the IotaAddress for the given keyIndex. Seed and security
	 * are taken from the <code>IotaConfig</code>. Balance of the address will be 0.
	 * 
	 * @param keyIndex keyIndex of the address
	 */
	public IotaAddress(int keyIndex) {
		this(keyIndex, 0);
	}

	/**
	 * Constructor, gives the IotaAddress for the given keyIndex, with given planned
	 * Balance. Seed and security are taken from the <code>IotaConfig</code>.
	 * 
	 * @param keyIndex keyIndex of the IotaAddress
	 * @param balance  planned balance of the IotaAddress
	 */
	public IotaAddress(int keyIndex, long balance) {
		try {
			this.keyIndex = keyIndex;
			// address = api.getAddressesUnchecked(seed, security, false, keyIndex,
			// 1).first();
			addressWithChecksum = api.getAddressesUnchecked(seed, security, true, keyIndex, 1).first();
			address = addressWithChecksum.substring(0, 81);
			this.balance = balance;

		} catch (ArgumentException e) {
			// offline-application of the api, exception unlikely
			e.printStackTrace();
		}
	}

	/**
	 * Constructor, gives the IotaAddress for the given keyIndex, with given planned
	 * Balance. Seed and security are taken from the <code>IotaConfig</code>.
	 * 
	 * @param keyIndex keyIndex of the IotaAddress
	 * @param balance  planned balance of the IotaAddress
	 */
	public IotaAddress(String addressWithChecksum, int keyIndex, long balance) {
		this.keyIndex = keyIndex;
		this.addressWithChecksum = addressWithChecksum;
		address = addressWithChecksum.substring(0, 81);
		this.balance = balance;

	}

	/**
	 * Get the 81-Trytes IOTA-address-string (without checksum)
	 * 
	 * @return String representing 81 trytes
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Get the 90-trytes IOTA-address-string (including 9 trytes checksum)
	 * 
	 * @return String representing 90 trytes
	 */
	public String getAddressWithChecksum() {
		return addressWithChecksum;
	}

	public int getKeyIndex() {
		return keyIndex;
	}

	/**
	 * Returns a List containing the 81-trytes address-strings for a given List of
	 * IotaAddress. Equivalent to using the .getAddress() of each IotaAddress.
	 * 
	 * @param list List of IotaAddress objects
	 * @return The addresses without checksum, as List<String>.
	 */
	public static List<String> asStringList_getAddress(List<IotaAddress> list) {
		List<String> returnList = new ArrayList<String>(list.size());
		for (int i = 0; i < list.size(); i++)
			returnList.add(list.get(i).getAddress());

		return returnList;
	}

	/**
	 * Returns a List containing the 90-trytes address-strings for a given List of
	 * IotaAddress (including checksum). Equivalent to using the
	 * .getAddressWithChecksum() of each IotaAddress.
	 * 
	 * @param list List of IotaAddress objects
	 * @return The addresses with checksum, as List<String>.
	 */
	public static List<String> asStringList_getAddressWithChecksum(List<IotaAddress> list) {
		List<String> returnList = new ArrayList<String>(list.size());
		for (int i = 0; i < list.size(); i++)
			returnList.add(list.get(i).getAddressWithChecksum());

		return returnList;
	}

	/**
	 * Returns a Array containing the 81-trytes address-strings for a given List of
	 * IotaAddress. Equivalent to using the .getAddress() of each IotaAddress.
	 * 
	 * @param list List of IotaAddress objects
	 * @return The addresses without checksum, as String[]
	 */
	public static String[] asStringArray_getAddress(List<IotaAddress> list) {
		String result[] = new String[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i).getAddress();

		return result;

	}

	/**
	 * Returns a Array containing the 90-trytes address-strings for a given List of
	 * IotaAddress (including checksum). Equivalent to using the .getAddress() of
	 * each IotaAddress.
	 * 
	 * @param list List of IotaAddress objects
	 * @return The addresses with checksum, as String[]
	 */
	public static String[] asStringArray_getAddressWithChecksum(List<IotaAddress> list) {
		String result[] = new String[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i).getAddressWithChecksum();

		return result;
	}

	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}

	/**
	 * Creates a <code>jota.model.Input</code> using this address and balance.
	 * 
	 * @return Input for <code>.sendTransfer</code> in <code>jota.IotaAPI</code>
	 */
	public jota.model.Input getAsJotaInput() {
		return new Input(address, balance, keyIndex, security);
	}

	/**
	 * Creates a <code>jota.model.Transfer</code> using this address and balance.
	 * 
	 * @return Transfer for <code>.sendTransfer</code> in <code>jota.IotaAPI</code>
	 */
	public jota.model.Transfer getAsJotaTransfer() {
		return new jota.model.Transfer(addressWithChecksum, balance);
	}

	/**
	 * Creates a <code>jota.model.Transfer</code> using this address and balance,
	 * adding specified message and tag.
	 * 
	 * @param message message of the IOTA transaction
	 * @param tag     tag of the IOTA transaction
	 * @return Transfer for <code>.sendTransfer</code> in <code>jota.IotaAPI</code>
	 */
	public jota.model.Transfer getAsJotaTransfer(String message, String tag) {
		if (message == null)
			message = "";
		if (tag == null)
			tag = "";

		return new jota.model.Transfer(addressWithChecksum, balance, TrytesConverter.asciiToTrytes(message),
				TrytesConverter.asciiToTrytes(tag));
	}

	/**
	 * Returns a list of new IotaAddress-Objects, containing all addresses from the
	 * startKeyIndex to the endKeyIndex (both inclusive). Addresses are assumed to
	 * have balance of 0. No check on tangle.
	 * 
	 * @param startKeyIndex keyIndex of the first address in the list
	 * @param endKeyIndex   keyIndex of the last address in the list
	 * @return list of new IotaAddress-Objects
	 */
	public static List<IotaAddress> newListOfUncheckedAddresses(int startKeyIndex, int endKeyIndex) {
		return newListOfUncheckedAddresses(startKeyIndex, endKeyIndex, 0);
	}

	/**
	 * Returns a list of new IotaAddress-Objects, containing all addresses from the
	 * startKeyIndex to the endKeyIndex (both inclusive).
	 * 
	 * @param startKeyIndex   keyIndex of the first address in the list
	 * @param endKeyIndex     keyIndex of the last address in the list
	 * @param startingBalance Initial balance of the addresses, no check on tangle.
	 * @return list of new IotaAddress-Objects
	 */
	public static List<IotaAddress> newListOfUncheckedAddresses(int startKeyIndex, int endKeyIndex,
			int startingBalance) {
		
		/*List<IotaAddress> result = new ArrayList<IotaAddress>();
		for (int i = startKeyIndex; i <= endKeyIndex; i++)
			result.add(new IotaAddress(i, startingBalance));
		return result;
		*/
		List<IotaAddress> result = new ArrayList<IotaAddress>();
		List<String> addressesWithChecksum = new ArrayList<String>();
		try {
			addressesWithChecksum = api.getAddressesUnchecked(seed, security, true, startKeyIndex, endKeyIndex-startKeyIndex+1).getAddresses();
			for (int i = 0; i<addressesWithChecksum.size(); i++)
			{
				result.add(new IotaAddress(addressesWithChecksum.get(i), startKeyIndex+i, startingBalance));
			}

		} catch (ArgumentException e) {
			// offline-application of the api, exception unlikely
			e.printStackTrace();
		}
		return result;
		
	}
}
