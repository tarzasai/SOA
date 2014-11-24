package net.esorciccio.soa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CCHidden extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sendBroadcast(new Intent(OAReceiver.REQ_CC));
		finish();
	}
}