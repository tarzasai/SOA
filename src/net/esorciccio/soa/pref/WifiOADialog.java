package net.esorciccio.soa.pref;

import net.esorciccio.soa.serv.OASession.PK;
import android.content.Context;
import android.util.AttributeSet;

public class WifiOADialog extends BaseWifiDialog {
	
	public WifiOADialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		wifiPreference = PK.WFWRK;
	}
}