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
	public static final String LOGTAG = "SOAData";
	
	private static OASession singleton;
	
	public static OASession getInstance(Context context) {
		if (singleton == null) {
			singleton = new OASession(context);
		}
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
		return getPrefs().getInt("dayset" + Integer.toString(weekday), 8);
	}
	
	public int[] getWeekHours() {
		return new int[] { getDayHours(2), getDayHours(3), getDayHours(4), getDayHours(5), getDayHours(6),
			getDayHours(7) };
	}
	
	public void setWeekHours(int[] daysets) {
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			editor.putInt("dayset" + Integer.toString(i + 2), daysets[i]);
		editor.commit();
	}
	
	public Set<String> getWifiSet() {
		return getPrefs().getStringSet("wifiset", new HashSet<String>());
	}
	
	public void setWifiSet(Set<String> ssids) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet("wifiset", ssids);
		editor.commit();
	}
	
	public long getStartTime() {
		long res = getPrefs().getLong("start", 0);
		if (res > 0 && !DateUtils.isToday(res))
			res = 0;
		return res;
	}
	
	public void setStartTime(long value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("start", value);
		editor.commit();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
	}
}