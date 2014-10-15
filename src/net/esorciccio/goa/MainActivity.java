package net.esorciccio.goa;

import java.text.DateFormat;

import net.esorciccio.goa.OASession.PK;
import net.esorciccio.wta.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements OnSharedPreferenceChangeListener {
	
	private OASession session;
	
	private TextView txtArrival;
	private TextView txtLeaving;
	private TextView txtTmpLeft;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		session = OASession.getInstance(this);
		session.resetAlarms();
		
		txtArrival = (TextView) findViewById(R.id.txt_arrival);
		txtLeaving = (TextView) findViewById(R.id.txt_leave);
		txtTmpLeft = (TextView) findViewById(R.id.txt_left);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		txtArrival.setText(DateUtils.formatSameDayTime(session.getArrival(), System.currentTimeMillis(),
			DateFormat.SHORT, DateFormat.MEDIUM));
		txtLeaving.setText(DateUtils.formatSameDayTime(session.getLeaving(), System.currentTimeMillis(),
			DateFormat.SHORT, DateFormat.MEDIUM));
		txtTmpLeft.setText(DateUtils.formatSameDayTime(session.getLeft(), System.currentTimeMillis(),
			DateFormat.SHORT, DateFormat.MEDIUM));
		
		session.getPrefs().registerOnSharedPreferenceChangeListener(this);
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
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.ARRIV) || key.equals(PK.HOURS)) {
			txtArrival.setText(DateUtils.formatSameDayTime(session.getArrival(), System.currentTimeMillis(),
				DateFormat.SHORT, DateFormat.MEDIUM));
			txtLeaving.setText(DateUtils.formatSameDayTime(session.getLeaving(), System.currentTimeMillis(),
				DateFormat.SHORT, DateFormat.MEDIUM));
		} else if (key.equals(PK.LEAVE)) {
			txtTmpLeft.setText(DateUtils.formatSameDayTime(session.getLeft(), System.currentTimeMillis(),
				DateFormat.SHORT, DateFormat.MEDIUM));
		}
	}
}
