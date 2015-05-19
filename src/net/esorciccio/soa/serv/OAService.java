package net.esorciccio.soa.serv;

import net.esorciccio.soa.MainActivity;
import net.esorciccio.soa.OAWidgetLarge;
import net.esorciccio.soa.R;
import net.esorciccio.soa.serv.OASession.WR;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class OAService extends IntentService {
	private static final String TAG = "OAService";
	
	private OASession session;
	private ComponentName compName;
	private boolean terminated = false;
	
	public OAService() {
		super("OAService");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(TAG, "onHandleIntent");
		session = OASession.getInstance(this);
		session.checkNetwork();
		session.checkAlarms();
		compName = new ComponentName(this, OAWidgetLarge.class);
		try {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			while (!terminated) {
				if (pm.isPowerSaveMode() || !pm.isInteractive())
					Thread.sleep(5000);
				else {
					AppWidgetManager.getInstance(this).updateAppWidget(compName, buildUpdate(this));
					Thread.sleep(1500);
				}
			}
		} catch (InterruptedException e) {
			terminated = true;
		} finally {
			stopSelf();
		}
	}
	
	private RemoteViews buildUpdate(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		
		//views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		long ttogo = session.getLeaving();
		long tleft = session.getLeft();
		if (tleft > 0 && tleft < ttogo) {
			views.setTextColor(R.id.txt_leaving, Color.CYAN);
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(tleft));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_action_av_fast_rewind, 0, 0, 0);
		} else {
			views.setTextColor(R.id.txt_leaving, Color.WHITE);
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(ttogo));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_action_av_skip_previous, 0, 0, 0);
		}
		
		views.setTextViewText(R.id.txt_clock, OASession.timeString(System.currentTimeMillis()));
		
		views.setTextViewText(R.id.txt_today, OASession.dateString(System.currentTimeMillis(), "EEE d MMM"));
		
		@SuppressWarnings("deprecation")
		String alarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		if (TextUtils.isEmpty(alarm)) {
			views.setTextViewText(R.id.txt_alarm, "nessuna");
			views.setTextViewCompoundDrawables(R.id.txt_alarm, 0, 0, R.drawable.ic_action_alarm_off, 0);
		} else {
			views.setTextViewText(R.id.txt_alarm, alarm);
			views.setTextViewCompoundDrawables(R.id.txt_alarm, 0, 0, R.drawable.ic_action_alarm_set, 0);
		}
		
		int ci = 0;
		if (OASession.isOnWIFI)
			ci = R.drawable.ic_action_wifi;
		else if (!OASession.isOn3G)
			ci = R.drawable.ic_action_noconn;
		else if (OASession.isRoaming)
			ci = R.drawable.ic_action_roaming;
		else
			ci = R.drawable.ic_action_cell;
		
		views.setTextViewText(R.id.txt_tre, OASession.network);
		views.setTextViewCompoundDrawables(R.id.txt_tre, ci, 0, 0, 0);
		
		views.setOnClickPendingIntent(R.id.txt_voldn, PendingIntent.getBroadcast(context, 0,
			new Intent(WR.VOLUME_DOWN), 0));
		
		views.setOnClickPendingIntent(R.id.txt_volup, PendingIntent.getBroadcast(context, 0,
			new Intent(WR.VOLUME_UP), 0));
		
		views.setOnClickPendingIntent(R.id.frm_left, PendingIntent.getActivity(context, 0,
			new Intent(context, MainActivity.class), 0));
		
		try {
			Intent clockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock");
			context.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
			clockIntent.setComponent(cn);
			views.setOnClickPendingIntent(R.id.txt_clock, PendingIntent.getActivity(context, 0, clockIntent, 0));
		} catch (Exception err) {
			Log.e(TAG, "clockIntent", err);
		}
		
		return views;
	}
}