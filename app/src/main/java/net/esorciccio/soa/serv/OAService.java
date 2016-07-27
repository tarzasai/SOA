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

import net.esorciccio.soa.R;
import net.esorciccio.soa.SettingsActivity;

public class OAService extends IntentService {
	private static final String TAG = "OAService";

	private static final int NOTIF_ID = 1199;
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
				return;
			case "android.net.conn.CONNECTIVITY_CHANGE":
			case "android.net.wifi.WIFI_STATE_CHANGED":
			case "android.net.wifi.STATE_CHANGE":
			case OASession.AC.CHECK:
				if (!session.checkNetwork())
					session.checkAlarms();
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
		nb.setSmallIcon(R.drawable.ic_notif_small);

		long lt = session.getLeaving();
		if (session.getAtLunch()) {
			nb.setContentTitle(getString(R.string.notif_lunch_title));
			nb.setContentText(String.format(getString(R.string.notif_lunch_text),
				OASession.timeString(session.getLunchBegin()), OASession.timeString(session.getLunchEnd())));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_lunch));
		} else if (System.currentTimeMillis() < lt) {
			nb.setContentTitle(getString(R.string.notif_atwork_title));
			nb.setContentText(String.format(getString(R.string.notif_atwork_text),
				OASession.timeString(session.getArrival()), OASession.timeString(lt)));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_work));
		} else {
			nb.setContentTitle(getString(R.string.notif_gohome_title));
			nb.setContentText(String.format(getString(R.string.notif_gohome_text), OASession.timeString(lt)));
			nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_home));
		}
		if (sound)
			nb.setSound(NOTIF_SOUND);

		Intent ri = new Intent(this, SettingsActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
		nb.setContentIntent(pi);

		nm.notify(NOTIF_ID, nb.build());
	}
}
