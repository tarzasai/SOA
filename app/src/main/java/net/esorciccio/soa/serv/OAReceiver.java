package net.esorciccio.soa.serv;

import net.esorciccio.soa.R;
import net.esorciccio.soa.serv.OASession.WR;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class OAReceiver extends BroadcastReceiver {
	private static final String TAG = "OAReceiver";

	private static final int NOTIF_ENTER = 1;
	private static final int NOTIF_LEAVE = 2;
	private static final int NOTIF_BLUNC = 3;
	private static final int NOTIF_ELUNC = 4;
	private static final int NOTIF_CLEAN = 5;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	private static final int VFLAGS = AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI;

	private static AudioManager audioMan(Context context) {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	private static Notification getNotif(Context context, int title, int text) {
		return getNotif(context, context.getString(title), context.getString(text));
	}

	private static Notification getNotif(Context context, String title, String text) {
		Toast.makeText(context, title, Toast.LENGTH_LONG).show();
		return new NotificationCompat.Builder(context).setSound(NOTIF_SOUND).setSmallIcon(R.drawable.notif_alarm)
			.setContentTitle(title).setContentText(text).build();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		OASession session = OASession.getInstance(context);
		String act = intent.getAction();
		Log.v(TAG, act);
		switch (act) {
			// messaggi dal sistema
			case "android.intent.action.BOOT_COMPLETED":
				session.setLastWiFiScan(null);
				session.checkNetwork();
				break;
			case "android.net.conn.CONNECTIVITY_CHANGE":
			case "android.net.wifi.WIFI_STATE_CHANGED":
			case "android.net.wifi.STATE_CHANGE":
				session.checkNetwork();
				break;
			case "android.net.wifi.SCAN_RESULTS":
				session.setLastWiFiScan(((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults());
				break;
			// questi vengono dai timer
			case OASession.AC.ENTER:
				nm.notify(NOTIF_ENTER, getNotif(context, context.getString(R.string.msg_enter_title),
					context.getString(R.string.msg_enter_text) + " " +
						DateUtils.getRelativeTimeSpanString(session.getLeaving(), System.currentTimeMillis(),
							DateUtils.MINUTE_IN_MILLIS).toString()));
				break;
			case OASession.AC.LEAVE:
				nm.notify(NOTIF_LEAVE, getNotif(context, context.getString(R.string.msg_leave_title),
					context.getString(R.string.msg_leave_text) + " " +
						DateUtils.getRelativeTimeSpanString(session.getLeaving(), System.currentTimeMillis(),
							DateUtils.MINUTE_IN_MILLIS).toString()));
				nm.cancel(NOTIF_BLUNC);
				nm.cancel(NOTIF_ELUNC);
				break;
			case OASession.AC.LEFTW:
				nm.cancel(NOTIF_ENTER);
				nm.cancel(NOTIF_LEAVE);
				nm.cancel(NOTIF_BLUNC);
				nm.cancel(NOTIF_ELUNC);
				break;
			case OASession.AC.BLUNC:
				nm.notify(NOTIF_BLUNC, getNotif(context, R.string.msg_blunc_title, R.string.msg_blunc_text));
				break;
			case OASession.AC.ELUNC:
				nm.notify(NOTIF_ELUNC, getNotif(context, R.string.msg_elunc_title, R.string.msg_elunc_text));
				nm.cancel(NOTIF_BLUNC);
				break;
			case OASession.AC.CLEAN:
				nm.notify(NOTIF_CLEAN, getNotif(context, R.string.msg_clean_title, R.string.msg_clean_text));
				break;
			// comandi dal widget/shortcut
			case WR.VOLUME_UP:
				audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, VFLAGS);
				break;
			case WR.VOLUME_DOWN:
				audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, VFLAGS);
				break;
		}
	}
}