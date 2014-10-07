package net.esorciccio.wta;

import android.app.IntentService;
import android.content.Intent;

public class SOAService extends IntentService {
	// TODO: Rename actions, choose action names that describe tasks that this
	// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
	public static final String ACTION_FOO = "net.esorciccio.wta.action.FOO";
	public static final String ACTION_BAZ = "net.esorciccio.wta.action.BAZ";
	
	public SOAService() {
		super("SOAService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_FOO.equals(action)) {
				
			} else if (ACTION_BAZ.equals(action)) {
				
			}
		}
	}
}