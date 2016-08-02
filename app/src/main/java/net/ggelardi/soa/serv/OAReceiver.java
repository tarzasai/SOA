package net.ggelardi.soa.serv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OAReceiver extends BroadcastReceiver {
	private static final String TAG = "OAReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService(new Intent(context, OAService.class).setAction(intent.getAction()));
	}
}