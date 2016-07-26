package net.esorciccio.soa.serv;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.esorciccio.soa.MainActivity;
import net.esorciccio.soa.R;

public class OAService extends IntentService {
	private static final String TAG = "OAService";

	private static final int NOTIF_STATUS = 1199;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

	public OAService() {
		super("OAService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String act = intent != null ? intent.getAction() : "asd";
		Log.v(TAG, act);

		OASession session = OASession.getInstance(this);
		boolean sound = false;

		switch (act) {
			case "android.intent.action.BOOT_COMPLETED":
				session.setLastWiFiScan(null);
				session.checkNetwork();
				break;
			case "android.net.conn.CONNECTIVITY_CHANGE":
			case "android.net.wifi.WIFI_STATE_CHANGED":
			case "android.net.wifi.STATE_CHANGE":
			case "startup":
				session.checkNetwork();
				break;
			case "android.net.wifi.SCAN_RESULTS":
				session.setLastWiFiScan(((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).getScanResults());
				break;
			case OASession.AC.ENTER:
			case OASession.AC.LEAVE:
			case OASession.AC.LEFTW:
				sound = true;
				break;
			case OASession.AC.BLUNC:
				session.setAtLunch(true);
				sound = true;
				break;
			case OASession.AC.ELUNC:
				session.setAtLunch(false);
				sound = true;
				break;
			default:
				return;
		}

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (!(session.getAtWork() || session.getAtLunch())) {
			nm.cancelAll();
			return;
		}

		NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
		nb.setCategory(NotificationCompat.CATEGORY_STATUS);
		nb.setPriority(NotificationCompat.PRIORITY_HIGH);
		nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		nb.setAutoCancel(false);
		nb.setOngoing(true);

		if (session.getAtLunch()) {
			nb.setContentTitle(getString(R.string.notif_atlunch));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_at_lunch));
		} else {
			long lt = session.getLeaving();
			long st = System.currentTimeMillis();
			nb.setContentTitle(getString(st < lt ? R.string.notif_atwork : R.string.notif_gohome));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_at_work));
		}
		nb.setSmallIcon(R.drawable.ic_stat_notif);
		nb.setContentText(String.format(getString(R.string.notif_details), OASession.timeString(session.getArrival()),
			OASession.timeString(session.getLeaving())));
		if (sound)
			nb.setSound(NOTIF_SOUND);

		Intent ri = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
		nb.setContentIntent(pi);

		nm.notify(NOTIF_STATUS, nb.build());
	}
}
