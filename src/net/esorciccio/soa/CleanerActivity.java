package net.esorciccio.soa;

import net.esorciccio.soa.serv.OASession.WR;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CleanerActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sendBroadcast(new Intent(WR.CLEAR_CACHE));
		finish();
	}
}