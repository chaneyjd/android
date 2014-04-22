package com.citi.example.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

import com.citi.example.mqtt.R;
import com.citi.example.mqtt.service.MainService;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.provider.Telephony;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private StatusUpdateReceiver statusUpdateIntentReceiver;
	private MQTTMessageReceiver messageIntentReceiver;
	private MainService mainsvc;
	private boolean mIsBound;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
	        mainsvc = ((MainService.LocalBinder<MainService>)service).getService();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        mainsvc = null;
	    }
	};

	void doBindService() {
	    bindService(new Intent(this, MainService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	void doUnbindService() {
	    if (mIsBound) {
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}

    
	public void popup(View view) {
		JSONObject json = new JSONObject();
		try {
			json.put("id", Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
			json.put("action", "transfer");
			json.put("from_account", "1111222233334444");
			json.put("to_account", "5555666677778888");
			json.put("message", "Please help");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		mainsvc.publishMessageToTopic("com.citi.example.inbox", json.toString());

//		showMyDialog("title", "message");
		
//		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//		alertDialog.setTitle("Reset...");
//		alertDialog.setMessage("Are you sure?");
//		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
//		public void onClick(DialogInterface dialog, int which) {
//		// here you can add functions
//		}
//		});
//		alertDialog.setIcon(R.drawable.ic_citi);
//		alertDialog.show();
		
//		Intent intent = new Intent(this, MainActivityNotify.class);
//		startActivity(intent);
	}
//	public void disconnect(View view) {
//		Intent svc = new Intent(this, MainService.class);
//		stopService(svc);
//	}

	public void connect(View view) {
		Intent svc = new Intent(this, MainService.class);
//		stopService(svc);

		String udid = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);

		SharedPreferences settings = getSharedPreferences(MainService.APP_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		
//		EditText mTextHost = (EditText)findViewById(R.id.hostName);
//		editor.putString("broker", mTextHost.getText().toString());
		editor.putString("broker", "jasonchaney.info");
		editor.putString("topic", "com/citi/example/messages");
		editor.putString("topic2", "com/citi/example/android/" + udid);
		editor.commit();
		
		startService(svc);
		doBindService();
		
		Button mButton = (Button)findViewById(R.id.button1);		
		mButton.setEnabled(false);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mIsBound) {
			Button mButton = (Button)findViewById(R.id.button1);		
			mButton.setEnabled(!mainsvc.isAlreadyConnected());
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mIsBound) {
			Button mButton = (Button)findViewById(R.id.button1);		
			mButton.setEnabled(!mainsvc.isAlreadyConnected());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		

		statusUpdateIntentReceiver = new StatusUpdateReceiver();
		IntentFilter intentSFilter = new IntentFilter(MainService.MQTT_STATUS_INTENT);
		registerReceiver(statusUpdateIntentReceiver, intentSFilter);

		messageIntentReceiver = new MQTTMessageReceiver();
		IntentFilter intentCFilter = new IntentFilter(MainService.MQTT_MSG_RECEIVED_INTENT);
		registerReceiver(messageIntentReceiver, intentCFilter);
		
		try {
			ContentValues values = new ContentValues();
			values.put(Telephony.Sms.ADDRESS, "9999999999");
			values.put(Telephony.Sms.BODY, "Test Status");
			getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
		} catch (Exception e) {
			Log.d("DEBUG",e.getStackTrace().toString());
		}
		
		connect(class.MainActivity);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public class StatusUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle notificationData = intent.getExtras();
			String newStatus = notificationData
					.getString(MainService.MQTT_STATUS_MSG);
			
//			TextView t = new TextView(context);
//			t = (TextView) findViewById(R.id.textView1);
//			t.append(newStatus + "\n");
			
			showMyDialog(newStatus, newStatus);
			
//			ContentValues values = new ContentValues();
//			values.put(Telephony.Sms.ADDRESS, "999-999-9999");
//			values.put(Telephony.Sms.BODY, newStatus);
//			getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
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
			//TextView t = new TextView(context);
			//t = (TextView) findViewById(R.id.textView1);
			//t.append("(" + newTopic + ") = " + newData + "\n");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(statusUpdateIntentReceiver);
		unregisterReceiver(messageIntentReceiver);

		Intent svc = new Intent(this, MainService.class);
		stopService(svc);
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
//		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//		alertDialog.setTitle(title);
//		alertDialog.setMessage(message);
//		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
//		public void onClick(DialogInterface dialog, int which) {
//		// here you can add functions
//		}
//		});
//		alertDialog.setIcon(R.drawable.ic_citi);
//		alertDialog.show();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
//		mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
//	    mDialog.getWindow().getAttributes().privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS; 
		
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ic_citi);
        builder.setNegativeButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
