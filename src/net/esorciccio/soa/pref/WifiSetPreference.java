package net.esorciccio.soa.pref;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.esorciccio.soa.R;
import net.esorciccio.soa.data.WifisetAdapter;
import net.esorciccio.soa.serv.OASession;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class WifiSetPreference extends ListPreference {
	
	private OASession session;
	private Set<String> lastSet;
	private WifisetAdapter adapter;
	private WifiManager wifiman;
	private WifiReceiver receiver;
	
	protected String wifiPreference;
	
	public WifiSetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setDialogLayoutResource(R.layout.dialog_wifiset);
		
		session = OASession.getInstance(context);
	}
	
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		
		adapter = new WifisetAdapter(getContext(), wifiPreference);
		
		ListView lv = (ListView) view.findViewById(R.id.lst_wifiset);
		lv.setAdapter(adapter);
		
		wifiman = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
		if (!wifiman.isWifiEnabled())
			Toast.makeText(getContext(), R.string.msg_wifi_off, Toast.LENGTH_LONG).show();
		else {
			receiver = new WifiReceiver();
			getContext().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			wifiman.startScan();
	        Toast.makeText(getContext(), R.string.msg_wifi_scan, Toast.LENGTH_SHORT).show();
		}
		
		return view;
	}
	
	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		getContext().unregisterReceiver(receiver);
		
		if (positiveResult)
			session.setWifiSet(wifiPreference, adapter.getWifiset(true));
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getTextArray(index);
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		String value = null;
		CharSequence[] defaults = defaultValue == null ? new CharSequence[0] : (CharSequence[]) defaultValue;
		String joinedDefaults = TextUtils.join("|", Arrays.asList(defaults));
		value = restoreValue ? getPersistedString(joinedDefaults) : joinedDefaults;
		lastSet = new HashSet<String>(Arrays.asList(value.split("|")));
		setSummary(TextUtils.join(", ", lastSet));
	}
	
	private void foundWiFi(Set<String> ssids) {
		adapter.addNetworks(ssids);
	}
	
	class WifiReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent intent) {
			Set<String> ssids = new HashSet<String>();
			for (ScanResult sr: wifiman.getScanResults())
				ssids.add(sr.SSID);
			foundWiFi(ssids);
		}
	}
}