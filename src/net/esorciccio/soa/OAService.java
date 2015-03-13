package net.esorciccio.soa;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class OAService extends IntentService {
	private static final String TAG = "OAService";
	
	private OASession session;
	private ComponentName compName;
	private Intent treIntent;
	private boolean terminated = false;
	
	public OAService() {
		super("OAService");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(TAG, "onHandleIntent");
		session = OASession.getInstance(this);
		session.checkAlarms();
		session.checkNetwork();
		session.checkBluetooth();
		compName = new ComponentName(this, OAWidgetLarge.class);
		treIntent = new Intent(getBaseContext(), TreActivity.class);
		treIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			while (!terminated) {
				if (pm.isPowerSaveMode() || !pm.isInteractive())
					Thread.sleep(5000);
				else {
					AppWidgetManager.getInstance(this).updateAppWidget(compName, buildUpdate(this));
					/*
					if (session.canTreCheck())
						getApplication().startActivity(treIntent);
					*/
					Thread.sleep(1500);
				}
			}
		} catch (InterruptedException e) {
			terminated = true;
		} finally {
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		/*
		if (wv != null) {
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			wm.removeView(wv);
		}
		*/
	}
	
	private RemoteViews buildUpdate(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		
		//views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		long ttogo = session.getLeaving();
		long tleft = session.getLeft();
		if (tleft > 0 && tleft < ttogo) {
			views.setTextColor(R.id.txt_leaving, Color.CYAN);
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(tleft));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_action_av_fast_rewind, 0, 0, 0);
		} else {
			views.setTextColor(R.id.txt_leaving, Color.WHITE);
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(ttogo));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_action_av_skip_previous, 0, 0, 0);
		}
		
		views.setTextViewText(R.id.txt_clock, OASession.timeString(System.currentTimeMillis()));
		
		views.setTextViewText(R.id.txt_today, OASession.dateString(System.currentTimeMillis(), "EEE d MMM"));
		
		@SuppressWarnings("deprecation")
		String alarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		if (TextUtils.isEmpty(alarm)) {
			views.setTextViewText(R.id.txt_alarm, "nessuna");
			views.setTextViewCompoundDrawables(R.id.txt_alarm, 0, 0, R.drawable.ic_action_alarm_off, 0);
		} else {
			views.setTextViewText(R.id.txt_alarm, alarm);
			views.setTextViewCompoundDrawables(R.id.txt_alarm, 0, 0, R.drawable.ic_action_alarm_set, 0);
		}
		
		int ci = 0;
		if (TreActivity.running)
			ci = R.drawable.ic_action_check;
		else if (OASession.isOnWIFI)
			ci = R.drawable.ic_action_wifi;
		else if (!OASession.isOn3G)
			ci = R.drawable.ic_action_noconn;
		else if (OASession.isRoaming)
			ci = R.drawable.ic_action_roaming;
		else if (session.isLast3failed())
			ci = R.drawable.ic_action_error;
		
		views.setTextViewText(R.id.txt_tre, session.getLast3cred() + " / " + session.getLast3traf());
		views.setTextViewCompoundDrawables(R.id.txt_tre, R.drawable.ic_action_h3g, 0, ci, 0);
		
		views.setOnClickPendingIntent(R.id.txt_tre, PendingIntent.getBroadcast(context, 0,
			new Intent(OAReceiver.REQ_E3), 0));
		
		views.setOnClickPendingIntent(R.id.txt_voldn, PendingIntent.getBroadcast(context, 0,
			new Intent(OAReceiver.REQ_VD), 0));
		
		views.setOnClickPendingIntent(R.id.txt_volup, PendingIntent.getBroadcast(context, 0,
			new Intent(OAReceiver.REQ_VU), 0));
		
		views.setOnClickPendingIntent(R.id.frm_left, PendingIntent.getActivity(context, 0,
			new Intent(context, MainActivity.class), 0));
		
		try {
			Intent clockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock");
			context.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
			clockIntent.setComponent(cn);
			views.setOnClickPendingIntent(R.id.txt_clock, PendingIntent.getActivity(context, 0, clockIntent, 0));
		} catch (Exception err) {
			Log.e(TAG, "clockIntent", err);
		}
		
		return views;
	}
	
	/*
	private WebView wv;
	
	@SuppressLint("SetJavaScriptEnabled")
	private void checkTre() {
		Log.v(TAG, "checkTre");
		if (wv == null) {
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
				PixelFormat.TRANSLUCENT);
			lp.gravity = Gravity.TOP | Gravity.START;
			lp.x = 0;
			lp.y = 0;
			lp.width = 200;
			lp.height = 200;
			wv = new WebView(getApplicationContext());
			wv.getSettings().setJavaScriptEnabled(true);
			wv.addJavascriptInterface(new JSCheck3(), "HTMLOUT");
			wv.setWebChromeClient(new WebChromeClient());
			wv.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					Log.v(TAG, "onPageFinished");
					time3 = System.currentTimeMillis();
					running3 = false;
					wv.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
				}
			});
			wm.addView(wv, lp);
		}
		running3 = true;
		wv.loadUrl("http://ac3.tre.it/133/costi-e-soglie.jsp");
	}
	
	class JSCheck3 {
		@JavascriptInterface
		public void processHTML(String html) {
			if (TextUtils.isEmpty(html))
				return;
			Log.v(TAG, "processHTML");
			try {
				Document doc = Jsoup.parse(html);
				Elements lst = doc.getElementsByClass("box_Credito");
				String credito = lst.first().child(0).child(0).child(0).text().replace(" ", ""); // formato ok: "999.99€"
				Log.v(TAG, "credito: " + credito);
				session.setLast3cred(credito);
				lst = doc.getElementsByClass("box_Note");
				String traffico = lst.first().text();
				traffico = traffico.substring(0, traffico.indexOf("GB ") + 2);
				traffico = traffico.substring(traffico.lastIndexOf(" ") + 1); // formato ok: "9,99GB"
				Log.v(TAG, "traffico: " + traffico);
				session.setLast3traf(traffico);
			} catch (Exception err) {
				session.setLast3fail(err.getLocalizedMessage());
				Log.e(TAG, "processHTML", err);
				savePage(html);
			}
		}
		public void savePage(String html) {
			try {
				File file = new File(Environment.getExternalStorageDirectory(), "testTre.html");
	            file.createNewFile();
	            FileOutputStream stream = new FileOutputStream(file);
	            OutputStreamWriter writer = new OutputStreamWriter(stream);
	            writer.append(html);
	            writer.close();
	            stream.close();
	            Log.v("parser", "saved testTre.html");
			} catch (Exception err) {
				Log.e("parser", "saveTestPage", err);
			}
		}
	}
	*/
}