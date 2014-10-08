package net.esorciccio.wta;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

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
	
	public String getDay() {
		return Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
	}
	
	public String getDay(int weekday) {
		return daynames[weekday];
	}
	
	public int getHours() {
		return getHours(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // today
	}
	
	public int getHours(int weekday) {
		return getPrefs().getInt("dayset" + Integer.toString(weekday), 8);
	}
	
	public int[] getDaysets() {
		return new int[] { getHours(2), getHours(3), getHours(4), getHours(5), getHours(6), getHours(7) };
	}
	
	public void setDaysets(int[] daysets) {
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			editor.putInt("dayset" + Integer.toString(i + 2), daysets[i]);
		editor.commit();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
	}
}