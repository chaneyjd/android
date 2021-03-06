package com.citi.example.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

import com.citi.example.mqtt.service.MainService;

import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class ActivateActivity extends Activity {
	
	private MainService mainsvc;
	private boolean mIsBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activate);
		
	    bindService(new Intent(this, MainService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activate, menu);
		return true;
	}
	
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
    
	public void transfer(View v) {
		JSONObject json = new JSONObject();
		JSONObject json2 = new JSONObject();
		try {
			EditText mAmount = (EditText)findViewById(R.id.editTextAmount);
			Spinner mFrom = (Spinner)findViewById(R.id.spinnerFrom);
			Spinner mTo = (Spinner)findViewById(R.id.spinnerTo);

			json.put("id", Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
			json.put("action", "transfer");
			json.put("account_from", mFrom.getSelectedItem().toString());
			json.put("account_to", mTo.getSelectedItem().toString());
			json.put("message", mAmount.getText().toString());
			
			json2.put("1", "one");
			json2.put("2", "two");
			
			json.put("test", json2);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		mainsvc.publishMessageToTopic(json.toString());
	}
}
