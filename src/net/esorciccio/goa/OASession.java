package net.esorciccio.goa;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

public class OASession implements OnSharedPreferenceChangeListener {
	
	static class PK {
		public static final String HOURS = "pk_hours";
		public static final String WIFIS = "pk_wifis";
		public static final String ARRIV = "pk_arrival";
		public static final String LEAVE = "pk_leaving";
		public static final String LUNCH = "pk_lunch";
		public static final String LUNCB = "pk_lstart";
		public static final String LUNCE = "pk_lstop";
	}
	
	private static OASession singleton;
	
	public static OASession getInstance(Context context) {
		if (singleton == null)
			singleton = new OASession(context);
		return singleton;
	}
	
	private final SharedPreferences prefs;
	private final String[] daynames;
	
	public OASession(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
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
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			editor.putInt(PK.HOURS + Integer.toString(i + 2), daysets[i]);
		editor.commit();
	}
	
	public Set<String> getWifiSet() {
		return getPrefs().getStringSet(PK.WIFIS, new HashSet<String>());
	}
	
	public void setWifiSet(Set<String> ssids) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(PK.WIFIS, ssids);
		editor.commit();
	}
	
	public long getArrival() {
		long res = getPrefs().getLong(PK.ARRIV, 0);
		if (res > 0 && !DateUtils.isToday(res))
			res = 0;
		return res;
	}
	
	public void setArrival(long value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.ARRIV, value);
		editor.commit();
	}
	
	public long getLeaving() {
		long res = getPrefs().getLong(PK.LEAVE, 0);
		if (res > 0 && !DateUtils.isToday(res))
			res = 0;
		return res;
	}
	
	public void setLeaving(long value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.LEAVE, value);
		editor.commit();
	}
	
	public boolean getLunchAlerts() {
		return getPrefs().getBoolean(PK.LUNCH, true);
	}
	
	public long getLunchBegin() {
		String[] tp = getPrefs().getString(PK.LUNCB, "13:00").split(":");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	public long getLunchEnd() {
		String[] tp = getPrefs().getString(PK.LUNCE, "14:00").split(":");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
	}
}