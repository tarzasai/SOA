package net.esorciccio.soa;

import java.util.Date;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

public class OAService extends IntentService implements OnSharedPreferenceChangeListener {
	private static final String TAG = "OAService";
	
	private OASession session;
	private boolean terminated = false;
	
	private Long last3time = Long.valueOf(0);
	private boolean error3 = false;
	private String credito = "n/a";
	private String traffico = "n/a";
	
	public OAService() {
		super("OAService");
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.v(TAG, "onCreate");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(TAG, "onHandleIntent");
		
		session = OASession.getInstance(this);
		
		try {
			while (!terminated) {
				
				if (!session.isOnWIFI(this) && ((System.currentTimeMillis() - last3time) / (60 * 1000) % 60))) {
					
					/*
					Intent dialogIntent = new Intent(getBaseContext(), myActivity.class);
					dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplication().startActivity(dialogIntent);
					*/
					
					saved = true;
				}
				
				AppWidgetManager.getInstance(this).updateAppWidget(new ComponentName(this, OAWidgetLarge.class),
					buildUpdate(this));

				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					terminated = true;
				}
			}
		} finally {
			stopSelf();
		}
	}
	
	private RemoteViews buildUpdate(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		@SuppressWarnings("deprecation")
		String alarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		// views.setTextViewText(R.id.txt_left, OASession.timeString(session.getLeft()));
		// views.setTextViewText(R.id.txt_clock, "23:48");
		views.setTextViewText(R.id.txt_clock, OASession.timeString(System.currentTimeMillis()));
		views.setTextViewText(R.id.txt_today, OASession.dateString(System.currentTimeMillis(), "EEE d MMM"));
		views.setTextViewText(R.id.txt_alarm, alarm);
		views.setTextViewText(R.id.txt_phone, credito);
		views.setTextViewText(R.id.txt_data, traffico);
		
		//
		PendingIntent pendingIntent;
		
		Intent mainIntent = new Intent(context, MainActivity.class);
		pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
		views.setOnClickPendingIntent(R.id.frm_left, pendingIntent);
		
		try {
			Intent clockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock");
			context.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
			clockIntent.setComponent(cn);
			pendingIntent = PendingIntent.getActivity(context, 0, clockIntent, 0);
			views.setOnClickPendingIntent(R.id.txt_clock, pendingIntent);
		} catch (Exception err) {
			Log.e(TAG, "clockIntent", err);
		}
		
		return views;
	}
}