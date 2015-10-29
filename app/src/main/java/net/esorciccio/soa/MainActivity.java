package net.esorciccio.soa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import net.esorciccio.soa.serv.OASession;

import java.util.Calendar;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {

	private OASession session;

	private TextView txtArrival;
	private TextView txtLeaving;
	private TextView txtTmpLeft;
	private TableRow rowTmpLeft;
	private TextView txtScanTim;
	private TextView txtScanRes;
	private TextView txtDbgText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String pkg = getPackageName();
		PowerManager pm = getSystemService(PowerManager.class);
		if (!pm.isIgnoringBatteryOptimizations(pkg))
			startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse(
				"package:" + pkg)));

		session = OASession.getInstance(this);
		if (session.getLastWiFiScan().isEmpty())
			session.checkNetwork();
		else
			session.checkLocation();

		txtArrival = (TextView) findViewById(R.id.txt_enter);
		txtLeaving = (TextView) findViewById(R.id.txt_leave);
		txtTmpLeft = (TextView) findViewById(R.id.txt_left);
		rowTmpLeft = (TableRow) findViewById(R.id.row_left);
		txtScanTim = (TextView) findViewById(R.id.txt_tscan);
		txtScanRes = (TextView) findViewById(R.id.txt_lscan);
		txtDbgText = (TextView) findViewById(R.id.txt_dtext);
	}

	@Override
	protected void onResume() {
		super.onResume();

		session.getPrefs().registerOnSharedPreferenceChangeListener(this);

		updateView();
	}

	@Override
	protected void onPause() {
		super.onPause();

		session.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		if (id == R.id.action_reset) {
			final EditText input = new EditText(this);
			new AlertDialog.Builder(MainActivity.this).setTitle("Reset arrival").setView(
				input).setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
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
					} catch (Exception err) {
						Toast.makeText(MainActivity.this, err.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
			}).setCancelable(true).show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateView();
	}

	private void updateView() {
		txtArrival.setText(OASession.timeString(session.getArrival()));
		txtLeaving.setText(OASession.timeString(session.getLeaving()));
		txtTmpLeft.setText(OASession.timeString(session.getLeft()));
		rowTmpLeft.setVisibility(session.getLeft() <= 0 ? View.GONE : View.VISIBLE);
		txtScanTim.setText(OASession.timeString(session.getPrefs().getLong(OASession.PK.TSCAN, 0)));
		txtScanRes.setText(TextUtils.join("\n", session.getLastWiFiScan()));
		txtDbgText.setText(session.getPrefs().getString(OASession.PK.DEBUG, "N/A"));
	}
}