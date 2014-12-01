package net.esorciccio.soa;

import net.esorciccio.soa.OASession.PK;
import android.content.Context;
import android.util.AttributeSet;

public class WifiOADialog extends BaseWifiDialog {
	
	public WifiOADialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		wifiPreference = PK.WIFIS;
	}
}