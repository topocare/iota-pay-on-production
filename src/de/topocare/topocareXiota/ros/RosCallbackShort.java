package de.topocare.topocareXiota.ros;

import java.util.function.Consumer;

import javax.json.JsonValue;

import edu.wpi.rail.jrosbridge.Ros;
import edu.wpi.rail.jrosbridge.Topic;
import edu.wpi.rail.jrosbridge.messages.Message;

/**
 * Subscribes a topic with short/int16 messages on ROS and send the messages to a consumer.
 * Only sends the message to the consumer of the content changed.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class RosCallbackShort implements edu.wpi.rail.jrosbridge.callback.TopicCallback {
	Message lastMessage = null;
	Consumer<Short> consumer;

	Topic rosTopic;
	Ros ros;

	@Override
	public void handleMessage(Message message) {
		// sorting out repeating messages
		boolean messageIsNew = false;
		if (lastMessage == null)
			messageIsNew = true;
		else if (!lastMessage.equals(message))
			messageIsNew = true;

		lastMessage = message;

		// if message has new content, then proceed by type
		if (messageIsNew) {
			JsonValue jsonData = message.toJsonObject().get("data");
			consumer.accept(Short.parseShort(jsonData.toString()));
		}

	}

	public RosCallbackShort(Ros ros, Consumer<Short> consumer, String topicName) {
		this.consumer = consumer;
		this.ros = ros;

		Topic topic = new Topic(ros, topicName, "std_msgs/Int16");
		topic.subscribe(this);
	}

}
