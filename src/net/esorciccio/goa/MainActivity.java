package net.esorciccio.goa;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnSharedPreferenceChangeListener {
	
	private OASession session;
	
	private TextView txtArrival;
	private TextView txtLeaving;
	private TextView txtTmpLeft;
	private TableRow rowTmpLeft;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		session = OASession.getInstance(this);
		session.checkAlarms();
		
		txtArrival = (TextView) findViewById(R.id.txt_arrival);
		txtLeaving = (TextView) findViewById(R.id.txt_leave);
		txtTmpLeft = (TextView) findViewById(R.id.txt_left);
		rowTmpLeft = (TableRow) findViewById(R.id.row_left);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
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
	}
}