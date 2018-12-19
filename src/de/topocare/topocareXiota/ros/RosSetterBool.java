package de.topocare.topocareXiota.ros;

import edu.wpi.rail.jrosbridge.*;

/**
 * Publishes a boolean message on a specified ROS topic, used for the LEDs.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class RosSetterBool {
	Ros ros;
	
	private Topic topic;
	private boolean oldValue;
	private boolean firstValue=true;
	
	public RosSetterBool(Ros ros, String topicName)
	{
		this.ros = ros;
		topic = new Topic(ros, topicName, "std_msgs/Bool");
	}
	
	public void set(boolean value)
	{
		if (value != oldValue || firstValue)
		{
			oldValue = value;
			firstValue = false;
			topic.publish(new edu.wpi.rail.jrosbridge.messages.std.Bool(value));
		}
	}
	
	
}
