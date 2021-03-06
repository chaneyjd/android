package com.citi.example.mqtt.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import com.citi.example.AssetPropertyReader;
import com.citi.example.mqtt.FullscreenActivity;
import com.citi.example.mqtt.R;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

@SuppressLint("Wakelock")
public class MainService extends Service implements MqttSimpleCallback {
	public static final String APP_ID = "com.citi.example.mqtt.service";
	public static final String MQTT_MSG_RECEIVED_INTENT = "com.citi.example.mqtt.service.MSGRECVD";
	public static final String MQTT_MSG_RECEIVED_TOPIC = "com.citi.example.mqtt.service.MSGRECVD_TOPIC";
	public static final String MQTT_MSG_RECEIVED_MSG = "com.citi.example.mqtt.service.MSGRECVD_MSGBODY";
	public static final String MQTT_STATUS_INTENT = "com.citi.example.mqtt.service.STATUS";
	public static final String MQTT_STATUS_MSG = "com.citi.example.mqtt.service.STATUS_MSG";
	public static final String MQTT_PING_ACTION = "com.citi.example.mqtt.service.PING";
	public static final int MQTT_NOTIFICATION_ONGOING = 1;
	public static int MQTT_NOTIFICATION_UPDATE = 2;
	public static final int MAX_MQTT_CLIENTID_LENGTH = 22;
	private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;
	private Hashtable<String, String> dataCache = new Hashtable<String, String>();
	private LocalBinder<MainService> mBinder;
	private String brokerHostName = "";
	private String publishTopic = "";
	private ArrayList<String> topics = new ArrayList<String>();
	private ArrayList<Integer> qos = new ArrayList<Integer>();
	private String brokerPortNumber = "";
	private MqttPersistence usePersistence = null;
	private boolean cleanStart = true;
	private short keepAliveSeconds = 20 * 60;
	private String mqttClientId = null; // unique to the broker - two clients not permitted using the same client ID
	private IMqttClient mqttClient = null;
	private NetworkConnectionIntentReceiver netConnReceiver;
	private BackgroundDataChangeIntentReceiver dataEnabledReceiver;
	private PingSender pingSender;
	private AssetPropertyReader assetsPropertyReader;
    private Properties p;

	public enum MQTTConnectionStatus {
		INITIAL, // initial status
		CONNECTING, // attempting to connect
		CONNECTED, // connected
		NOTCONNECTED_WAITINGFORINTERNET, // can't connect because the phone does not have Internet access
		NOTCONNECTED_USERDISCONNECT, // user has explicitly requested disconnection
		NOTCONNECTED_DATADISABLED, // can't connect because the user has disabled data access
		NOTCONNECTED_UNKNOWNREASON // failed to connect for some reason
	}
	
	public MainService() {
//		Log.d("MainService", "In Constructor");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		connectionStatus = MQTTConnectionStatus.INITIAL;
		mBinder = new LocalBinder<MainService>(this);

		assetsPropertyReader = new AssetPropertyReader(this);
        p = assetsPropertyReader.getProperties("amqtt.properties");

		brokerHostName = p.getProperty("host");
		brokerPortNumber = p.getProperty("port");
		topics.add(p.getProperty("public_topic"));
		qos.add(2);
		topics.add(p.getProperty("private_topic") + "android/" + Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
		qos.add(2);
		publishTopic = p.getProperty("publish_topic");
		
		dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
		registerReceiver(dataEnabledReceiver, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
		defineConnectionToBroker(brokerHostName);
	}
	
	@Override
	public void onStart(final Intent intent, final int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, "MQTTservice").start();
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, "MQTTservice").start();
		return START_STICKY;
	}

	@SuppressWarnings("deprecation")
	synchronized void handleStart(Intent intent, int startId) {
		if (mqttClient == null) {
			stopSelf();
			return;
		}

		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getBackgroundDataSetting() == false) // respect the user's no data
		{
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
			broadcastServiceStatus("Not connected - background data disabled");
			return;
		}

		rebroadcastStatus();
		rebroadcastReceivedMessages();

		if (isAlreadyConnected() == false) {
			connectionStatus = MQTTConnectionStatus.CONNECTING;

//			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//			Notification notification = new Notification(R.drawable.ic_citi, "MQTT", System.currentTimeMillis());
//			notification.flags |= Notification.FLAG_ONGOING_EVENT;
//			notification.flags |= Notification.FLAG_NO_CLEAR;
//			Intent notificationIntent = new Intent(this, MainActivity.class);
//			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//			notification.setLatestEventInfo(this, "MQTT", "MQTT Service is running", contentIntent);
//			notification.setLatestEventInfo(this, "MQTT", "MQTT Service is running", null);
//			nm.notify(MQTT_NOTIFICATION_ONGOING, notification);

			if (isOnline()) {
				if (connectToBroker()) {
					subscribeToTopic();
				}
			} else {
				connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
				broadcastServiceStatus("Waiting for network connection");
			}
		}

		if (netConnReceiver == null) {
			netConnReceiver = new NetworkConnectionIntentReceiver();
			registerReceiver(netConnReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));

		}

		if (pingSender == null) {
			pingSender = new PingSender();
			registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
		}
	}

	@Override
	public void onDestroy() {
		Log.d("DEBUG", "In onDestroy for Service");
		super.onDestroy();

		disconnectFromBroker();
		broadcastServiceStatus("Disconnected");

		if (dataEnabledReceiver != null) {
			unregisterReceiver(dataEnabledReceiver);
			dataEnabledReceiver = null;
		}

		if (mBinder != null) {
			mBinder.close();
			mBinder = null;
		}
	}

	private void broadcastServiceStatus(String statusDescription) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MQTT_STATUS_INTENT);
		broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
		sendBroadcast(broadcastIntent);
	}

	private void broadcastReceivedMessage(String topic, String message) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
		broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
		broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
		sendBroadcast(broadcastIntent);
	}

	@SuppressWarnings("deprecation")
	private void notifyUser(String alert, String title, String body) {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_citi, alert, System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.ledARGB = Color.MAGENTA;
		Intent notificationIntent = new Intent(this, FullscreenActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 2, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, title, body, contentIntent);
		nm.notify(MQTT_NOTIFICATION_UPDATE++, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder<S> extends Binder {
		private WeakReference<S> mService;

		public LocalBinder(S service) {
			mService = new WeakReference<S>(service);
		}

		public S getService() {
			return mService.get();
		}

		public void close() {
			mService = null;
		}
	}

	public MQTTConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public void rebroadcastStatus() {
		String status = "";

		switch (connectionStatus) {
		case INITIAL:
			status = "Please wait";
			break;
		case CONNECTING:
			status = "Connecting...";
			break;
		case CONNECTED:
			status = "Connected";
			break;
		case NOTCONNECTED_UNKNOWNREASON:
			status = "Not connected - waiting for network connection";
			break;
		case NOTCONNECTED_USERDISCONNECT:
			status = "Disconnected";
			break;
		case NOTCONNECTED_DATADISABLED:
			status = "Not connected - background data disabled";
			break;
		case NOTCONNECTED_WAITINGFORINTERNET:
			status = "Unable to connect";
			break;
		}

		if ((connectionStatus != MQTTConnectionStatus.INITIAL) && (connectionStatus != MQTTConnectionStatus.CONNECTED) && (connectionStatus != MQTTConnectionStatus.CONNECTING)) {
			broadcastServiceStatus(status);
		}
	}

	public void disconnect() {
		disconnectFromBroker();
		connectionStatus = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;
		broadcastServiceStatus("Disconnected");
	}

	public void connectionLost() throws Exception {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		wl.acquire();

		if (isOnline() == false) {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

			broadcastServiceStatus("Connection lost - no network connection");
			notifyUser("Connection lost - no network connection", "MQTT", "Connection lost - no network connection");
		} else {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Connection lost - reconnecting...");
			if (connectToBroker()) {
				subscribeToTopic();
			}
		}

		wl.release();
	}

	public void publishArrived(String topic, byte[] payloadbytes, int qos, boolean retained) {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		wl.acquire();
		String messageBody = new String(payloadbytes);
		try {
			JSONObject json = new JSONObject(messageBody);
			if (addReceivedMessageToStore(topic, messageBody)) {
				broadcastReceivedMessage(topic, messageBody);
				notifyUser(json.get("alert").toString() , " ", json.get("message").toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		scheduleNextPing();
		wl.release();
	}

	private void defineConnectionToBroker(String brokerHostName) {
		String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;

		try {
			mqttClient = MqttClient.createMqttClient(mqttConnSpec, usePersistence);
			mqttClient.registerSimpleHandler(this);
		} catch (MqttException e) {
			mqttClient = null;
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Invalid connection parameters");
			notifyUser("Unable to connect", "MQTT", "Unable to connect");
		}
	}

	private boolean connectToBroker() {
		try {
			mqttClient.connect(generateClientId(), cleanStart, keepAliveSeconds);
//			broadcastServiceStatus("Connected");
			connectionStatus = MQTTConnectionStatus.CONNECTED;
			scheduleNextPing();
			return true;
		} catch (MqttException e) {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Unable to connect");
			notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");
			scheduleNextPing();
			return false;
		}
	}

	private void subscribeToTopic() {
		boolean subscribed = false;

		if (isAlreadyConnected() == false) {
			Log.e("mqtt", "Unable to subscribe as we are not connected");
		} else {
			try {
				mqttClient.subscribe(topics.toArray(new String[topics.size()]), convertIntegers(qos));
				subscribed = true;
			} catch (MqttNotConnectedException e) {
				Log.e("mqtt", "subscribe failed - MQTT not connected", e);
			} catch (IllegalArgumentException e) {
				Log.e("mqtt", "subscribe failed - illegal argument", e);
			} catch (MqttException e) {
				Log.e("mqtt", "subscribe failed - MQTT exception", e);
			}
		}

		if (subscribed == false) {
			broadcastServiceStatus("Unable to subscribe");
			notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
		}
	}
	
	public void publishMessageToTopic(String message) {
		boolean published = false;

		if (isAlreadyConnected() == false) {
			Log.e("mqtt", "Unable to publish as we are not connected");
		} else {
			try {
				mqttClient.publish(publishTopic, message.getBytes(), 2, true);
				published = true;
			} catch (MqttNotConnectedException e) {
				Log.e("mqtt", "publish failed - MQTT not connected", e);
			} catch (IllegalArgumentException e) {
				Log.e("mqtt", "publish failed - illegal argument", e);
			} catch (MqttException e) {
				Log.e("mqtt", "publish failed - MQTT exception", e);
			}
		}

		if (published == false) {
			broadcastServiceStatus("Unable to publish");
			notifyUser("Unable to publish", "MQTT", "Unable to publish");
		}
		
	}

	private void disconnectFromBroker() {
		try {
			if (netConnReceiver != null) {
				unregisterReceiver(netConnReceiver);
				netConnReceiver = null;
			}

			if (pingSender != null) {
				unregisterReceiver(pingSender);
				pingSender = null;
			}
		} catch (Exception eee) {
			Log.e("mqtt", "unregister failed", eee);
		}

		try {
			if (mqttClient != null) {
				mqttClient.disconnect();
			}
		} catch (MqttPersistenceException e) {
			Log.e("mqtt", "disconnect failed - persistence exception", e);
		} finally {
			mqttClient = null;
		}

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

	public boolean isAlreadyConnected() {
		return ((mqttClient != null) && (mqttClient.isConnected() == true));
	}

	private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context ctx, Intent intent) {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			wl.acquire();

			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			if (cm.getBackgroundDataSetting()) {
				defineConnectionToBroker(brokerHostName);
				handleStart(intent, 0);
			} else {
				connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
				broadcastServiceStatus("Not connected - background data disabled");
				disconnectFromBroker();
			}

			wl.release();
		}
	}

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			wl.acquire();

			if (isOnline()) {
//				if (connectToBroker()) {
//					subscribeToTopic();
//				}
			}
			wl.release();
		}
	}

	private void scheduleNextPing() {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(MQTT_PING_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);
		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
	}

	public class PingSender extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				mqttClient.ping();
			} catch (MqttException e) {
				Log.e("mqtt", "ping failed - MQTT exception", e);

				try {
					mqttClient.disconnect();
				} catch (MqttPersistenceException e1) {
					Log.e("mqtt", "disconnect failed - persistence exception", e1);
				}

				if (connectToBroker()) {
					subscribeToTopic();
				}
			}
			scheduleNextPing();
		}
	}

	private boolean addReceivedMessageToStore(String key, String value) {
		String previousValue = null;

		if (value.length() == 0) {
			previousValue = dataCache.remove(key);
		} else {
			previousValue = dataCache.put(key, value);
		}

		return ((previousValue == null) || (previousValue.equals(value) == false));
	}

	public void rebroadcastReceivedMessages() {
		Enumeration<String> e = dataCache.keys();
		while (e.hasMoreElements()) {
			String nextKey = e.nextElement();
			String nextValue = dataCache.get(nextKey);

			broadcastReceivedMessage(nextKey, nextValue);
		}
	}

	private String generateClientId() {

		if (mqttClientId == null) {
			String timestamp = "" + (new Date()).getTime();
			String android_id = Settings.System.getString(getContentResolver(), Secure.ANDROID_ID);
			mqttClientId = timestamp + android_id;
			if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
				mqttClientId = mqttClientId.substring(0,
						MAX_MQTT_CLIENTID_LENGTH);
			}
		}

		return mqttClientId;
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		}

		return false;
	}
	
	private static int[] convertIntegers(List<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    Iterator<Integer> iterator = integers.iterator();
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().intValue();
	    }
	    return ret;
	}
}
