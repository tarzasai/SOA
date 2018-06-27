package net.ggelardi.soa.serv;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.ggelardi.soa.R;
import net.ggelardi.soa.SettingsActivity;

public class OAService extends IntentService {
	private static final String TAG = "OAService";

	private static final String NOTIF_CHANNEL = "SOA_NOTIFICATION_CHANNEL";
	private static final int NOTIF_ID = 1199;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

	public OAService() {
		super("OAService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		Log.v(TAG, "onStartCommand");

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.v(TAG, "onDestroy");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String act = intent != null ? intent.getAction() : "asd";
		Log.v(TAG, act);
		OASession session = OASession.getInstance(this);
		switch (act) {
			case "android.intent.action.BOOT_COMPLETED":
				session.setLastWiFiScan(null);
				session.requestWifiScan(false);
				break;
			case "android.net.conn.CONNECTIVITY_CHANGE":
			case "android.net.wifi.WIFI_STATE_CHANGED":
			case "android.net.wifi.STATE_CHANGE":
				session.requestWifiScan(false);
				session.checkAlarms();
				break;
			case "android.net.wifi.SCAN_RESULTS":
				session.setLastWiFiScan(((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).getScanResults());
				break;
			case OASession.AC.CHECK:
				session.requestWifiScan(true);
				session.checkAlarms();
				break;
			case OASession.AC.ENTER:
			case OASession.AC.LEAVE:
			case OASession.AC.LEFTW:
			case OASession.AC.BLUNC:
			case OASession.AC.ELUNC:
				// continue to update the notification
				break;
			default:
				return;
		}
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		long at = session.getArrival();
		if (at <= 0) {
			nm.cancelAll();
			return;
		}
		//
		long lt = session.getLeft();
		long bt = session.getLunchBegin();
		long et = session.getLunchEnd();
		long st = System.currentTimeMillis();
		boolean atLunch = bt > 0 && st >= bt && et > st;
		boolean atWork = at > 0 && (lt <= 0 || atLunch);
		if (!atWork) {
			nm.cancelAll();
			return;
		}
		//
		if (nm.getNotificationChannel(NOTIF_CHANNEL) == null) {
			AudioAttributes audioAttrs = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_NOTIFICATION)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();
			NotificationChannel nc = new NotificationChannel(NOTIF_CHANNEL, "SOA Alarms",
					NotificationManager.IMPORTANCE_HIGH);
			nc.setDescription("All SOA notifications");
			nc.setSound(NOTIF_SOUND, audioAttrs);
			nc.setBypassDnd(true);
			nm.createNotificationChannel(nc);
		}
		NotificationCompat.Builder nb = new NotificationCompat.Builder(this, NOTIF_CHANNEL);
		nb.setCategory(NotificationCompat.CATEGORY_STATUS);
		//nb.setPriority(NotificationCompat.PRIORITY_HIGH);
		nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		nb.setSmallIcon(R.drawable.ic_notif_small);
		nb.setOnlyAlertOnce(true);
		nb.setAutoCancel(false);
		nb.setOngoing(true);
		lt = session.getLeaving();
		if (atLunch) {
			nb.setContentTitle(getString(R.string.notif_lunch_title));
			nb.setContentText(String.format(getString(R.string.notif_lunch_text), OASession.timeString(et)));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_lunch));
		} else if (st >= lt) {
			nb.setContentTitle(getString(R.string.notif_gohome_title));
			nb.setContentText(String.format(getString(R.string.notif_gohome_text), OASession.timeString(lt)));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_home));
		} else {
			nb.setContentTitle(getString(R.string.notif_atwork_title));
			nb.setContentText(String.format(getString(R.string.notif_atwork_text), OASession.timeString(at),
				OASession.timeString(lt)));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_work));
		}
		Intent ri = new Intent(this, SettingsActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
		nb.setContentIntent(pi);
		nm.notify(NOTIF_ID, nb.build());
	}
}
