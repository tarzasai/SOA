package net.esorciccio.soa.pref;

import java.util.Locale;

import net.esorciccio.soa.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
	
	private int lastHour = 0;
	private int lastMinute = 0;
	private TimePicker picker = null;
	
	public TimePreference(Context ctxt, AttributeSet attrs) {
		super(ctxt, attrs);
		
		setPositiveButtonText(R.string.dlg_btn_ok);
		setNegativeButtonText(R.string.dlg_btn_cancel);
	}
	
	@Override
	protected View onCreateDialogView() {
		picker = new TimePicker(getContext());
		picker.setIs24HourView(true);
		return (picker);
	}
	
	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		
		picker.setCurrentHour(lastHour);
		picker.setCurrentMinute(lastMinute);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			lastHour = picker.getCurrentHour();
			lastMinute = picker.getCurrentMinute();
			String time = getSummary().toString();
			if (callChangeListener(time)) {
				persistString(time);
				notifyChanged();
			}
		}
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getString(index));
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		String time;
		if (!restoreValue)
			time = defaultValue.toString();
		else if (defaultValue == null)
			time = getPersistedString("00:00");
		else
			time = getPersistedString(defaultValue.toString());
		lastHour = getHour(time);
		lastMinute = getMinute(time);
		setSummary(getSummary());
	}
	
	@Override
	public CharSequence getSummary() {
		return String.format(Locale.getDefault(), "%02d:%02d", lastHour, lastMinute);
	}
	
	private static int getHour(String time) {
		return (Integer.parseInt(time.split(":")[0]));
	}
	
	private static int getMinute(String time) {
		return (Integer.parseInt(time.split(":")[1]));
	}
}