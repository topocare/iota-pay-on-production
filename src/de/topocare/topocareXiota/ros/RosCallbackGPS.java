package de.topocare.topocareXiota.ros;

import java.util.function.Consumer;

import javax.json.JsonObject;
import javax.json.JsonValue;

import edu.wpi.rail.jrosbridge.Ros;
import edu.wpi.rail.jrosbridge.Topic;
import edu.wpi.rail.jrosbridge.messages.Message;

/**
 * Subscribes the GPS-topic on ROS, extracts latitude and longitude and sends them as one string to the consumer.
 *
 * @author Stefan Kuenne [info@topocare.de]
 */
public class RosCallbackGPS implements edu.wpi.rail.jrosbridge.callback.TopicCallback {

	Ros ros;
	String topicName;
	Consumer<String> consumer;

	public RosCallbackGPS(Ros ros, String topicName, Consumer<String> consumer) {
		this.ros = ros;
		this.topicName = topicName;
		this.consumer = consumer;

		Topic topic = new Topic(ros, topicName, "sensor_msgs/NavSatFix");
		topic.subscribe(this);
	}

	@Override
	public void handleMessage(Message message) {
		JsonObject dings = message.toJsonObject();

		JsonValue latitude = dings.get("latitude");
		JsonValue longitude = dings.get("longitude");
		if (consumer != null)
			if (longitude != null && latitude != null) {
				consumer.accept(latitude.toString().substring(0, 13) + "," + longitude.toString().substring(0, 13));
			}
	}

}
