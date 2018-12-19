package de.topocare.topocareXiota.ros;

import java.util.function.Consumer;

import javax.json.JsonValue;

import edu.wpi.rail.jrosbridge.Ros;
import edu.wpi.rail.jrosbridge.Topic;
import edu.wpi.rail.jrosbridge.messages.Message;

/**
 * Subscribes a topic with boolean messages on ROS and send the messages to a consumer.
 * Only sends the message to the consumer of the content changed.
 * 
 * @author Stefan Kuenne [info@topocare.de]
 */
public class RosCallbackBoolean implements edu.wpi.rail.jrosbridge.callback.TopicCallback {
	Message lastMessage = null;
	Consumer<Boolean> consumer;

	Ros ros;
	Topic rosTopic;

	public RosCallbackBoolean(Ros ros, Consumer<Boolean> consumer, String topicName) {
		this.ros = ros;
		this.consumer = consumer;

		Topic topic = new Topic(ros, topicName, "std_msgs/Bool");
		topic.subscribe(this);
	}

	@Override
	public void handleMessage(Message message) {
		// sorting out repeating messages
		boolean messageIsNew = false;
		if (lastMessage == null)
			messageIsNew = true;
		else if (!lastMessage.equals(message))
			messageIsNew = true;

		lastMessage = message;

		// if message has new content, then get it
		if (messageIsNew) {
			JsonValue jsonData = message.toJsonObject().get("data");
			if (jsonData == JsonValue.TRUE)
				consumer.accept(true);
			else
				consumer.accept(false);
		}
	}

}
