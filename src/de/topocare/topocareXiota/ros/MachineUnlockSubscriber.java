package de.topocare.topocareXiota.ros;

import java.util.function.Consumer;

import edu.wpi.rail.jrosbridge.messages.Message;

/**
 * Subscribes on ROS and sends the message to a specified consumer.
 * Used to subscribe multiple topics for MachineUnlock.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class MachineUnlockSubscriber implements edu.wpi.rail.jrosbridge.callback.TopicCallback{
	Consumer<Message> consumer;
	
	@Override
	public void handleMessage(Message message) {
		consumer.accept(message);
	}

	public MachineUnlockSubscriber(Consumer<Message> consumer) {
		super();
		this.consumer = consumer;
	}
	
	
	
}
