package net.esorciccio.soa;

import java.util.HashSet;
import java.util.Set;

import net.esorciccio.soa.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class WifisetDialog extends DialogPreference {
	
	private OASession session;
	private WifisetAdapter adapter;
	private WifiManager wifiman;
	private WifiReceiver receiver;
	
	public WifisetDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.dialog_wifiset);
		
		session = OASession.getInstance(context);
		adapter = new WifisetAdapter(context);
	}
	
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		
		ListView lv = (ListView) view.findViewById(R.id.lst_wifiset);
		lv.setAdapter(adapter);
		
		wifiman = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiman.isWifiEnabled()) {
			receiver = new WifiReceiver();
			getContext().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			wifiman.startScan();
	        Toast.makeText(getContext(), R.string.msg_wifi_scan, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getContext(), R.string.msg_wifi_off, Toast.LENGTH_LONG).show();
		}
		
		return view;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		getContext().unregisterReceiver(receiver);
		
		if (positiveResult)
			session.setWifiSet(adapter.getWifiset(true));
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