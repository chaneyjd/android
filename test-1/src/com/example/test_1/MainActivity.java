package com.example.test_1;

import com.google.android.gcm.GCMRegistrar;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	private String SENDER_ID = "236431556452";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		RegisterWithGCM();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void RegisterWithGCM()
	{           
	    GCMRegistrar.checkDevice(this);
	    GCMRegistrar.checkManifest(this);
	    final String regId = GCMRegistrar.getRegistrationId(this);
	    if (regId.equals("")) {
	      GCMRegistrar.register(this, SENDER_ID); // Note: get the sender id from configuration.
	    } else {
	      Log.v("Registration", "Already registered, regId: " + regId);
	    }
	    
        Intent myIntent = new Intent(getApplicationContext(), FullscreenActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra("value", regId);
        startActivity(myIntent);
	}
	
	public void testClick(View v) {
        Intent myIntent = new Intent(getApplicationContext(), OpenGLES20.class);
        //myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //myIntent.putExtra("value", "Test Click");
        startActivity(myIntent);
	}
}
