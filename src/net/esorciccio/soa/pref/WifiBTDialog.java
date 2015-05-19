package net.esorciccio.soa.pref;

import net.esorciccio.soa.serv.OASession.PK;
import android.content.Context;
import android.util.AttributeSet;

public class WifiBTDialog extends BaseWifiDialog {
	
	public WifiBTDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		wifiPreference = PK.WFHOM;
	}
}