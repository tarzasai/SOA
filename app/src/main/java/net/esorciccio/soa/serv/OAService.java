package net.esorciccio.soa.serv;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import net.esorciccio.soa.R;
import net.esorciccio.soa.SettingsActivity;

public class OAService extends IntentService {
	private static final String TAG = "OAService";

	private static final int NOTIF_STATUS = 1199;
	private static final String ACTION_ADD1MIN = "OAService.ACTION_ADD1MIN";
	private static final String ACTION_DEL1MIN = "OAService.ACTION_DEL1MIN";

	private static final int NOTIF_ENTER = 1;
	private static final int NOTIF_LEAVE = 2;
	private static final int NOTIF_BLUNC = 3;
	private static final int NOTIF_ELUNC = 4;
	private static final int NOTIF_CLEAN = 5;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	private static final int VFLAGS = AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI;

	private OASession session;
	private NotificationManager notifmn;

	private static AudioManager audioMan(Context context) {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	private static Notification getNotif(Context context, int title, int text) {
		return getNotif(context, context.getString(title), context.getString(text));
	}

	private static Notification getNotif(Context context, String title, String text) {
		Toast.makeText(context, title, Toast.LENGTH_LONG).show();
		return new NotificationCompat.Builder(context).setSound(NOTIF_SOUND).setSmallIcon(R.drawable.notif_alarm)
			.setContentTitle(title).setContentText(text).setAutoCancel(true).build();
	}

	public OAService() {
		super("OAService");

		session = OASession.getInstance(this);
		notifmn = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String act = intent != null ? intent.getAction() : null;
		Log.v(TAG, act);
		switch (act) {
			// messaggi dal sistema
			case "android.intent.action.BOOT_COMPLETED":
				notifService();
				session.setLastWiFiScan(null);
				session.checkNetwork();
				break;
			case "android.net.conn.CONNECTIVITY_CHANGE":
			case "android.net.wifi.WIFI_STATE_CHANGED":
			case "android.net.wifi.STATE_CHANGE":
				notifService();
				session.checkNetwork();
				break;
			case "android.net.wifi.SCAN_RESULTS":
				session.setLastWiFiScan(((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).getScanResults());
				break;
			// questi vengono dai timer
			case OASession.AC.ENTER:
				notifService();
				notifmn.notify(NOTIF_ENTER, getNotif(this, this.getString(R.string.msg_enter_title),
					this.getString(R.string.msg_enter_text) + " " + OASession.timeString(session.getLeaving())));
				break;
			case OASession.AC.LEAVE:
				notifService();
				notifmn.notify(NOTIF_LEAVE, getNotif(this, this.getString(R.string.msg_leave_title),
					this.getString(R.string.msg_leave_text) + " " +
						DateUtils.getRelativeTimeSpanString(session.getLeaving(), System.currentTimeMillis(),
							DateUtils.MINUTE_IN_MILLIS).toString()));
				notifmn.cancel(NOTIF_BLUNC);
				notifmn.cancel(NOTIF_ELUNC);
				break;
			case OASession.AC.LEFTW:
				notifService();
				notifmn.cancel(NOTIF_ENTER);
				notifmn.cancel(NOTIF_LEAVE);
				notifmn.cancel(NOTIF_BLUNC);
				notifmn.cancel(NOTIF_ELUNC);
				break;
			case OASession.AC.BLUNC:
				notifmn.notify(NOTIF_BLUNC, getNotif(this, R.string.msg_blunc_title, R.string.msg_blunc_text));
				break;
			case OASession.AC.ELUNC:
				notifmn.notify(NOTIF_ELUNC, getNotif(this, R.string.msg_elunc_title, R.string.msg_elunc_text));
				notifmn.cancel(NOTIF_BLUNC);
				break;
			case OASession.AC.CLEAN:
				notifmn.notify(NOTIF_CLEAN, getNotif(this, R.string.msg_clean_title, R.string.msg_clean_text));
				break;
		}
	}

	private void notifService() {
		// https://developer.android.com/design/patterns/notifications.html
		NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
		nb.setCategory(NotificationCompat.CATEGORY_SERVICE);
		nb.setPriority(NotificationCompat.PRIORITY_HIGH);
		nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		nb.setSmallIcon(R.drawable.ic_launcher);
		nb.setAutoCancel(false);
		Intent ri;
		PendingIntent pi;
		if (session.getAtHome()) {
			nb.setContentTitle(getString(R.string.notif_athome));
			nb.setContentText(null);
		} else if (!session.getAtWork()) {
			nb.setContentTitle(getString(R.string.notif_roamin));
			nb.setContentText(null);
		} else {
			nb.setContentTitle(getString(R.string.notif_atwork));
			StringBuilder sb = new StringBuilder();
			sb.append(String.format(getString(R.string.notif_tenter), OASession.timeString(session.getArrival())));
			sb.append(" - ");
			long lt = session.getLeft();
			if (lt > 0)
				sb.append(String.format(getString(R.string.notif_tleft), OASession.timeString(lt)));
			else {
				lt = session.getLeaving();
				sb.append(String.format(getString(R.string.notif_tleave), OASession.timeString(lt)));
				//
				ri = new Intent(this, OAService.class);
				ri.setAction(OAService.ACTION_ADD1MIN);
				pi = PendingIntent.getService(this, 0, ri, 0);
				nb.addAction(R.drawable.ic_stat_add_1m, "", pi);
				//
				ri = new Intent(this, OAService.class);
				ri.setAction(OAService.ACTION_DEL1MIN);
				pi = PendingIntent.getService(this, 0, ri, 0);
				nb.addAction(R.drawable.ic_stat_del_1m, "", pi);
			}
			nb.setContentText(sb.toString());
		}
		//
		ri = new Intent(this, SettingsActivity.class);
		pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
		nb.setContentIntent(pi);
		//
		startForeground(NOTIF_STATUS, nb.build());
	}
}
