package net.esorciccio.soa.pref;

import net.esorciccio.soa.R;
import net.esorciccio.soa.serv.OASession;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class DaysetsDialog extends DialogPreference {
	
	private final OASession session;
	private final DaysetAdapter adapter;
	
	public DaysetsDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setDialogLayoutResource(R.layout.dialog_daysets);
		
		session = OASession.getInstance(context);
		adapter = new DaysetAdapter(context);
	}
	
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		ListView lv = (ListView) view.findViewById(R.id.lst_daysets);
		lv.setAdapter(adapter);
		return view;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult)
			session.setWeekHours(adapter.getDaysets());
	}
}