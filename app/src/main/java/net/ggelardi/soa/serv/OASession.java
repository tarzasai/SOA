package net.ggelardi.soa.serv;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
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
		Manifest.permission.RECEIVE_BOOT_COMPLETED,
		Manifest.permission.ACCESS_COARSE_LOCATION,
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_NETWORK_STATE,
		Manifest.permission.ACCESS_WIFI_STATE,
		Manifest.permission.CHANGE_WIFI_STATE,
		Manifest.permission.INTERNET
	};

	private static Context appContext;
	private static OASession singleton;

	public final String[] dayNames;

	private final SharedPreferences prefs;

	public OASession(Context context) {
		appContext = context.getApplicationContext();

		dayNames = new DateFormatSymbols(Locale.getDefault()).getWeekdays();

		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);

		IntentFilter flt = new IntentFilter();
		flt.addAction("android.net.wifi.SCAN_RESULTS");
		appContext.registerReceiver(new OAReceiver(), flt);
	}

	public static OASession getInstance(Context context) {
		if (singleton == null)
			singleton = new OASession(context);
		return singleton;
	}

	public static String timeString(long time) {
		return time <= 0 ? "N/A" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
	}

	public SharedPreferences getPrefs() {
		return prefs;
	}

	public List<String> getMissingPermissions() {
		List<String> perms = new ArrayList<>();
		for (String p : PERMLIST)
			if (ContextCompat.checkSelfPermission(appContext, p) != PackageManager.PERMISSION_GRANTED)
				perms.add(p);
		return perms;
	}

	public String getDayName(int weekday) {
		return dayNames[weekday];
	}

	public Set<String> getWifiSet(String wifiPref) {
		return getPrefs().getStringSet(wifiPref, new HashSet<String>());
	}

	public int getDayHours() {
		return getDayHours(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // today
	}

	public int getDayHours(int weekday) {
		return getPrefs().getInt(PK.HOURS + Integer.toString(weekday), 0);
	}

	public int[] getWeekHours() {
		return new int[]{getDayHours(2), getDayHours(3), getDayHours(4), getDayHours(5), getDayHours(6),
			getDayHours(7)};
	}

	public void setWeekHours(int[] daysets) {
		Log.v(TAG, "setWeekHours");
		SharedPreferences.Editor spe = prefs.edit();
		for (int i = 0; i < daysets.length; i++)
			spe.putInt(PK.HOURS + Integer.toString(i + 2), daysets[i]);
		spe.commit();
	}

	public Set<String> getLastWiFiScan() {
		return getPrefs().getStringSet(PK.WSCAN, new HashSet<String>());
	}

	public void setLastWiFiScan(List<ScanResult> networks) {
		Log.v(TAG, "Scan results received");
		SharedPreferences.Editor spe = getPrefs().edit();
		spe.putLong(PK.TSCAN, System.currentTimeMillis());
		if (networks != null && !networks.isEmpty()) {
			Set<String> values = new HashSet<>();
			for (ScanResult sr : networks)
				if (!TextUtils.isEmpty(sr.SSID))
					values.add(sr.SSID);
			spe.putStringSet(PK.WSCAN, values);
		} else
			spe.remove(PK.WSCAN);
		spe.commit();
		// cancel the next programmed scan alarm (if any)
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		PendingIntent ci = mkPI(AC.CHECK);
		am.cancel(ci);
		// on AC power looks like a scan happens about every minute, so on workdays, while we are
		// not yet at work AND on battery we'll set an alarm to request a new scan in few minutes,
		// but it'll be probably canceled by a system issued scan (because location service)
		if (getDayHours() > 0 && getArrival() <= 0 && !isCharging()) {
			long ct = System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ct, ci);
			Log.v(TAG, "check alarm reset to " + timeString(ct));
		}
	}

	public long getArrival() {
		long res = getPrefs().getLong(PK.ARRIV, 0);
		return res > 0 && DateUtils.isToday(res) ? res : 0;
	}

	public void setArrival(long value) {
		if (value == getArrival())
			return;
		Log.v(TAG, "setArrival");
		// arrotondamenti
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(value);
		if (getPrefs().getBoolean(PK.ROUND, false)) {
			int m = cal.get(Calendar.MINUTE);
			m -= m % 5;
			cal.set(Calendar.MINUTE, m);
		}
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		value = cal.getTimeInMillis();
		// storage
		SharedPreferences.Editor spe = prefs.edit();
		spe.putLong(PK.ARRIV, value);
		spe.putLong(PK.LEAVE, 0);
		spe.commit();
	}

	public long getLeft() {
		long res = getPrefs().getLong(PK.LEAVE, 0);
		return res > 0 && DateUtils.isToday(res) ? res : 0;
	}

	public void setLeft(long value) {
		if (value == getLeft())
			return;
		Log.v(TAG, "setLeaving");
		SharedPreferences.Editor spe = prefs.edit();
		spe.putLong(PK.LEAVE, value);
		spe.commit();
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

	public boolean getLunchAlerts() {
		return getPrefs().getBoolean(PK.LUNCH, true);
	}

	public long getLunchBegin() {
		if (!getLunchAlerts())
			return 0;
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
		if (!getLunchAlerts())
			return 0;
		String[] tp = getPrefs().getString(PK.ELUNC, "14:30").split(":");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public boolean requestWifiScan(boolean force) {
		if (getLastWiFiScan().isEmpty() || force) {
			List<String> mp = getMissingPermissions();
			if (mp.isEmpty()) {
				((WifiManager) appContext.getSystemService(Context.WIFI_SERVICE)).startScan();
				return true;
			}
		}
		return false;
	}

	public void checkLocation() {
		Log.v(TAG, "checkLocation()");
		Set<String> workWiFis = getPrefs().getStringSet(PK.WFWRK, new HashSet<String>());
		Set<String> scanWiFis = getLastWiFiScan();
		Log.d(TAG, "Local networks: " + TextUtils.join(", ", scanWiFis));
		if (workWiFis.isEmpty() || scanWiFis.isEmpty())
			return;
		boolean workWifiFound = false;
		for (String ssid : workWiFis)
			if (scanWiFis.contains(ssid)) {
				Log.d(TAG, "office network: " + ssid);
				workWifiFound = true;
				break;
			}
		long st = System.currentTimeMillis();
		long at = getArrival();
		long lt = getLeft();
		if (workWifiFound) {
			if (at <= 0) // sono arrivato in ufficio
				setArrival(st);
			else if (lt > 0) // ero uscito ma sono rientrato (pranzo?)
				setLeft(0);
			// cancel the next forced scan
			((AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE)).cancel(mkPI(AC.CHECK));
		} else if (at > 0 && lt <= 0) // sono uscito
			setLeft(st);
	}

	private boolean isCharging() {
		IntentFilter flt = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent bsi = appContext.registerReceiver(null, flt);
		int bsv = bsi.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return bsv == BatteryManager.BATTERY_STATUS_CHARGING || bsv == BatteryManager.BATTERY_STATUS_FULL;
	}

	private static PendingIntent mkPI(String action) {
		return PendingIntent.getBroadcast(appContext, 0, new Intent(appContext, OAReceiver.class).setAction(action),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void checkAlarms() {
		Log.v(TAG, "checkAlarms()");
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		PendingIntent li = mkPI(AC.LEAVE);
		PendingIntent bi = mkPI(AC.BLUNC);
		PendingIntent ei = mkPI(AC.ELUNC);
		long st = System.currentTimeMillis();
		long at = getArrival();
		long lt = getLeaving();
		long bt = getLunchBegin();
		long et = getLunchEnd();
		// allarmi lavoro
		if (at > 0 && lt > st && getLeft() <= 0) {
			am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, lt - AlarmManager.INTERVAL_FIFTEEN_MINUTES, li);
			am.setRepeating(AlarmManager.RTC_WAKEUP, lt, AlarmManager.INTERVAL_FIFTEEN_MINUTES, li);
			Log.v(TAG, "leaving alarm set to " + timeString(lt));
			// allarmi pranzo
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
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		if (key.startsWith(PK.HOURS)) {
			checkLocation();
			checkAlarms(); // checkLocation potrebbe non eseguirlo, ma è necessario.
		} else
			switch (key) {
				case PK.WFWRK:
				case PK.WSCAN:
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
					checkAlarms();
					break;
			}
	}

	public static class AC {
		public static final String CHECK = "SOA.AC.CHECK";
		public static final String ENTER = "SOA.AC.ENTER";
		public static final String LEAVE = "SOA.AC.LEAVE";
		public static final String LEFTW = "SOA.AC.LEFTW";
		public static final String BLUNC = "SOA.AC.BLUNC";
		public static final String ELUNC = "SOA.AC.ELUNC";
	}

	public static class PK {
		// settings (user preferences)
		public static final String HOURS = "pk_hours";
		public static final String WFWRK = "pk_wifis";
		public static final String ROUND = "pk_round";
		public static final String LUNCH = "pk_lunch";
		public static final String BLUNC = "pk_lstart";
		public static final String ELUNC = "pk_lstop";
		public static final String NVINI = "pk_vacbegin";
		public static final String NVEND = "pk_vacend";
		// stored data (system managed)
		public static final String ARRIV = "pk_arrival"; //
		public static final String LEAVE = "pk_leaving"; //
		public static final String UDOFF = "pk_offwork"; //
		public static final String TSCAN = "pk_time_scan"; // last wifi scan time
		public static final String WSCAN = "pk_last_scan"; // last wifi scan results
	}
}