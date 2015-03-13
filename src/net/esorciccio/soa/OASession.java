package net.esorciccio.soa;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class OASession implements OnSharedPreferenceChangeListener, BluetoothProfile.ServiceListener {
	
	static class AC {
		public static final String LEAVE = "net.esorciccio.soa.OASession.AC.LEAVE";
		public static final String BLUNC = "net.esorciccio.soa.OASession.AC.BLUNC";
		public static final String ELUNC = "net.esorciccio.soa.OASession.AC.ELUNC";
		public static final String CLEAN = "net.esorciccio.soa.OASession.AC.CLEAN";
	}
	
	static class PK {
		public static final String HOURS = "pk_hours";
		public static final String WIFIS = "pk_wifis";
		public static final String WIFIH = "pk_wifih";
		public static final String ROUND = "pk_round";
		public static final String BTDEV = "pk_btauto";
		public static final String THERE = "pk_there";
		public static final String ARRIV = "pk_arrival";
		public static final String LEAVE = "pk_leaving";
		public static final String LUNCH = "pk_lunch";
		public static final String BLUNC = "pk_lstart";
		public static final String ELUNC = "pk_lstop";
		public static final String CLEAN = "pk_clean";
		public static final String L3CRE = "last_3_cred";
		public static final String L3TRA = "last_3_traf";
		public static final String L3TIM = "last_3_time";
		public static final String L3ERR = "last_3_fail";
	}
	
	private static Context appContext;
	private static OASession singleton;
	
	public static OASession getInstance(Context context) {
		if (singleton == null)
			singleton = new OASession(context);
		return singleton;
	}
	
	public static boolean isOnWIFI = false;
	public static boolean isOn3G = false;
	public static boolean isRoaming = false;
	public static boolean isBTEnabled = false;
	public static boolean isBTConnected = false;
	
	private final String[] daynames;
	private final SharedPreferences prefs;
	
	public OASession(Context context) {
		appContext = context.getApplicationContext();
		
		daynames = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	
	public int getWeekDay() {
		return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
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
	
	public Set<String> getWifiSet(String wifiPref) {
		return getPrefs().getStringSet(wifiPref, new HashSet<String>());
	}
	
	public void setWifiSet(String wifiPref, Set<String> ssids) {
		Log.v(getClass().getSimpleName(), "setWifiSet");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(wifiPref, ssids);
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
		} else if (getArrival() > 0 && getLeft() <= 0)
			setLeft(System.currentTimeMillis());
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
	
	public boolean getCleanAlert() {
		return getPrefs().getBoolean(PK.CLEAN, false);
	}
	
	public String getBTACDevice() {
		return getPrefs().getString(PK.BTDEV, "");
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
	
	public void setLast3data(String credito, String traffico) {
		Log.v(getClass().getSimpleName(), "setLast3data");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PK.L3CRE, credito);
		editor.putString(PK.L3TRA, traffico);
		editor.putLong(PK.L3TIM, System.currentTimeMillis());
		editor.remove(PK.L3ERR);
		editor.commit();
	}
	
	public void setLast3fail(String error) {
		Log.v(getClass().getSimpleName(), "setLast3fail");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PK.L3ERR, error);
		editor.commit();
	}
	
	public String getLast3cred() {
		return prefs.getString(PK.L3CRE, "n/a");
	}
	
	public String getLast3traf() {
		return prefs.getString(PK.L3TRA, "n/a");
	}
	
	public long getLast3oktime() {
		return prefs.getLong(PK.L3TIM, 0);
	}
	
	public String getLast3error() {
		return prefs.getString(PK.L3ERR, "");
	}
	
	public boolean isLast3failed() {
		return !TextUtils.isEmpty(getLast3error());
	}
	
	public boolean canTreCheck() {
		if (TreActivity.running || !isOn3G || isRoaming)
			return false;
		long t = getLast3oktime();
		if (t > 0)
			t = System.currentTimeMillis() - t;
		if (isLast3failed())
			return TreActivity.lastrun <= 0 || (System.currentTimeMillis() - TreActivity.lastrun) > (15 * 60000);
		return t > (30 * 60000);
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
		PendingIntent ci = mkPI(AC.CLEAN);
		long at = getArrival();
		long lt = getLeaving();
		long bt = getLunchBegin();
		long et = getLunchEnd();
		if (!(getInOffice() && at > 0 && lt > System.currentTimeMillis())) {
			am.cancel(li);
			Log.v(getClass().getSimpleName(), "leaving alarm canceled");
		} else {
			am.setRepeating(AlarmManager.RTC_WAKEUP, lt, AlarmManager.INTERVAL_HALF_HOUR, li);
			Log.v(getClass().getSimpleName(), "leaving alarm set to " + timeString(lt));
		}
		if (!(getLunchAlerts() && at > 0 && lt > System.currentTimeMillis())) {
			am.cancel(bi);
			am.cancel(ei);
			Log.v(getClass().getSimpleName(), "lunch alarms canceled");
		} else {
			if (bt < System.currentTimeMillis()) {
				am.cancel(bi);
				Log.v(getClass().getSimpleName(), "lunch begin alarm canceled");
			} else {
				am.set(AlarmManager.RTC_WAKEUP, bt, bi);
				Log.v(getClass().getSimpleName(), "lunch begin alarm set to " + timeString(bt));
			}
			if (et < System.currentTimeMillis()) {
				am.cancel(ei);
				Log.v(getClass().getSimpleName(), "lunch end alarm canceled");
			} else {
				am.set(AlarmManager.RTC_WAKEUP, et, ei);
				Log.v(getClass().getSimpleName(), "lunch end alarm set to " + timeString(et));
			}
		}
		if (getWeekDay() != Calendar.TUESDAY) {
			am.cancel(ci);
			Log.v(getClass().getSimpleName(), "cleaning alarm canceled");
		} else {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 18);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long ct = cal.getTimeInMillis();
			if (System.currentTimeMillis() >= ct) {
				am.cancel(ci);
				Log.v(getClass().getSimpleName(), "cleaning alarm canceled");
			} else if (getCleanAlert()) {
				cal.set(Calendar.HOUR_OF_DAY, 17);
				cal.set(Calendar.MINUTE, 40);
				ct = cal.getTimeInMillis();
				if (ct > System.currentTimeMillis()) {
					am.setRepeating(AlarmManager.RTC_WAKEUP, ct, (getInOffice() ? 5 : 10) * 60000, ci);
					Log.v(getClass().getSimpleName(), "cleaning alarm set to " + timeString(ct));
				}
			}
		}
	}
	
	public void checkNetwork() {
		ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		int ns = (ni != null && ni.isConnected()) ? ni.getType() : ConnectivityManager.TYPE_DUMMY;
		isOnWIFI = ns == ConnectivityManager.TYPE_WIFI;
		isOn3G = ns == ConnectivityManager.TYPE_MOBILE;
		TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
		isRoaming = (tm != null && tm.isNetworkRoaming());
	}
	
	public void checkBluetooth() {
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		if (ba == null) {
			isBTEnabled = false;
			isBTConnected = false;
		} else {
			isBTEnabled = ba.isEnabled();
			if (!TextUtils.isEmpty(getBTACDevice()))
				ba.getProfileProxy(appContext, this, BluetoothProfile.A2DP);
		}
	}
	
	public void updateWidget() {
		appContext.sendBroadcast(new Intent(appContext, OAWidgetSmall.class).setAction(
			AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
				AppWidgetManager.getInstance(appContext).getAppWidgetIds(
					new ComponentName(appContext, OAWidgetSmall.class))));
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		checkAlarms();
		checkBluetooth();
		updateWidget();
	}
	
	@Override
	public void onServiceConnected(int profile, BluetoothProfile proxy) {
		List<BluetoothDevice> devices = proxy.getConnectedDevices();
		if (devices != null)
			for (BluetoothDevice bd: devices)
				if (bd.getAddress().equals(getBTACDevice())) {
					isBTConnected = true;
					return;
				}
		isBTConnected = false;
	}
	
	@Override
	public void onServiceDisconnected(int profile) {
		// nothing to do.
	}
	
	public static String dateString(long time, String format) {
		return new SimpleDateFormat(format, Locale.getDefault()).format(new Date(time));
	}
	
	public static String timeString(long time) {
		return time <= 0 ? "n/a" : DateUtils.formatSameDayTime(time, System.currentTimeMillis(), DateFormat.SHORT,
			DateFormat.SHORT).toString();
	}
}