package com.example.test_1;

import com.google.android.gcm.GCMBaseIntentService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
 
public class GCMIntentService extends GCMBaseIntentService {
 
    @Override
    protected void onError(Context arg0, String arg1) {
        Log.e("Registration", "Got an error!");
        Log.e("Registration", arg0.toString() + arg1.toString());
    }
 
    @Override
    protected void onMessage(Context arg0, Intent arg1) {
        Log.i("Registration", "Got a message!");
        Log.i("Registration", arg0.toString() + " " + arg1.toString());
        
		Bundle extras = arg1.getExtras();
		
		Log.i("Registration", extras.toString());
		String value = "";
		if (extras != null) {
		    value = extras.getString("message");
		}

        Intent myIntent = new Intent(getApplicationContext(), FullscreenActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra("value", value);
        startActivity(myIntent);

        // Note: this is where you would handle the message and do something in your app.
    }
 
    @Override
    protected void onRegistered(Context arg0, String arg1) {
        Log.i("Registration", "Just registered!");
        Log.i("Registration", arg0.toString() + arg1.toString());   
        // This is where you need to call your server to record the device toekn and registration id.
    }
 
    @Override
    protected void onUnregistered(Context arg0, String arg1) {
    }
}