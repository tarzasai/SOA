package net.esorciccio.soa;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.esorciccio.soa.pref.DaysetsDialog;
import net.esorciccio.soa.pref.IntListPreference;
import net.esorciccio.soa.serv.OASession;
import net.esorciccio.soa.serv.OASession.PK;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

public class SettingsFragment extends PreferenceFragment {
	
	private OASession session;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		session = OASession.getInstance(getActivity());
		
		Set<String> homeWifis = session.getWifiSet(PK.WFHOM);
		Set<String> workWifis = session.getWifiSet(PK.WFWRK);
		Set<String> lastWifis = session.getLastWiFiScan();
		for (String ssid: homeWifis)
			if (!lastWifis.contains(ssid))
				lastWifis.add(ssid);
		for (String ssid: workWifis)
			if (!lastWifis.contains(ssid))
				lastWifis.add(ssid);
		CharSequence[] wifis = lastWifis.toArray(new CharSequence[lastWifis.size()]);
		Arrays.sort(wifis);
		
		MultiSelectListPreference homeWifiSet = (MultiSelectListPreference) findPreference(PK.WFHOM);
		homeWifiSet.setEntries(wifis);
		homeWifiSet.setEntryValues(wifis);
		if (!homeWifis.isEmpty())
			homeWifiSet.setSummary(TextUtils.join(", ", homeWifis));
		homeWifiSet.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Set<String> list = newValue == null ? null : (Set<String>) newValue;
				preference.setSummary(list == null ? "Nessuna" : TextUtils.join(", ", list));
				return true;
			}
		});
		
		MultiSelectListPreference workWifiSet = (MultiSelectListPreference) findPreference(PK.WFWRK);
		workWifiSet.setEntries(wifis);
		workWifiSet.setEntryValues(wifis);
		if (!workWifis.isEmpty())
			workWifiSet.setSummary(TextUtils.join(", ", workWifis));
		workWifiSet.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Set<String> list = newValue == null ? null : (Set<String>) newValue;
				preference.setSummary(list == null ? "Nessuna" : TextUtils.join(", ", list));
				return true;
			}
		});
		
		DaysetsDialog dayset = (DaysetsDialog) findPreference(PK.HOURS);
		dayset.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setDaysetSummary(preference);
				return true;
			}
		});
		setDaysetSummary(dayset);
		
		IntListPreference cleaningDay = (IntListPreference) findPreference(PK.CLDAY);
		cleaningDay.setSummary(session.getCleanDay() <= 0 ? "Nessuno" : session.dayNames[session.getCleanDay()]);
		cleaningDay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Integer day = Integer.parseInt((String) newValue);
				preference.setSummary(day <= 0 ? "Nessuno" : session.dayNames[day]);
				return true;
			}
		});
	}
	
	private void setDaysetSummary(Preference pref) {
		String[] names = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
		int[] hours = session.getWeekHours();
		List<String> sets = new ArrayList<String>();
		for (int i = 1; i < 7; i++)
			sets.add(String.format(Locale.getDefault(), "%s %d", names[i+1], hours[i-1]));
		pref.setSummary(TextUtils.join(", ", sets));
	}
}