package net.esorciccio.soa;

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
	
	public TimePreference(Context ctxt) {
		this(ctxt, null);
	}
	
	public TimePreference(Context ctxt, AttributeSet attrs) {
		this(ctxt, attrs, 0);
	}
	
	public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
		super(ctxt, attrs, defStyle);
		
		setPositiveButtonText(R.string.dlg_btn_ok);
		setNegativeButtonText(R.string.dlg_btn_cancel);
	}
	
	public static int getHour(String time) {
		return (Integer.parseInt(time.split(":")[0]));
	}
	
	public static int getMinute(String time) {
		return (Integer.parseInt(time.split(":")[1]));
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
		String time = null;
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
		return String.valueOf(lastHour) + ":" + String.valueOf(lastMinute);
	}
}