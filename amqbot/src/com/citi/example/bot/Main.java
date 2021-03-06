package com.citi.example.bot;

import java.util.Date;
import java.util.Properties;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.citi.example.AssetPropertyReader;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

public class Main implements MqttSimpleCallback {
	public static final String APP_ID = "com.citi.example.mqttbot";
	public static final String MQTT_MSG_RECEIVED_INTENT = "com.citi.example.mqtt.service.MSGRECVD";
	public static final String MQTT_MSG_RECEIVED_TOPIC = "com.citi.example.mqtt.service.MSGRECVD_TOPIC";
	public static final String MQTT_MSG_RECEIVED_MSG = "com.citi.example.mqtt.service.MSGRECVD_MSGBODY";
	public static final String MQTT_STATUS_INTENT = "com.citi.example.mqtt.service.STATUS";
	public static final String MQTT_STATUS_MSG = "com.citi.example.mqtt.service.STATUS_MSG";
	public static final String MQTT_PING_ACTION = "com.citi.example.mqtt.service.PING";
	public static final int MQTT_NOTIFICATION_ONGOING = 1;
	public static final int MQTT_NOTIFICATION_UPDATE = 2;
	public static final int MAX_MQTT_CLIENTID_LENGTH = 22;
	private String brokerHostName = "";
	private String publishTopic = "";
	private String brokerPortNumber = "";
	private String publicTopic = "";
	private String privateTopic = "";
	private MqttPersistence usePersistence = null;
	private boolean cleanStart = true;
	private short keepAliveSeconds = 20 * 60;
	private String mqttClientId = null;
	private IMqttClient mqttClient = null;
	private AssetPropertyReader assetsPropertyReader;
    private Properties p;
	private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;

	public enum MQTTConnectionStatus {
		INITIAL, // initial status
		CONNECTING, // attempting to connect
		CONNECTED, // connected
		NOTCONNECTED_WAITINGFORINTERNET, // can't connect because the phone does not have Internet access
		NOTCONNECTED_USERDISCONNECT, // user has explicitly requested disconnection
		NOTCONNECTED_DATADISABLED, // can't connect because the user has disabled data access
		NOTCONNECTED_UNKNOWNREASON // failed to connect for some reason
	}

	public static void main(String[] args) {
		Main m = new Main();
		m.connectToBroker();
	}

	public Main() {
		assetsPropertyReader = new AssetPropertyReader("amq.properties");
        p = assetsPropertyReader.getProperties();
        brokerHostName = p.getProperty("host");
		brokerPortNumber = p.getProperty("port");
		publicTopic = p.getProperty("public_topic");
		privateTopic = p.getProperty("private_topic");
		publishTopic = p.getProperty("publish_topic");
		mqttClientId = generateClientId();
		defineConnectionToBroker(brokerHostName);
	}

	public void disconnect() {
		disconnectFromBroker();
		connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
	}

	@Override
	public void connectionLost() throws Exception {
		if (connectToBroker()) {
			subscribeToTopic();
		}
	}

	@Override
	public void publishArrived(String topic, byte[] payloadbytes, int qos, boolean retained) {
		String messageBody = new String(payloadbytes);
		try {
			JSONObject json = new JSONObject(messageBody);
			notifyUser("from id",json.get("id").toString());
			notifyUser("action", json.get("action").toString());
			handleRequest(json);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void defineConnectionToBroker(String brokerHostName) {
		String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;

		try {
			mqttClient = MqttClient.createMqttClient(mqttConnSpec, usePersistence);
			mqttClient.registerSimpleHandler(this);
		} catch (MqttException e) {
			mqttClient = null;
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			notifyUser("ERROR", "Unable to connect");
		}
	}

	private boolean connectToBroker() {
		try {
			mqttClient.connect(mqttClientId, cleanStart, keepAliveSeconds);
			connectionStatus = MQTTConnectionStatus.CONNECTED;
			subscribeToTopic();
			return true;
		} catch (MqttException e) {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			notifyUser("ERROR", "Unable to connect");
			return false;
		}
	}

	private void subscribeToTopic() {
		boolean subscribed = false;

		if (isAlreadyConnected() == false) {
			System.out.println("mqtt = Unable to subscribe as we are not connected");
		} else {
			try {
				mqttClient.subscribe(new String[] { publishTopic }, new int[] { 2 } );
				subscribed = true;
			} catch (MqttNotConnectedException e) {
				System.out.println("mqtt = subscribe failed - MQTT not connected");
			} catch (IllegalArgumentException e) {
				System.out.println("mqtt = subscribe failed - illegal argument");
			} catch (MqttException e) {
				System.out.println("mqtt = subscribe failed - MQTT exception");
			}
		}

		if (subscribed == false) {
			notifyUser("ERROR", "Unable to subscribe");
		}
	}
	
	public void publishMessageToTopic(String topic, String message) {
		boolean published = false;

		if (isAlreadyConnected() == false) {
			System.out.println("mqtt = Unable to publish as we are not connected");
		} else {
			try {
				mqttClient.publish(topic, message.getBytes(), 2, true);
				published = true;
				notifyUser(topic, message);
			} catch (MqttNotConnectedException e) {
				System.out.println("mqtt = publish failed - MQTT not connected");
			} catch (IllegalArgumentException e) {
				System.out.println("mqtt = publish failed - illegal argument");
			} catch (MqttException e) {
				System.out.println("mqtt = publish failed - MQTT exception");
			}
		}

		if (published == false) {
			notifyUser("ERROR", "Unable to publish");
		}
	}
	
	public boolean isAlreadyConnected() {
		return ((mqttClient != null) && (mqttClient.isConnected() == true));
	}

	private void disconnectFromBroker() {
		try {
			if (mqttClient != null) {
				mqttClient.disconnect();
			}
		} catch (MqttPersistenceException e) {
			System.out.println("mqtt = disconnect failed - persistence exception");
		} finally {
			mqttClient = null;
		}
	}

	private void notifyUser(String key, String value) {
		System.out.println(key + " = " + value);
	}
	
	private String generateClientId() {
		Random rand = new Random();
	    int randomNum = rand.nextInt((1000000 - 0) + 1) + 0;
		if (mqttClientId == null) {
			String timestamp = "" + (new Date()).getTime();
			mqttClientId = timestamp + randomNum;
			if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
				mqttClientId = mqttClientId.substring(0,
						MAX_MQTT_CLIENTID_LENGTH);
			}
		}

		return mqttClientId;
	}
	
	private void handleRequest(JSONObject input)
	{
		try {
			JSONObject json = new JSONObject();
			String topic = privateTopic + input.get("id").toString();
		
			switch (input.get("action").toString()) {
				case "paymentdue":
//					json.put("alert", "A payment is due on account ending in " + input.get("account").toString().substring(12));
//					json.put("title", "Please don't see the title");
					json.put("message", "A payment of $" + input.get("amount").toString() + " for your account ending in " + input.get("account").toString().substring(12) + " is due " + input.get("duedate").toString());
					json.put("pos", "Make a Payment");
					json.put("neg", "Cancel");
					json.put("pos_login", "true");
					json.put("action", "paymentdue");
					json.put("account_from", "1111222233334444");
					json.put("account_to", "3333444455556666");
					json.put("id", "");
					json.put("amount", "100.00");
					break;

				case "spend":
//					json.put("alert", "A purchase was made on your account ending in " + input.get("account").toString().substring(12));
//					json.put("title", "Please don't see the title");
					json.put("message", "A purchase was made on your account ending in " + input.get("account").toString().substring(12) + " in the amount of $" + input.get("amount").toString());
					json.put("pos", "OK");
					json.put("neg", "Cancel");
					json.put("action", "spend");
					json.put("account_from", "1111222233334444");
					json.put("account_to", "3333444455556666");
					json.put("id", "");
					json.put("amount", "100.00");
					break;

				case "spend_pos":
//					json.put("alert", "Your purchase has been authorized, thank you for your continued patronage");
//					json.put("title", "Please don't see the title");
					json.put("message", "Your purchase has been authorized, thank you for your continued patronage");
					json.put("pos", "OK");
					json.put("neg", "Cancel");
					json.put("action", "spend_pos");
					json.put("account_from", "1111222233334444");
					json.put("account_to", "3333444455556666");
					json.put("id", "");
					json.put("amount", "100.00");
					break;

				case "spend_neg":
//					json.put("alert", "A purchase was made on your account ending in " + input.get("account").toString().substring(12));
//					json.put("title", "Please don't see the title");
					json.put("message", "Your purchase has been denied, you will be contacted by Citi to resolve this disputed charge");
					json.put("pos", "OK");
					json.put("neg", "Cancel");
					json.put("action", "spend_neg");
					json.put("account_from", "11112222");
					json.put("account_to", "33334444");
					json.put("id", "");
					json.put("amount", "100.00");
					break;

				case "activatecard1":
					String token = RandomString.nextString(5);
					json.put("alert", "Credit Card Activation requested");
					json.put("title", "Please don't see the title");
					json.put("message", "Credit Card Activation requested for your account ending in " + input.get("account").toString().substring(12) + " please use this token (" + token + ")");
					json.put("pos", "OK");
					json.put("neg", "Cancel");
					json.put("action", "activatecard2");
					json.put("account_from", "11112222");
					json.put("account_to", "33334444");
					json.put("id", "");
					json.put("amount", "100.00");
					json.put("token", token);
					break;

				case "activatecard2":
					if (input.get("ssn").equals("1234")) {
						json.put("alert", "Credit Card Activated");
						json.put("title", "Credit Card Activated");
						json.put("message", "Your card ending in " + input.get("account").toString().substring(12) + " is now activated, Citi thanks you for your business");
						json.put("pos", "OK");
						json.put("neg", "Cancel");
						json.put("action", "activate-end");
						json.put("account_from", "11112222");
						json.put("account_to", "33334444");
						json.put("id", "");
						json.put("amount", "100.00");
					} else {
						json.put("alert", "Credit Card NOT Activated");
						json.put("title", "Credit Card NOT Activated");
						json.put("message", "Your card ending in " + input.get("account").toString().substring(12) + " cannot be activated, please contact Citi for further assistance");
						json.put("pos", "OK");
						json.put("neg", "Cancel");
						json.put("action", "activate-end");
						json.put("account_from", "11112222");
						json.put("account_to", "33334444");
						json.put("id", "");
						json.put("amount", "100.00");
					}
					break;

				case "transfer":
					json.put("alert", "Transfer Completed");
					json.put("title", "Please don't see the title");
					json.put("message", "Your transfer from account ending in " + input.get("from_account").toString().substring(12) + " to account ending in " + input.get("to_account").toString().substring(12) + " in the amount of $" + input.get("amount").toString() + " was successfull");
					json.put("pos", "Login");
					json.put("neg", "OK");
					json.put("pos_login", "true");
					json.put("action", "activatecard2");
					json.put("account_from", "11112222");
					json.put("account_to", "33334444");
					json.put("id", "");
					json.put("amount", "100.00");
					break;
			}
			
			final String tmp = json.toString();
			final String tmp2 = topic.toString();
			Thread t = new Thread(new Runnable() {
		         public void run()
		         {
		        	 try {
						mqttClient.publish(tmp2, tmp.getBytes(), 2, true);
						System.out.println(tmp2 + " = " + tmp);
					} catch (IllegalArgumentException | MqttException e) {
						e.printStackTrace();
					}
		 		 }
			});
			t.start();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static class RandomString {
		public static String nextString(int length) {
			char[] buf;
			Random random = new Random();
			StringBuilder tmp = new StringBuilder();
			for (char ch = '0'; ch <= '9'; ++ch)
				tmp.append(ch);
//			for (char ch = 'a'; ch <= 'z'; ++ch)
//				tmp.append(ch);
			char[] symbols = tmp.toString().toCharArray();

			buf = new char[length];

			for (int idx = 0; idx < buf.length; ++idx)
				buf[idx] = symbols[random.nextInt(symbols.length)];
			return new String(buf);
		}
	}
}
