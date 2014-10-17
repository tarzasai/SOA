package net.esorciccio.goa;

import java.util.Set;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class OAReceiver extends BroadcastReceiver {
	public static final int NOTIF_LEAVE_ID = 1;
	public static final int NOTIF_BLUNC_ID = 2;
	public static final int NOTIF_ELUNC_ID = 3;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		OASession session = OASession.getInstance(context);
		Uri snd = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		String act = intent.getAction();
		Log.v(getClass().getSimpleName(), act);
		if (act.equals("android.intent.action.BOOT_COMPLETED")) {
			session.checkAlarms();
		} else if (act.equals("android.net.wifi.SCAN_RESULTS")) {
			WifiManager wifiman = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			Set<String> ssids = session.getWifiSet();
			boolean res = false;
			for (ScanResult sr : wifiman.getScanResults())
				if (ssids.contains(sr.SSID)) {
					res = true;
					break;
				}
			session.setInOffice(res);
		} else if (act.equals(OASession.AC.BLUNC)) {
			NotificationCompat.Builder ncb = new NotificationCompat.Builder(context).setSound(snd).setSmallIcon(
				R.drawable.notif_alarm).setContentTitle(context.getString(R.string.msg_blunc_title)).setContentText(
				context.getString(R.string.msg_blunc_text));
			NotificationManager nmg = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			nmg.notify(NOTIF_BLUNC_ID, ncb.build());
			Toast.makeText(context, R.string.msg_blunc_title, Toast.LENGTH_LONG).show();
		} else if (act.equals(OASession.AC.ELUNC)) {
			NotificationCompat.Builder ncb = new NotificationCompat.Builder(context).setSound(snd).setSmallIcon(
				R.drawable.notif_alarm).setContentTitle(context.getString(R.string.msg_elunc_title)).setContentText(
				context.getString(R.string.msg_elunc_text));
			NotificationManager nmg = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			nmg.notify(NOTIF_ELUNC_ID, ncb.build());
			nmg.cancel(NOTIF_BLUNC_ID);
			Toast.makeText(context, R.string.msg_elunc_title, Toast.LENGTH_LONG).show();
		} else if (act.equals(OASession.AC.LEAVE)) {
			NotificationCompat.Builder ncb = new NotificationCompat.Builder(context).setSound(snd).setSmallIcon(
				R.drawable.notif_alarm).setContentTitle(context.getString(R.string.msg_leave_title)).setContentText(
				context.getString(R.string.msg_leave_text) + DateUtils.getRelativeTimeSpanString(
				session.getLeaving(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString());
			NotificationManager nmg = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			nmg.notify(NOTIF_LEAVE_ID, ncb.build());
			nmg.cancel(NOTIF_BLUNC_ID);
			nmg.cancel(NOTIF_ELUNC_ID);
			Toast.makeText(context, R.string.msg_leave_title, Toast.LENGTH_LONG).show();
		}
	}
}