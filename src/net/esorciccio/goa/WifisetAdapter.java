package net.esorciccio.goa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.esorciccio.wta.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class WifisetAdapter extends BaseAdapter {
	
	private final OASession session;
	private final LayoutInflater inflater;
	
	private List<WifiSet> wifiset;
	
	public WifisetAdapter(Context context) {
		super();
		
		session = OASession.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		wifiset = new ArrayList<WifiSet>();
		for (String s: session.getWifiSet())
			wifiset.add(WifiSet.create(s, true));
	}
	
	@Override
	public int getCount() {
		return wifiset.size();
	}
	
	@Override
	public WifiSet getItem(int position) {
		return wifiset.get(position);
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
			view = inflater.inflate(R.layout.item_wifiset, parent, false);
			vh = new ViewHolder();
			vh.chk = (CheckBox) view.findViewById(R.id.chk_wifiset);
			view.setTag(vh);
			//
			vh.chk.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					int pos;
					try {
						pos = (Integer) buttonView.getTag();
					} catch (Exception err) {
						return; // wtf?
					}
					wifiset.get(pos).listen = isChecked;
				}
			});
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.chk.setTag(Integer.valueOf(position));
		WifiSet item = getItem(position);
		vh.chk.setText(item.ssid);
		vh.chk.setChecked(item.listen);
		return view;
	}
	
	public void addNetworks(Set<String> ssids) {
		Set<String> chk = getWifiset(false);
		for (String s: ssids)
			if (!chk.contains(s))
				wifiset.add(WifiSet.create(s, false));
		notifyDataSetChanged();
	}
	
	public Set<String> getWifiset(boolean selected) {
		Set<String> res = new HashSet<String>();
		for (WifiSet ws: wifiset)
			if (!selected || ws.listen)
				res.add(ws.ssid);
		return res;
	}
	
	static class WifiSet {
		String ssid;
		boolean listen;
		
		static WifiSet create(String name, boolean check) {
			WifiSet res = new WifiSet();
			res.ssid = name;
			res.listen = check;
			return res;
		}
	}
	
	static class ViewHolder {
		CheckBox chk;
	}
}