package net.esorciccio.soa.data;

import net.esorciccio.soa.R;
import net.esorciccio.soa.R.id;
import net.esorciccio.soa.R.layout;
import net.esorciccio.soa.serv.OASession;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class DaysetAdapter extends BaseAdapter {
	
	private final OASession session;
	private final LayoutInflater inflater;
	private final ArrayAdapter<Integer> hours;
	
	private int[] daysets;
	
	public DaysetAdapter(Context context) {
		super();
		
		session = OASession.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		hours = new ArrayAdapter<Integer>(context, android.R.layout.simple_spinner_item,
			new Integer[] {0, 4, 5, 6, 7, 8});
		hours.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		daysets = session.getWeekHours();
	}
	
	@Override
	public int getCount() {
		return daysets.length;
	}
	
	@Override
	public Object getItem(int position) {
		return daysets[position];
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_dayset, parent, false);
			vh = new ViewHolder();
			vh.txt = (TextView) view.findViewById(R.id.txt_dayset);
			vh.spi = (Spinner) view.findViewById(R.id.spi_dayset);
			view.setTag(vh);
			//
			vh.spi.setAdapter(hours);
			vh.spi.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					int pos;
					try {
						pos = (Integer) parent.getTag();
					} catch (Exception err) {
						return; // wtf?
					}
					daysets[pos] = 0;
				}
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					int pos;
					try {
						pos = (Integer) parent.getTag();
					} catch (Exception err) {
						return; // wtf?
					}
					daysets[pos] = hours.getItem(position);
				}
			});
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.spi.setTag(Integer.valueOf(position));
		vh.spi.setSelection(hours.getPosition(daysets[position]));
		vh.txt.setText(session.getDayName(position + 2));
		return view;
	}
	
	public int[] getDaysets() {
		return daysets;
	}
	
	static class ViewHolder {
		TextView txt;
		Spinner spi;
	}
}