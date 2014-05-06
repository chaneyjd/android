package com.citi.example.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

import com.citi.example.mqtt.R;
import com.citi.example.mqtt.service.MainService;

import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
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
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends Activity {

	private StatusUpdateReceiver statusUpdateIntentReceiver;
	private MQTTMessageReceiver messageIntentReceiver;
	private MainService mainsvc;
	private boolean mIsBound;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
	        mainsvc = ((MainService.LocalBinder<MainService>)service).getService();
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
    
	public void popup(View view) {
		JSONObject json = new JSONObject();
		try {
			EditText mAmount = (EditText)findViewById(R.id.editText1);
			Spinner mFrom = (Spinner)findViewById(R.id.spinner1);
			Spinner mTo = (Spinner)findViewById(R.id.spinner2);

			json.put("id", Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
			json.put("action", "transfer");
			json.put("from_account", mFrom.getSelectedItem().toString());
			json.put("to_account", mTo.getSelectedItem().toString());
			json.put("message", mAmount.getText().toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		mainsvc.publishMessageToTopic(json.toString());
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
//		assetsPropertyReader = new AssetPropertyReader(this);
//      p = assetsPropertyReader.getProperties("amqtt.properties");

		statusUpdateIntentReceiver = new StatusUpdateReceiver();
		IntentFilter intentSFilter = new IntentFilter(MainService.MQTT_STATUS_INTENT);
		registerReceiver(statusUpdateIntentReceiver, intentSFilter);

		messageIntentReceiver = new MQTTMessageReceiver();
		IntentFilter intentCFilter = new IntentFilter(MainService.MQTT_MSG_RECEIVED_INTENT);
		registerReceiver(messageIntentReceiver, intentCFilter);
		
	    bindService(new Intent(this, MainService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
		startService(new Intent(this, MainService.class));
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
			String newStatus = notificationData.getString(MainService.MQTT_STATUS_MSG);
			showMyDialog(newStatus, newStatus);
		}
	}

	public class MQTTMessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle notificationData = intent.getExtras();
			String newTopic = notificationData.getString(MainService.MQTT_MSG_RECEIVED_TOPIC);
			String newData = notificationData.getString(MainService.MQTT_MSG_RECEIVED_MSG);
			showMyDialog(newTopic, newData);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(statusUpdateIntentReceiver);
		unregisterReceiver(messageIntentReceiver);
		stopService(new Intent(this, MainService.class));
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" ");
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setIcon(R.drawable.ic_citi);
        builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                Intent intent = new Intent(MainActivity.this, FullscreenActivity.class);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
