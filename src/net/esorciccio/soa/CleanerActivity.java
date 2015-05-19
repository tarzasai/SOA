package net.esorciccio.soa;

import net.esorciccio.soa.serv.OAReceiver;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CleanerActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sendBroadcast(new Intent(OAReceiver.REQ_CC));
		finish();
	}
}