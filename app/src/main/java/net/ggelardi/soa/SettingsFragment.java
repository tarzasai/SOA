package net.ggelardi.soa;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.ggelardi.soa.pref.DaysetsDialog;
import net.ggelardi.soa.serv.OASession;
import net.ggelardi.soa.serv.OASession.PK;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	private OASession session;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		session = OASession.getInstance(getActivity());
		
		Set<String> workWifis = session.getWifiSet(PK.WFWRK);
		Set<String> lastWifis = session.getLastWiFiScan();
		for (String ssid: workWifis)
			if (!lastWifis.contains(ssid))
				lastWifis.add(ssid);
		CharSequence[] wifis = lastWifis.toArray(new CharSequence[lastWifis.size()]);
		Arrays.sort(wifis);

		MultiSelectListPreference workWifiSet = (MultiSelectListPreference) findPreference(PK.WFWRK);
		workWifiSet.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Set<String> list = newValue == null ? null : (Set<String>) newValue;
				preference.setSummary(list == null ? getString(R.string.pd_wifiset_summ) : TextUtils.join(", ", list));
				return true;
			}
		});
		workWifiSet.setSummary(workWifis.isEmpty() ? getString(R.string.pd_wifiset_summ) :
			TextUtils.join(", ", workWifis));
		workWifiSet.setEntries(wifis);
		workWifiSet.setEntryValues(wifis);
		
		findPreference(PK.HOURS).setSummary(getDaysetSummary());

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.startsWith(PK.HOURS))
			findPreference(PK.HOURS).setSummary(getDaysetSummary());
	}

	private String getDaysetSummary() {
		String[] names = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
		int[] hours = session.getWeekHours();
		List<String> lst = new ArrayList<>();
		for (int i = 1; i < 7; i++)
			if (hours[i-1] > 0)
				lst.add(String.format(Locale.getDefault(), "%s %d", names[i+1], hours[i-1]));
		return lst.isEmpty() ? getString(R.string.pd_dayset_summ) : TextUtils.join(", ", lst);
	}
}