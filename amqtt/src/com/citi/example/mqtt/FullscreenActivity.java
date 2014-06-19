package com.citi.example.mqtt;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

//import com.citi.example.mqtt.MainActivity.MQTTMessageReceiver;
//import com.citi.example.mqtt.MainActivity.RandomString;
//import com.citi.example.mqtt.MainActivity.StatusUpdateReceiver;
import com.citi.example.mqtt.service.MainService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.widget.EditText;

public class FullscreenActivity extends Activity {

	private StatusUpdateReceiver statusUpdateIntentReceiver;
	private MQTTMessageReceiver messageIntentReceiver;
	private MainService mainsvc;
	private boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			mainsvc = ((MainService.LocalBinder<MainService>) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			mainsvc = null;
		}
	};

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen);

		statusUpdateIntentReceiver = new StatusUpdateReceiver();
		IntentFilter intentSFilter = new IntentFilter(
				MainService.MQTT_STATUS_INTENT);
		registerReceiver(statusUpdateIntentReceiver, intentSFilter);

		messageIntentReceiver = new MQTTMessageReceiver();
		IntentFilter intentCFilter = new IntentFilter(
				MainService.MQTT_MSG_RECEIVED_INTENT);
		registerReceiver(messageIntentReceiver, intentCFilter);

		bindService(new Intent(this, MainService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
		startService(new Intent(this, MainService.class));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		delayedHide(100);
	}

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	public class StatusUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle notificationData = intent.getExtras();
			String newStatus = notificationData
					.getString(MainService.MQTT_STATUS_MSG);
			showMyDialog(newStatus, newStatus);
		}
	}

	public class MQTTMessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle notificationData = intent.getExtras();
			String newTopic = notificationData
					.getString(MainService.MQTT_MSG_RECEIVED_TOPIC);
			String newData = notificationData
					.getString(MainService.MQTT_MSG_RECEIVED_MSG);
			showMyDialog(newTopic, newData);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(statusUpdateIntentReceiver);
		unregisterReceiver(messageIntentReceiver);
		// stopService(new Intent(this, MainService.class));
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNotificationManager.cancel(MainService.MQTT_NOTIFICATION_UPDATE);
		}
	}

	public void showMyDialog(String title, String message) {
		try {
			JSONObject json = new JSONObject(message);

			// Payment Due Alert
			if (json.get("action").toString().equals("paymentdue")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				String pos = getDefault(json, "pos", "OK");
				String neg = getDefault(json, "neg", "Cancel");
				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());
//				builder.setCancelable(true);
				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								dialog.dismiss();
							}
						});
				builder.setNegativeButton(neg,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								dialog.dismiss();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
			
			if (json.get("action").toString().equals("spend_pos")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				AlertDialog.Builder builder_pos = new AlertDialog.Builder(this);
				final AlertDialog alert_pos = builder_pos.create();
				String pos = getDefault(json, "pos", "OK");

				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());

				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								alert_pos.show();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}

			if (json.get("action").toString().equals("spend_neg")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				AlertDialog.Builder builder_pos = new AlertDialog.Builder(this);
				final AlertDialog alert_pos = builder_pos.create();
				String pos = getDefault(json, "pos", "OK");

				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());

				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								alert_pos.show();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}

			if (json.get("action").toString().equals("spend")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
//				AlertDialog.Builder builder_pos = new AlertDialog.Builder(this);
//				AlertDialog.Builder builder_neg = new AlertDialog.Builder(this);
//
//				builder_pos.setTitle(" ");
//				builder_pos.setMessage("Your purchase has been authorized, thank you for your continued patronage");
//				builder_pos.setIcon(R.drawable.ic_citi);
//
//				builder_pos.setPositiveButton("Ok",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//								dialog.cancel();
//							}
//						});
//
//				builder_neg.setTitle(" ");
//				builder_neg.setMessage("Your purchase has been denied, you will be contacted by Citi to resolve this disputed charge");
//				builder_neg.setIcon(R.drawable.ic_citi);
//
//				builder_neg.setPositiveButton("Ok",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//								dialog.cancel();
//							}
//						});

//				final AlertDialog alert_pos = builder_pos.create();
//				final AlertDialog alert_neg = builder_neg.create();

				String pos = getDefault(json, "pos", "OK");
				String neg = getDefault(json, "neg", "Cancel");

				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());

//				builder.setCancelable(true);
				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								
								JSONObject json = new JSONObject();
								try {
									json.put("id", "android/" + Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
									json.put("action", "spend_pos");
									json.put("message", "Card Activated");
//									json.put("duedate", RandomString.nextString(1) + "/12/2014");
//									json.put("amount",RandomString.nextString(3) + "." + RandomString.nextString(2)); 
//									json.put("account", RandomString.nextString(16));
								} catch (JSONException e) {
									e.printStackTrace();
								}
								mainsvc.publishMessageToTopic(json.toString());

//								alert_pos.show();
							}
						});
				builder.setNegativeButton(neg,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								
								JSONObject json = new JSONObject();
								try {
									json.put("id", "android/" + Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
									json.put("action", "spend_neg");
									json.put("message", "Card Activated");
//									json.put("duedate", RandomString.nextString(1) + "/12/2014");
//									json.put("amount",RandomString.nextString(3) + "." + RandomString.nextString(2)); 
//									json.put("account", RandomString.nextString(16));
								} catch (JSONException e) {
									e.printStackTrace();
								}
								mainsvc.publishMessageToTopic(json.toString());

//								alert_neg.show();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}

			if (json.get("action").toString().equals("activate-end")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				String pos = getDefault(json, "pos", "OK");

				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());

//				builder.setCancelable(true);
				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}

			if (json.get("action").toString().equals("activate1")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				AlertDialog.Builder builder_pos = new AlertDialog.Builder(this);
//				AlertDialog.Builder builder_neg = new AlertDialog.Builder(this);

//				builder_neg.setTitle(" ");
//				builder_neg.setMessage("Your card is now activated, Thank You for your business.");
//				builder_neg.setIcon(R.drawable.ic_citi);
//
//				builder_neg.setPositiveButton("OK",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//							}
//						});

//				final AlertDialog alert_neg = builder_neg.create();
				
				final EditText input = new EditText(this);
				builder_pos.setView(input);

				builder_pos.setTitle(" ");
				builder_pos.setMessage("Please enter the last four digits of your Social Security Number");
				builder_pos.setIcon(R.drawable.ic_citi);

				builder_pos.setPositiveButton("Activate",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
//								String value = input.getText().toString();
								dialog.cancel();
//								alert_neg.show();
								
								JSONObject json = new JSONObject();
								try {
									json.put("id", "android/" + Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
									json.put("action", "activatecard2");
									json.put("message", "Card Activated");
									json.put("ssn", input.getText());
									json.put("duedate", RandomString.nextString(1) + "/12/2014");
									json.put("amount",RandomString.nextString(3) + "." + RandomString.nextString(2)); 
									json.put("account", RandomString.nextString(16));
								} catch (JSONException e) {
									e.printStackTrace();
								}
								mainsvc.publishMessageToTopic(json.toString());
								
							}
						});
				builder_pos.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

				final AlertDialog alert_pos = builder_pos.create();

				String pos = getDefault(json, "pos", "OK");
				String neg = getDefault(json, "neg", "Cancel");

				builder.setTitle(" ");
				builder.setMessage(json.get("message").toString());

//				builder.setCancelable(true);
				builder.setIcon(R.drawable.ic_citi);
				builder.setPositiveButton(pos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								alert_pos.show();
							}
						});
				builder.setNegativeButton(neg,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getDefault(JSONObject json, String key, String def) {
		String retval = def;
		try {
			retval = json.get(key).toString();
		} catch (Exception e) {
		}
		return retval;
	}

	public static class RandomString {
		public static String nextString(int length) {
			char[] buf;
			Random random = new Random();
			StringBuilder tmp = new StringBuilder();
			for (char ch = '0'; ch <= '9'; ++ch)
				tmp.append(ch);
			char[] symbols = tmp.toString().toCharArray();

			buf = new char[length];

			for (int idx = 0; idx < buf.length; ++idx)
				buf[idx] = symbols[random.nextInt(symbols.length)];
			return new String(buf);
		}
	}
}
