package net.esorciccio.soa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import net.esorciccio.soa.OASession.PK;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class OAReceiver extends BroadcastReceiver implements BluetoothProfile.ServiceListener {
	private static final String TAG = "OAReceiver";
	
	private static final int NOTIF_LEAVE = 1;
	private static final int NOTIF_BLUNC = 2;
	private static final int NOTIF_ELUNC = 3;
	private static final int NOTIF_CLEAN = 4;
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	private static final int VFLAGS = AudioManager.FLAG_PLAY_SOUND + AudioManager.FLAG_SHOW_UI;
	
	public static final String REQ_CC = "net.esorciccio.soa.REQUEST_CACHE_CLEAR";
	public static final String REQ_VD = "net.esorciccio.soa.REQUEST_VOLUME_DOWN";
	public static final String REQ_VU = "net.esorciccio.soa.REQUEST_VOLUME_UP";
	public static final String REQ_E3 = "net.esorciccio.soa.REQUEST_DIALOG_TRE";
	
	private static AudioManager audioMan(Context context) {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}
	
	private static Notification getNotif(Context context, int title, int text) {
		return getNotif(context, context.getString(title), context.getString(text));
	}
	
	private static Notification getNotif(Context context, String title, String text) {
		Toast.makeText(context, title, Toast.LENGTH_LONG).show();
		return new NotificationCompat.Builder(context).setSound(NOTIF_SOUND).setSmallIcon(
			R.drawable.notif_alarm).setContentTitle(title).setContentText(text).build();
	}
	
	private OASession session;
	private BluetoothDevice btDevice;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		session = OASession.getInstance(context);
		String act = intent.getAction();
		Log.v(getClass().getSimpleName(), act);
		if (act.equals("android.intent.action.BOOT_COMPLETED")) {
			session.checkAlarms();
			session.checkNetwork();
			session.checkBluetooth();
		} else if (act.equals("android.net.conn.CONNECTIVITY_CHANGE") ||
			act.equals("android.net.wifi.WIFI_STATE_CHANGED") ||
			act.equals("android.net.wifi.STATE_CHANGE")) {
			session.checkNetwork();
		} else if (act.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
			int bton = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
			OASession.isBTEnabled = bton == BluetoothAdapter.STATE_ON;
		} else if (act.equals("android.net.wifi.SCAN_RESULTS")) {
			List<ScanResult> wifis = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults();
			Set<String> ssids;
			boolean res = false;
			// office
			ssids = session.getWifiSet(PK.WIFIS);
			for (ScanResult sr : wifis)
				if (ssids.contains(sr.SSID)) {
					res = true;
					break;
				}
			session.setInOffice(res);
			/*
			// bluetooth
			String btauto = session.getBTACDevice();
			if (!TextUtils.isEmpty(btauto) && OASession.isBTEnabled && !OASession.isBTConnected) {
				res = false;
				ssids = session.getWifiSet(PK.WIFIH);
				for (ScanResult sr : wifis)
					if (ssids.contains(sr.SSID)) {
						res = true;
						break;
					}
				if (res) {
					BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
					btDevice = ba.getRemoteDevice(session.getBTACDevice());
					ba.getProfileProxy(context, this, BluetoothProfile.A2DP);
				}
			}
			*/
		} else if (act.equals(OASession.AC.BLUNC)) {
			nm.notify(NOTIF_BLUNC, getNotif(context, R.string.msg_blunc_title, R.string.msg_blunc_text));
		} else if (act.equals(OASession.AC.ELUNC)) {
			nm.notify(NOTIF_ELUNC, getNotif(context, R.string.msg_elunc_title, R.string.msg_elunc_text));
			nm.cancel(NOTIF_BLUNC);
		} else if (act.equals(OASession.AC.LEAVE)) {
			nm.notify(NOTIF_LEAVE, getNotif(context, context.getString(R.string.msg_leave_title),
				context.getString(R.string.msg_leave_text) + " " + DateUtils.getRelativeTimeSpanString(
				session.getLeaving(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()));
			nm.cancel(NOTIF_BLUNC);
			nm.cancel(NOTIF_ELUNC);
		} else if (act.equals(OASession.AC.CLEAN)) {
			nm.notify(NOTIF_CLEAN, getNotif(context, R.string.msg_clean_title, R.string.msg_clean_text));
		} else if (act.equals(REQ_CC)) {
			PackageManager pm = context.getPackageManager();
			for (Method m : pm.getClass().getDeclaredMethods())
				if (m.getName().equals("freeStorageAndNotify")) {
					try {
						m.invoke(pm, Integer.MAX_VALUE, null);
						m.invoke(pm, Long.MAX_VALUE, null);
					} catch (Exception e) {
						Log.e(this.getClass().getSimpleName(), "onReceive", e); // permission problem?
					}
					break;
				}
			Toast.makeText(context, "request sent", Toast.LENGTH_SHORT).show();
		} else if (act.equals(REQ_VU)) {
			audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, VFLAGS);
		} else if (act.equals(REQ_VD)) {
			audioMan(context).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, VFLAGS);
		} else if (act.equals(REQ_E3)) {
			Toast.makeText(context, DateUtils.getRelativeTimeSpanString(session.getLast3oktime(),
				System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(), Toast.LENGTH_SHORT).show();
			if (session.isLast3failed())
				TreActivity.lastrun = -1;
		} else if (act.equals("android.bluetooth.device.action.ACL_CONNECTED")) {
	        OASession.isBTConnected = true;
	    } else if (act.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
	    	OASession.isBTConnected = false;
	    }
	}
	
	@Override
	public void onServiceConnected(int profile, BluetoothProfile proxy) {
		Log.v(TAG, "Connecting bluetooth device " + btDevice.getName());
		try {
			Method method = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
			method.setAccessible(true);
			method.invoke((BluetoothA2dp) proxy, btDevice);
		} catch (NoSuchMethodException ex) {
			Log.e(TAG, "Unable to find connect(BluetoothDevice) method in BluetoothA2dp proxy.");
		} catch (InvocationTargetException ex) {
			Log.e(TAG, "Unable to invoke connect(BluetoothDevice) method on proxy. " + ex.toString());
		} catch (IllegalAccessException ex) {
			Log.e(TAG, "Illegal Access! " + ex.toString());
		}
	}
	
	@Override
	public void onServiceDisconnected(int profile) {
		// nothing to do.
	}
}