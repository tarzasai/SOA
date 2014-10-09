package net.esorciccio.goa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OAReceiver extends BroadcastReceiver {
	
	private OASession session;
	
	public OAReceiver() {
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		session = OASession.getInstance(context);
		
	}
}