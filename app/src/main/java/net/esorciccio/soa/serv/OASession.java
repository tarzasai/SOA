package net.esorciccio.soa.serv;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OASession implements OnSharedPreferenceChangeListener {
	private static final String TAG = "OASession";

	private static final String[] PERMLIST = {
		//
		Manifest.permission.RECEIVE_BOOT_COMPLETED,
		Manifest.permission.ACCESS_COARSE_LOCATION,
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_NETWORK_STATE,
		Manifest.permission.ACCESS_WIFI_STATE,
		Manifest.permission.CHANGE_WIFI_STATE,
		Manifest.permission.INTERNET,
		Manifest.permission.READ_PHONE_STATE,
		//
		Manifest.permission.BLUETOOTH,
		Manifest.permission.BLUETOOTH_ADMIN
	};

	public static boolean isOnWIFI = false;
	public static boolean isOn3G = false;
	public static boolean isRoaming = false;
	public static String network = "Non connesso";

	private static Context appContext;
	private static OASession singleton;

	public final String[] dayNames;

	private final SharedPreferences prefs;

	public OASession(Context context) {
		appContext = context.getApplicationContext();

		dayNames = new DateFormatSymbols(Locale.getDefault()).getWeekdays();

		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	public static OASession getInstance(Context context) {
		if (singleton == null)
			singleton = new OASession(context);
		return singleton;
	}

	private static PendingIntent mkPI(String action) {
		return PendingIntent.getBroadcast(appContext, 0, new Intent(appContext, OAReceiver.class).setAction(action),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static String dateString(long time, String format) {
		return new SimpleDateFormat(format, Locale.getDefault()).format(new Date(time));
	}

	public static String timeString(long time) {
		return time <= 0 ? "N/A" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
	}

	public List<String> missingPermissions() {
		List<String> perms = new ArrayList<>();
		for (String p : PERMLIST)
			if (ContextCompat.checkSelfPermission(appContext, p) != PackageManager.PERMISSION_GRANTED)
				perms.add(p);
		return perms;
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
		return dayNames[weekday];
	}

	public int getDayHours() {
		return getDayHours(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // today
	}

	public int getDayHours(int weekday) {
		return getPrefs().getInt(PK.HOURS + Integer.toString(weekday), 8);
	}

	public int[] getWeekHours() {
		return new int[]{getDayHours(2), getDayHours(3), getDayHours(4), getDayHours(5), getDayHours(6),
			getDayHours(7)};
	}

	public void setWeekHours(int[] daysets) {
		Log.v(TAG, "setWeekHours");
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			editor.putInt(PK.HOURS + Integer.toString(i + 2), daysets[i]);
		editor.commit();
	}

	public Set<String> getWifiSet(String wifiPref) {
		return getPrefs().getStringSet(wifiPref, new HashSet<String>());
	}

	public void setWifiSet(String wifiPref, Set<String> ssids) {
		Log.v(TAG, "setWifiSet");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(wifiPref, ssids);
		editor.commit();
	}

	public Set<String> getLastWiFiScan() {
		return getPrefs().getStringSet(PK.WSCAN, new HashSet<String>());
	}

	public void setLastWiFiScan(List<ScanResult> networks) {
		SharedPreferences.Editor editor = getPrefs().edit();
		editor.putLong(PK.TSCAN, System.currentTimeMillis());
		if (networks != null && !networks.isEmpty()) {
			Set<String> values = new HashSet<>();
			for (ScanResult sr : networks)
				if (!TextUtils.isEmpty(sr.SSID))
					values.add(sr.SSID);
			editor.putStringSet(PK.WSCAN, values);
		} else
			editor.remove(PK.WSCAN);
		editor.commit();
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

		Log.v(TAG, "setArrival");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.ARRIV, value);
		editor.putLong(PK.LEAVE, 0);
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

		Log.v(TAG, "setLeaving");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.LEAVE, value);
		editor.putString(PK.DEBUG, timeString(System.currentTimeMillis()) + " setLeft: " + timeString(value));
		editor.commit();
	}

	public long getLeaving() {
		long res = getArrival();
		if (res > 0) {
			res += (getDayHours() * AlarmManager.INTERVAL_HOUR);
			if (getLunchAlerts() && getLunchBegin() < res)
				res += (getLunchEnd() - getLunchBegin());
		}
		return res;
	}

	public boolean getRoundAt5() {
		return getPrefs().getBoolean(PK.ROUND, false);
	}

	public int getCleanDay() {
		return getPrefs().getInt(PK.CLDAY, 0);
	}

	public long getCleanAlarmTime() {
		if (getWeekDay() != getCleanDay())
			return 0;
		String[] tp = getPrefs().getString(PK.CLTIM, "18:00").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.MINUTE, -30); // mezz'ora prima
		return cal.getTimeInMillis();
	}

	public boolean getLunchAlerts() {
		return getPrefs().getBoolean(PK.LUNCH, true);
	}

	public boolean getAtWork() {
		return getPrefs().getBoolean(PK.ATWRK, false);
	}

	public boolean getAtHome() {
		return getPrefs().getBoolean(PK.ATHOM, false);
	}

	public long getLunchBegin() {
		String[] tp = getPrefs().getString(PK.BLUNC, "13:30").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public long getLunchEnd() {
		String[] tp = getPrefs().getString(PK.ELUNC, "14:30").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public void checkNetwork() {
		List<String> mp = missingPermissions();
		if (!mp.isEmpty())
			network = "Permessi mancanti!";
		else {
			ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			int ns = (ni != null && ni.isConnected()) ? ni.getType() : ConnectivityManager.TYPE_DUMMY;
			isOn3G = ns == ConnectivityManager.TYPE_MOBILE;
			isOnWIFI = ns == ConnectivityManager.TYPE_WIFI;
			network = "";
			if (isOnWIFI) {
				WifiManager wm = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
				if (getLastWiFiScan().isEmpty())
					wm.startScan();
				WifiInfo wi = wm.getConnectionInfo();
				if (wi != null && !TextUtils.isEmpty(wi.getSSID()))
					network = wi.getSSID().replace("\"", "");
			}
			TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
			if (TextUtils.isEmpty(network)) {
				if (ns != ConnectivityManager.TYPE_MOBILE || tm == null)
					network = "Nessuna connessione";
				else {
					network = tm.getNetworkOperatorName();
					isRoaming = tm.isNetworkRoaming();
				}
			}
		}
		Log.d(TAG, "checkNetwork(): " + network);
	}

	public void checkLocation() {
		Log.v(TAG, "checkLocation()");
		Set<String> workWiFis = getPrefs().getStringSet(PK.WFWRK, new HashSet<String>());
		Set<String> homeWiFis = getPrefs().getStringSet(PK.WFHOM, new HashSet<String>());
		Set<String> scanWiFis = getLastWiFiScan();
		Log.d(TAG, "Local networks: " + TextUtils.join(", ", scanWiFis));
		if (workWiFis.isEmpty() || scanWiFis.isEmpty())
			return;
		SharedPreferences.Editor editor = prefs.edit();
		boolean work = false;
		for (String ssid : workWiFis)
			if (scanWiFis.contains(ssid)) {
				Log.d(TAG, "office network: " + ssid);
				work = true;
				break;
			}
		editor.putBoolean(PK.ATWRK, work);
		if (work)
			editor.putBoolean(PK.ATHOM, false);
		else
			for (String ssid : homeWiFis)
				if (scanWiFis.contains(ssid)) {
					Log.d(TAG, "home network: " + ssid);
					editor.putBoolean(PK.ATHOM, true);
					break;
				}
		editor.commit();
		long st = System.currentTimeMillis();
		long at = getArrival();
		long lt = getLeft();
		if (work) {
			if (at <= 0) // sono arrivato in ufficio
				setArrival(st);
			else if (lt > 0) // ero uscito ma sono rientrato (pranzo)
				setLeft(0);
		} else if (at > 0 && lt <= 0) // sono uscito
			setLeft(st);
	}

	public void checkAlarms() {
		Log.v(TAG, "resetAlarms()");
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		PendingIntent li = mkPI(AC.LEAVE);
		PendingIntent bi = mkPI(AC.BLUNC);
		PendingIntent ei = mkPI(AC.ELUNC);
		PendingIntent ci = mkPI(AC.CLEAN);
		long st = System.currentTimeMillis();
		long at = getArrival();
		long lt = getLeaving();
		long bt = getLunchBegin();
		long et = getLunchEnd();
		// allarmi uscita e pranzo
		if (at > 0 && lt > st && getLeft() <= 0) {
			am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, lt - AlarmManager.INTERVAL_FIFTEEN_MINUTES, li);
			am.setRepeating(AlarmManager.RTC_WAKEUP, lt, AlarmManager.INTERVAL_FIFTEEN_MINUTES, li);
			Log.v(TAG, "leaving alarm set to " + timeString(lt));
			if (getLunchAlerts()) {
				if (bt < st || bt >= lt) {
					am.cancel(bi);
					Log.v(TAG, "lunch begin alarm canceled");
				} else {
					am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bt, bi);
					Log.v(TAG, "lunch begin alarm set to " + timeString(bt));
				}
				if (et < st || et >= lt) {
					am.cancel(ei);
					Log.v(TAG, "lunch end alarm canceled");
				} else {
					am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, et, ei);
					Log.v(TAG, "lunch end alarm set to " + timeString(et));
				}
			}
		} else {
			li.cancel();
			am.cancel(li);
			Log.v(TAG, "leaving alarm canceled");
			am.cancel(bi);
			am.cancel(ei);
			Log.v(TAG, "lunch alarms canceled");
		}
		// allarme pulizie
		long ct = getCleanAlarmTime();
		if (ct > st) {
			am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ct, ci); // è già mezz'ora prima
			am.setRepeating(AlarmManager.RTC_WAKEUP, ct + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				AlarmManager.INTERVAL_FIFTEEN_MINUTES, ci);
			Log.v(TAG, "cleaning alarm set to " + timeString(ct));
		} else {
			am.cancel(ci);
			Log.v(TAG, "cleaning alarm canceled");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		if (key.startsWith(PK.HOURS)) {
			checkLocation();
			checkAlarms(); // checkLocation potrebbe non eseguirlo, ma è necessario.
		} else
			switch (key) {
				case PK.WSCAN:
				case PK.WFWRK:
				case PK.WFHOM:
					checkLocation();
					break;
				case PK.ARRIV:
					checkAlarms();
					am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, mkPI(AC.ENTER));
					break;
				case PK.LEAVE:
					checkAlarms();
					am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, mkPI(AC.LEFTW));
					break;
				case PK.LUNCH:
				case PK.BLUNC:
				case PK.ELUNC:
				case PK.CLDAY:
				case PK.CLTIM:
					checkAlarms();
					break;
			}
	}

	public static class AC {
		public static final String ENTER = "net.esorciccio.soa.OASession.AC.ENTER";
		public static final String LEAVE = "net.esorciccio.soa.OASession.AC.LEAVE";
		public static final String LEFTW = "net.esorciccio.soa.OASession.AC.LEFTW";
		public static final String BLUNC = "net.esorciccio.soa.OASession.AC.BLUNC";
		public static final String ELUNC = "net.esorciccio.soa.OASession.AC.ELUNC";
		public static final String CLEAN = "net.esorciccio.soa.OASession.AC.CLEAN";
	}

	public static class PK {
		public static final String HOURS = "pk_hours";
		public static final String WFWRK = "pk_wifis";
		public static final String WFHOM = "pk_wifih";
		public static final String ROUND = "pk_round";
		public static final String ARRIV = "pk_arrival";
		public static final String LEAVE = "pk_leaving";
		public static final String LUNCH = "pk_lunch";
		public static final String BLUNC = "pk_lstart";
		public static final String ELUNC = "pk_lstop";
		public static final String CLDAY = "pk_cleanday";
		public static final String CLTIM = "pk_cleantime";
		// no checkalarms:
		public static final String ATHOM = "pk_at_home";
		public static final String ATWRK = "pk_at_work";
		public static final String TSCAN = "pk_time_scan";
		public static final String WSCAN = "pk_last_scan";
		public static final String DEBUG = "pk_debug_msg";
	}

	public static class WR {
		public static final String VOLUME_DOWN = "net.esorciccio.soa.REQUEST_VOLUME_DOWN";
		public static final String VOLUME_UP = "net.esorciccio.soa.REQUEST_VOLUME_UP";
	}
}