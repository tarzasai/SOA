package net.esorciccio.soa;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class BTListPreference extends ListPreference {
	
	public BTListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		if (!ba.isEnabled())
			Toast.makeText(getContext(), R.string.msg_bt_off, Toast.LENGTH_LONG).show();
		Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
		CharSequence[] entries = new CharSequence[pairedDevices.size()];
		CharSequence[] entryValues = new CharSequence[pairedDevices.size()];
		int i = 0;
		for (BluetoothDevice dev : pairedDevices) {
			entries[i] = dev.getName();
			entryValues[i] = dev.getAddress();
			i++;
		}
		setEntries(entries);
		setEntryValues(entryValues);
	}
	
	public BTListPreference(Context context) {
		this(context, null);
	}
}