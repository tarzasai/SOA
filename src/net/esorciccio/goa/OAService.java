package net.esorciccio.goa;

import java.util.Date;

import net.esorciccio.goa.OASession.PK;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class OAService extends IntentService implements OnSharedPreferenceChangeListener {
	public static final int NID_LUNCH_BEG = 1;
	public static final int NID_LUNCH_END = 2;
	public static final int NID_CAN_LEAVE = 3;
	public static final int NID_TIME_BANK = 4;
	
	private OASession session;
	private Boolean terminated = false;
	
	public OAService() {
		super("SOAService");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		session.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		session = OASession.getInstance(this);
		session.getPrefs().registerOnSharedPreferenceChangeListener(this);
		int waitTime = 20000; // 20 secs
		try {
			while (!terminated) {
				if (session.getDayHours() <= 0)
					waitTime = 1800000; // 30 mins
				else {
					checkLunchBegin();
					checkLunchEnd();
					checkLeaving();
				}
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					terminated = true;
				}
			}
		} finally {
			stopSelf();
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.LEAVE)) {
			
		}
	}
	
	private void checkLunchBegin() {
		if (!session.getLunchAlerts())
			return;
		long diff = (new Date().getTime()) - session.getLunchBegin();
		if (diff >= 0 && (diff / 1000 % 60) < 60) {
			/*
			PendingIntent rpi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(DM_BASE_NOTIF),
				PendingIntent.FLAG_UPDATE_CURRENT);
			NotificationCompat.Builder ncb = new NotificationCompat.Builder(this).setSmallIcon(
				R.drawable.ic_launcher).setContentTitle(getResources().getString(R.string.app_name)).setContentText(
				getResources().getString(news ? R.string.notif_dm_new : R.string.notif_dm_upd)).setContentIntent(rpi);
			NotificationManager nmg = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nmg.notify(NOTIFICATION_ID, ncb.build());
			*/
		}
	}
	
	private void checkLunchEnd() {
		if (!session.getLunchAlerts())
			return;
		long diff = (new Date().getTime()) - session.getLunchEnd();
		long dmin = diff / (60 * 1000) % 60;
		if (dmin <= 0) {
			
		}
	}
	
	private void checkLeaving() {
		long diff = (new Date().getTime()) - session.getLeaving();
		long dmin = diff / (60 * 1000) % 60;
		if (dmin <= 0) {
			
		} else if (dmin > 0) {
			
		}
	}
}