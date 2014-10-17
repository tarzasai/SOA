package net.esorciccio.goa;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class OASession implements OnSharedPreferenceChangeListener {
	
	static class AC {
		public static final String LEAVE = "net.esorciccio.goa.OASession.AC.LEAVE";
		public static final String BLUNC = "net.esorciccio.goa.OASession.AC.BLUNC";
		public static final String ELUNC = "net.esorciccio.goa.OASession.AC.ELUNC";
	}
	
	static class PK {
		public static final String HOURS = "pk_hours";
		public static final String WIFIS = "pk_wifis";
		public static final String ROUND = "pk_round";
		public static final String THERE = "pk_there";
		public static final String ARRIV = "pk_arrival";
		public static final String LEAVE = "pk_leaving";
		public static final String LUNCH = "pk_lunch";
		public static final String BLUNC = "pk_lstart";
		public static final String ELUNC = "pk_lstop";
	}
	
	private static Context appContext;
	private static OASession singleton;
	
	public static OASession getInstance(Context context) {
		if (singleton == null)
			singleton = new OASession(context);
		return singleton;
	}
	
	private final SharedPreferences prefs;
	private final String[] daynames;
	
	public OASession(Context context) {
		appContext = context.getApplicationContext();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		daynames = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
	}
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	
	public String getDayName() {
		return Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
	}
	
	public String getDayName(int weekday) {
		return daynames[weekday];
	}
	
	public int getDayHours() {
		return getDayHours(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // today
	}
	
	public int getDayHours(int weekday) {
		return getPrefs().getInt(PK.HOURS + Integer.toString(weekday), 8);
	}
	
	public int[] getWeekHours() {
		return new int[] { getDayHours(2), getDayHours(3), getDayHours(4), getDayHours(5), getDayHours(6),
			getDayHours(7) };
	}
	
	public void setWeekHours(int[] daysets) {
		Log.v(getClass().getSimpleName(), "setWeekHours");
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			editor.putInt(PK.HOURS + Integer.toString(i + 2), daysets[i]);
		editor.commit();
	}
	
	public Set<String> getWifiSet() {
		return getPrefs().getStringSet(PK.WIFIS, new HashSet<String>());
	}
	
	public void setWifiSet(Set<String> ssids) {
		Log.v(getClass().getSimpleName(), "setWifiSet");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(PK.WIFIS, ssids);
		editor.commit();
	}
	
	public boolean getInOffice() {
		return getPrefs().getBoolean(PK.THERE, false);
	}
	
	public void setInOffice(boolean value) {
		if (value == getInOffice())
			return;
		
		Log.v(getClass().getSimpleName(), "setInOffice");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PK.THERE, value);
		editor.commit();
		
		if (value) {
			if (getArrival() <= 0)
				setArrival(System.currentTimeMillis());
			else
				setLeft(0);
		} else if (getArrival() > 0 && getLeft() <= 0) {
			setLeft(System.currentTimeMillis());
		}
	}
	
	public long getArrival() {
		long res = getPrefs().getLong(PK.ARRIV, 0);
		if (res > 0 && !DateUtils.isToday(res))
			res = 0;
		else {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(res);
			if (getRoundAt5()) {
				int m = cal.get(Calendar.MINUTE);
				m -= m % 5;
				cal.set(Calendar.MINUTE, m);
			}
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			res = cal.getTimeInMillis();
		}
		return res;
	}
	
	public void setArrival(long value) {
		if (value == getArrival())
			return;
		
		Log.v(getClass().getSimpleName(), "setArrival");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.ARRIV, value);
		editor.commit();
	}
	
	public long getLeft() {
		long res = getPrefs().getLong(PK.LEAVE, 0);
		if (res > 0 && !DateUtils.isToday(res))
			res = 0;
		return res;
	}
	
	public void setLeft(long value) {
		if (value == getLeft())
			return;
		
		Log.v(getClass().getSimpleName(), "setLeaving");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.LEAVE, value);
		editor.commit();
	}
	
	public long getLeaving() {
		long res = getArrival();
		if (res > 0) {
			res += (1000 * 60 * 60 * getDayHours());
			if (getLunchAlerts())
				res += (getLunchEnd() - getLunchBegin());
		}
		return res;
	}
	
	public boolean getRoundAt5() {
		return getPrefs().getBoolean(PK.ROUND, false);
	}
	
	public boolean getLunchAlerts() {
		return getPrefs().getBoolean(PK.LUNCH, true);
	}
	
	public long getLunchBegin() {
		String[] tp = getPrefs().getString(PK.BLUNC, "13:00").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	public long getLunchEnd() {
		String[] tp = getPrefs().getString(PK.ELUNC, "14:00").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	private static PendingIntent mkPI(String action) {
		return PendingIntent.getBroadcast(appContext, 0, new Intent(appContext, OAReceiver.class).setAction(action),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	public void checkAlarms() {
		Log.v(getClass().getSimpleName(), "resetAlarms");
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent li = mkPI(AC.LEAVE);
		PendingIntent bi = mkPI(AC.BLUNC);
		PendingIntent ei = mkPI(AC.ELUNC);
		
		long at = getArrival();
		long lt = getLeaving();
		long bt = getLunchBegin();
		long et = getLunchEnd();
		
		if (!(getInOffice() && at > 0 && lt > System.currentTimeMillis())) {
			am.cancel(li);
		} else {
			am.setRepeating(AlarmManager.RTC_WAKEUP, lt, AlarmManager.INTERVAL_HALF_HOUR, li);
			Log.v(getClass().getSimpleName(), "leaving alarm set to " + DateUtils.formatSameDayTime(lt,
				System.currentTimeMillis(), DateFormat.SHORT, DateFormat.MEDIUM).toString());
		}
		
		if (!(getLunchAlerts() && at > 0 && lt > System.currentTimeMillis())) {
			am.cancel(bi);
			am.cancel(ei);
		} else {
			if (bt < System.currentTimeMillis())
				am.cancel(bi);
			else {
				am.set(AlarmManager.RTC_WAKEUP, bt, bi);
				Log.v(getClass().getSimpleName(), "lunch begin alarm set to " + DateUtils.formatSameDayTime(
					bt, System.currentTimeMillis(), DateFormat.SHORT, DateFormat.MEDIUM).toString());
			}
			if (et < System.currentTimeMillis())
				am.cancel(ei);
			else {
				am.set(AlarmManager.RTC_WAKEUP, et, ei);
				Log.v(getClass().getSimpleName(), "lunch end alarm set to " + DateUtils.formatSameDayTime(
					et, System.currentTimeMillis(), DateFormat.SHORT, DateFormat.MEDIUM).toString());
			}
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		checkAlarms();
	}
}