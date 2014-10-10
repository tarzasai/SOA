package net.esorciccio.goa;

import java.util.Date;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class OAReceiver extends BroadcastReceiver {
	
	private OASession session;
	private WifiManager wifiman;
	
	public OAReceiver() {
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		session = OASession.getInstance(context);
		
		if (inOffice(context)) {
			
			if (session.getArrival() <= 0)
				session.setArrival(new Date().getTime());
			
		} else if (session.getArrival() > 0) {
			
			if (session.getLeaving() <= 0)
				session.setLeaving(new Date().getTime());
			
		}
		
	}
	
	private boolean inOffice(Context context) {
		wifiman = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		Set<String> ssids = session.getWifiSet();
		for (ScanResult sr: wifiman.getScanResults())
			if (ssids.contains(sr.SSID))
				return true;
		return false;
	}
}