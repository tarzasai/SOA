package net.esorciccio.soa;

import java.lang.reflect.Method;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class OAReceiver extends BroadcastReceiver {
	public static final String REQ_CC = "net.esorciccio.soa.REQUEST_CACHE_CLEAR";
	public static final String REQ_VD = "net.esorciccio.soa.REQUEST_VOLUME_DOWN";
	public static final String REQ_VU = "net.esorciccio.soa.REQUEST_VOLUME_UP";
	public static final String REQ_E3 = "net.esorciccio.soa.REQUEST_DIALOG_TRE";
	
	private static final int NOTIF_LEAVE = 1;
	private static final int NOTIF_BLUNC = 2;
	private static final int NOTIF_ELUNC = 3;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	private static final int VFLAGS = AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI;
	
	private static AudioManager audioMan(Context context) {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		OASession session = OASession.getInstance(context);
		String act = intent.getAction();
		Log.v(getClass().getSimpleName(), act);
		if (act.equals("android.intent.action.BOOT_COMPLETED")) {
			session.checkAlarms();
		} else if (act.equals("android.net.wifi.SCAN_RESULTS")) {
			boolean res = false;
			Set<String> ssids = session.getWifiSet();
			for (ScanResult sr : ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults())
				if (ssids.contains(sr.SSID)) {
					res = true;
					break;
				}
			session.setInOffice(res);
		} else if (act.equals(OASession.AC.BLUNC)) {
			nm.notify(NOTIF_BLUNC, getNotif(context, R.string.msg_blunc_title, R.string.msg_blunc_text));
		} else if (act.equals(OASession.AC.ELUNC)) {
			nm.notify(NOTIF_ELUNC, getNotif(context, R.string.msg_elunc_title, R.string.msg_elunc_text));
			nm.cancel(NOTIF_BLUNC);
		} else if (act.equals(OASession.AC.LEAVE)) {
			nm.notify(NOTIF_LEAVE, getNotif(context, context.getString(R.string.msg_leave_title),
				context.getString(R.string.msg_leave_text) + " " + DateUtils.getRelativeTimeSpanString(
				session.getLeaving(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()));
			nm.cancel(NOTIF_BLUNC);
			nm.cancel(NOTIF_ELUNC);
		} else if (act.equals(REQ_CC)) {
			PackageManager pm = context.getPackageManager();
			for (Method m : pm.getClass().getDeclaredMethods())
				if (m.getName().equals("freeStorageAndNotify")) {
					try {
						m.invoke(pm, Integer.MAX_VALUE, null);
						m.invoke(pm, Long.MAX_VALUE, null);
					} catch (Exception e) {
						Log.e(this.getClass().getSimpleName(), "onReceive", e); // permission problem?
					}
					break;
				}
			Toast.makeText(context, "request sent", Toast.LENGTH_SHORT).show();
		} else if (act.equals(REQ_VU)) {
			audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, VFLAGS);
		} else if (act.equals(REQ_VD)) {
			audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, VFLAGS);
		} else if (act.equals(REQ_E3)) {
			String info = DateUtils.getRelativeTimeSpanString(TreActivity.lastrun, System.currentTimeMillis(),
				DateUtils.MINUTE_IN_MILLIS).toString();
			if (!session.isLast3failed())
				Toast.makeText(context, info + ": ok", Toast.LENGTH_SHORT).show();
			else {
				Toast.makeText(context, info + ": " + session.getLast3fail(), Toast.LENGTH_LONG).show();
				TreActivity.lastrun = 0;
			}
		}
	}
	
	private static Notification getNotif(Context context, int title, int text) {
		return getNotif(context, context.getString(title), context.getString(text));
	}
	
	private static Notification getNotif(Context context, String title, String text) {
		Toast.makeText(context, title, Toast.LENGTH_LONG).show();
		return new NotificationCompat.Builder(context).setSound(NOTIF_SOUND).setSmallIcon(
			R.drawable.notif_alarm).setContentTitle(title).setContentText(text).build();
	}
}