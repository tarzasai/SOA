package net.esorciccio.soa;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import net.esorciccio.soa.serv.OASession;

import java.util.List;

public class SettingsActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
	private static final int PERM_REQUEST = 123;

	private OASession session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

		session = OASession.getInstance(this);

		List<String> mp = session.missingPermissions();
		if (!mp.isEmpty()) {
			String[] pl = new String[mp.size()];
			pl = mp.toArray(pl);
			ActivityCompat.requestPermissions(this, pl, PERM_REQUEST);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		session.checkNetwork();
	}
}