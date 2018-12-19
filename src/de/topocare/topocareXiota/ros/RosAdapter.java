package de.topocare.topocareXiota.ros;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import edu.wpi.rail.jrosbridge.*;

/**
 * Connects to the ROS on the machine and provides access to sensors and actors.
 * Contains the MachineUnlock, which controls if machine may be used.
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class RosAdapter {
	Ros ros;
	MachineUnlock machineUnlock;

	RosSetterBool signalRedSetter, signalYellowSetter, signalGreenSetter;
	List<edu.wpi.rail.jrosbridge.callback.TopicCallback> callbacks = new ArrayList<edu.wpi.rail.jrosbridge.callback.TopicCallback>();

	public RosAdapter(String host) {
		ros = new Ros(host, 9090);
		ros.connect();


		signalRedSetter = new RosSetterBool(ros, "/sensor/io/signal_red");
		signalYellowSetter = new RosSetterBool(ros, "/sensor/io/signal_yellow");
		signalGreenSetter = new RosSetterBool(ros, "/sensor/io/signal_green");

		machineUnlock = new MachineUnlock(ros);
	}

	public void setMachineUnlocked(boolean value) {
		machineUnlock.setUnlock(value);
	}

	public void setSignalRed(boolean value) {
		signalRedSetter.set(value);
	}

	public void setSignalYellow(boolean value) {
		signalYellowSetter.set(value);
	}

	public void setSignalGreen(boolean value) {
		signalGreenSetter.set(value);
	}

	public void addCallback(Consumer<Boolean> consumer, String topicName) {
		callbacks.add(new RosCallbackBoolean(ros, consumer, topicName));
	}
	
	public void addCallbackGPS(Consumer<String> consumer, String topicName)
	{
		callbacks.add(new RosCallbackGPS(ros, topicName, consumer));
	}
	
	public void addCallbackShort(Consumer<Short> consumer, String topicName)
	{
		callbacks.add(new RosCallbackShort(ros, consumer, topicName));
	}

	public void end() {
		machineUnlock.end();
	}

	public boolean isConnected() {
		return ros.isConnected();
	}
}
