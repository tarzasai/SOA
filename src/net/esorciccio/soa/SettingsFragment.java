package net.esorciccio.soa;

import java.util.Set;

import net.esorciccio.soa.serv.OASession;
import net.esorciccio.soa.serv.OASession.PK;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {

	private MultiSelectListPreference homeWifiSet;
	private MultiSelectListPreference workWifiSet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		
		homeWifiSet = (MultiSelectListPreference) findPreference(PK.WFHOM);
		workWifiSet = (MultiSelectListPreference) findPreference(PK.WFWRK);

		OASession session = OASession.getInstance(getActivity());
		Set<String> lastwifis = session.getLastWiFiScan();
		CharSequence[] wifis = lastwifis.toArray(new CharSequence[lastwifis.size()]);

		homeWifiSet.setEntries(wifis);
		homeWifiSet.setEntryValues(wifis);
		
		workWifiSet.setEntries(wifis);
		workWifiSet.setEntryValues(wifis);
	}
}