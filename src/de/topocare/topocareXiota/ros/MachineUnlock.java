package de.topocare.topocareXiota.ros;

import edu.wpi.rail.jrosbridge.*;
import edu.wpi.rail.jrosbridge.messages.Message;


/**
 * Unlocks the machine by re-publishing messages subscribed from the controlpad to the track control.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class MachineUnlock {
	Ros ros;
	int i=0;
	
	boolean unlock = true;
	Topic topicInputLeft;
	Topic topicInputRight;
	MachineUnlockSubscriber subRightNotBlock;
	MachineUnlockSubscriber subLeftBlock;
	Topic topicOut;
	
	
	public MachineUnlock(Ros ros)
	{
		this.ros = ros;
		topicInputLeft = new Topic(ros, "/sensor/joypad/left_mode_velocity", "geometry_msgs/Twist");
		topicInputRight = new Topic(ros, "/sensor/joypad/right_mode_velocity", "geometry_msgs/Twist");
		topicOut = new Topic(ros, "/base/track/cmd_velocity", "geometry_msgs/Twist");
		

		subRightNotBlock = new MachineUnlockSubscriber((m) -> this.publish(m));
		subLeftBlock = new MachineUnlockSubscriber((m) -> this.publishIfUnlocked(m));
		
		topicInputLeft.subscribe(subLeftBlock);
		topicInputRight.subscribe(subRightNotBlock);
	}
	
	public void setUnlock(boolean value)
	{
		this.unlock = value;
	}

	public void publish(Message message)
	{
		topicOut.publish(message);
	}

	public void publishIfUnlocked(Message message)
	{
		if (unlock)
			publish(message);
	}
	
	public void end()
	{
		topicInputLeft.unsubscribe();
		topicInputRight.unsubscribe();
		topicOut.unadvertise();
	}
}