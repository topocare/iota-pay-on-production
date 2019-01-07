package de.topocare.topocareXiota;

import java.time.LocalDateTime;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.WalletState;
import de.topocare.topocareXiota.ros.RosAdapter;

/**
 * Connection between RosAdapter and IotaMachineWallet, defining necessary
 * consumers, callbacks and rotary encoder logic.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class Mediator {
	private IotaMachineWalletCore iotaMachineWallet;
	private RosAdapter ros;

	/**
	 * will contain the gps-data provided by the RosAdapter (bound
	 * callback/consumer)
	 */
	public String gpsData = ",";

	/**
	 * Counts how many production units have been payed since program start
	 */
	int unitsSinceProgramStart = 0;

	/**
	 * Keyword at the beginning of the message in the iota-transaction.
	 * Allows filtering, should be placed in config/properties file later.
	 */
	String keyword = "topo";

	/**
	 * To be bound as callback/consumer of the RosAdapter, defines reactions to the
	 * machine-buttons
	 * 
	 * @param buttonRed
	 */
	public void setButtonRed(boolean buttonRed) {
		if (buttonRed)
			iotaMachineWallet.transactionFactory.returnToCustomer();
	}

	/**
	 * To be bound as callback/consumer of the RosAdapter, defines reactions to the
	 * machine-buttons
	 * 
	 * @param buttonGreen
	 */
	public void setButtonGreen(boolean buttonGreen) {

	}

	/**
	 * To be bound as callback/consumer of the RosAdapter, defines reactions to the
	 * machine-buttons
	 * 
	 * @param buttonBlue
	 */
	public void setButtonBlue(boolean buttonBlue) {

		if (buttonBlue) {
			// iota.transactionManager.SpendUnits(1, null);
		}
	}

	/**
	 * Constructor, params are the IotaMachineWalletCore and RosAdapter to be
	 * connected.
	 * 
	 * @param iota
	 * @param ros
	 */
	public Mediator(IotaMachineWalletCore iota, RosAdapter ros) {
		this.iotaMachineWallet = iota;
		this.ros = ros;
		bindingCallbacks_ros();
		bindConsumer_iota();
		
		//get state of wallet at constructor time
		checkMachineUnlock(iota.walletState);
	}

	/**
	 * binding nessecery consumers and callbacks of the ros-package
	 */
	private void bindingCallbacks_ros() {
		ros.addCallback(bool -> setButtonRed(bool), "/sensor/io/button_red");
		ros.addCallback(bool -> setButtonGreen(bool), "/sensor/io/button_green");
		ros.addCallback(bool -> setButtonBlue(bool), "/sensor/io/button_blue");
		ros.addCallbackGPS(str -> {
			this.gpsData = str;
		}, "/sensor/gps/navsatfix_spp");
		ros.addCallbackShort(s -> setRotaryEncoderCounter(s), "/sensor/encoder/value");

	}

	/**
	 * binding necessary consumers and callbacks of the iotaMachineWallet-package
	 */
	private void bindConsumer_iota() {
		// iota.unitPool.sizeChangeEvent = (i -> checkMachineUnlock(i));
		iotaMachineWallet.walletStateUpdateConsumer = (i -> checkMachineUnlock(i));
	}

	/**
	 * Changes Machine-State (LEDs, unlock) according to the state of the
	 * iotaMachineWallet.
	 * 
	 * @param state new state of the iotaMachineWallet.
	 */
	private void checkMachineUnlock(WalletState state) {
		if (state == WalletState.allFundingInConfirmation) {
			setLEDs(true, true, true);
			ros.setMachineUnlocked(false);
		} else if (state == WalletState.available) {
			setLEDs(true, false, false);
			ros.setMachineUnlocked(true);
		} else if (state == WalletState.availableAndTransactionOngoing) {
			setLEDs(true, true, false);
			ros.setMachineUnlocked(true);
		} else if (state == WalletState.noFunding) {
			setLEDs(false, false, true);
			ros.setMachineUnlocked(false);
		} else if (state == WalletState.noFundingAndTransactionOngoing) {
			setLEDs(false, true, true);
			ros.setMachineUnlocked(false);
		} else if (state == WalletState.preparingRefunding) {
			setLEDs(false, true, false);
			ros.setMachineUnlocked(false);
		} else if (state == WalletState.refunding) {
			setLEDs(false, true, false);
			ros.setMachineUnlocked(false);
		}
	}

	/**
	 * Shortcut to set all three LEDs at once. Order is green, yellow, red
	 */
	private void setLEDs(boolean green, boolean yellow, boolean red) {
		ros.setSignalGreen(green);
		ros.setSignalYellow(yellow);
		ros.setSignalRed(red);
	}

	private boolean rotaryEncoderActive = true;

	/**
	 * Defines if data of the rotary encoder will be interpreted
	 * 
	 * @return rotaryEncoderActive
	 */
	public boolean isRotaryEncoderActive() {
		return rotaryEncoderActive;
	}

	/**
	 * Defines if data of the rotary encoder will be interpreted
	 * 
	 * @param rotaryEnconderActive new rotaryEncoderActive
	 */
	public void setRotaryEncoderActive(boolean rotaryEnconderActive) {
		if (!this.rotaryEncoderActive && rotaryEnconderActive)
			countFrom = lastValue;
		this.rotaryEncoderActive = rotaryEnconderActive;
	}

	/**
	 * how many ticks on the sensor are one production unit
	 */
	private int ticksPerPayment = 5;

	/**
	 * beginning of the last counted production unit
	 */
	private short countFrom = 0;

	/**
	 * last read sensor-value, will be the next <code>countFrom</code> if counting
	 * was deactivated and reactivated with
	 * <code>setRotaryEncoderActive(false)</code>
	 */
	private short lastValue = 0;

	private boolean firstReading = true;

	/**
	 * Handles the rotary-encoder data and the interpretation as production units.
	 * 
	 * @param rotarySensor data from the sensor, counting up each tick.
	 */
	public void setRotaryEncoderCounter(short rotarySensor) {
		if (firstReading) {
			lastValue = rotarySensor;
			countFrom = rotarySensor;
			firstReading = false;
		}
		if (rotaryEncoderActive) {
			if (rotarySensor == countFrom + ticksPerPayment) {
				unitsSinceProgramStart++;
				LocalDateTime now = LocalDateTime.now();

				StringBuilder message = new StringBuilder();

				// keyword for filtering
				message.append(keyword).append(",");

				// dateTime
				message.append(now.getYear()).append(",");
				message.append(now.getMonthValue()).append(",");
				message.append(now.getDayOfMonth()).append(",");
				message.append(now.getHour()).append(",");
				message.append(now.getMinute()).append(",");
				message.append(now.getSecond()).append(",");

				// gps-data
				message.append(gpsData).append(",");
				message.append(countFrom).append(",");

				// sensor data
				message.append(rotarySensor).append(",");
				message.append(unitsSinceProgramStart).append(",");

				// send to machine
				iotaMachineWallet.transactionFactory.SpendUnits(1, message.toString());
				countFrom = rotarySensor;

			}

		}
		lastValue = rotarySensor;

	}

}
