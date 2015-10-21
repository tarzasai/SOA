package net.esorciccio.soa.serv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OAReceiver extends BroadcastReceiver {
	private static final String TAG = "OAReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		WakefulIntentService.sendWakefulWork(context, new Intent(context, OAService.class).setAction(intent.getAction()));
	}
}