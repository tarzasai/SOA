package net.esorciccio.soa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import net.esorciccio.soa.serv.OAService;
import net.esorciccio.soa.serv.OASession;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener,
	ActivityCompat.OnRequestPermissionsResultCallback {
	private static final int PERM_REQUEST = 123;

	private OASession session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// Display the fragment as the settings content.
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

		session = OASession.getInstance(this);
		List<String> mp = session.getMissingPermissions();
		if (!mp.isEmpty()) {
			String[] pl = new String[mp.size()];
			pl = mp.toArray(pl);
			ActivityCompat.requestPermissions(this, pl, PERM_REQUEST);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		startService(new Intent(this, OAService.class).setAction(OASession.AC.CHECK));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_reset) {
			final EditText input = new EditText(this);
			input.setText(OASession.timeString(session.getArrival()));
			new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.dlg_reset_title).setView(input)
				.setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							String[] tp = input.getText().toString().split(":");
							Calendar cal = Calendar.getInstance();
							cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
							cal.set(Calendar.MINUTE, Integer.parseInt(tp[1]));
							cal.set(Calendar.SECOND, 0);
							cal.set(Calendar.MILLISECOND, 0);
							session.setArrival(cal.getTimeInMillis());
							Toast.makeText(SettingsActivity.this, R.string.dlg_reset_done, Toast.LENGTH_SHORT).show();
						} catch (Exception err) {
							Toast.makeText(SettingsActivity.this, err.getMessage(), Toast.LENGTH_SHORT).show();
						}
					}
				})
				.setNegativeButton(R.string.dlg_btn_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						Toast.makeText(SettingsActivity.this, R.string.dlg_reset_canc, Toast.LENGTH_SHORT).show();
					}
				})
				.setCancelable(true).show();
		}
		if (id == R.id.action_wifis) {
			Set<String> list = session.getLastWiFiScan();
			CharSequence[] cs = list.toArray(new CharSequence[list.size()]);
			new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.dlg_wifis_title)
				.setItems(cs, null).setCancelable(true).show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		startService(new Intent(this, OAService.class).setAction(OASession.AC.CHECK));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		startService(new Intent(this, OAService.class).setAction(OASession.AC.CHECK));
	}
}