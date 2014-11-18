package net.esorciccio.soa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

public class OAService extends IntentService {
	
	private OASession session;
	
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
		session = OASession.getInstance(this);
		
		boolean saved = false;
		
		try {
			while (!terminated) {
				AppWidgetManager.getInstance(this).updateAppWidget(new ComponentName(this, OAWidgetLarge.class),
					buildUpdate(this));
				
				if (!saved) {
					
					//String text = getPage("http://areaclienti3.tre.it/133/controllo-costi.jsp");
					//String text = getPage("http://ac3.tre.it/133/profilo.jsp");
					
					String text = getPage("/133/costi-e-soglie.jsp");
					Log.v("OAService", text);
					if (text != null)
					try {
						//File file = new File(Environment.getExternalStorageDirectory(), "Download/tre.html");
						File file = new File(Environment.getExternalStorageDirectory(), "tre.html");
						FileOutputStream fos = new FileOutputStream(file);
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);
				        outputStreamWriter.write(text);
				        outputStreamWriter.close();
				        /*
						fos.write(mediaTagBuffer);
						fos.flush();
						fos.close();
						*/
					} catch (Exception err) {
						Log.e("OAService", "save file", err);
					}
					
			        saved = true;
				}
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					terminated = true;
				}
			}
		} finally {
			stopSelf();
		}
	}
	
	private RemoteViews buildUpdate(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		@SuppressWarnings("deprecation")
		String alarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		// views.setTextViewText(R.id.txt_left, OASession.timeString(session.getLeft()));
		// views.setTextViewText(R.id.txt_clock, "23:48");
		views.setTextViewText(R.id.txt_clock, OASession.timeString(System.currentTimeMillis()));
		views.setTextViewText(R.id.txt_today, OASession.dateString(System.currentTimeMillis(), "EEE d MMM"));
		views.setTextViewText(R.id.txt_alarm, alarm);
		
		//
		PendingIntent pendingIntent;
		
		Intent mainIntent = new Intent(context, MainActivity.class);
		pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
		views.setOnClickPendingIntent(R.id.frm_left, pendingIntent);
		
		try {
			Intent clockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock");
			context.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
			clockIntent.setComponent(cn);
			pendingIntent = PendingIntent.getActivity(context, 0, clockIntent, 0);
			views.setOnClickPendingIntent(R.id.txt_clock, pendingIntent);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		return views;
	}
	
	//private final static String USERAGENT = "Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/LRX210) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36";
	private final static String USERAGENT = "Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/LRX210) iPhone Sfaccimme AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36";
	private final static String BASEURL3 = "http://ac3.tre.it";
	
	private static String getPage(String addr) {
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpClientParams.setRedirecting(httpParams, true);
			HttpClient httpClient = new DefaultHttpClient(httpParams);
			httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
			HttpContext context = new BasicHttpContext();
			HttpGet request;
			HttpResponse response;
			
			String lastUrl;
			String redirect = addr;
			while (true) {
				lastUrl = BASEURL3 + redirect;
				Log.v("OAService", "opening " + lastUrl);
				request = new HttpGet(lastUrl);
				request.setHeader("User-Agent", USERAGENT);
				response = httpClient.execute(request, context);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
					return null;
				HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
				if (!currentReq.getURI().toString().equals(redirect)) {
					redirect = currentReq.getURI().toString();
				} else {
					HttpEntity entity = response.getEntity();
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
					InputStream stream = bufHttpEntity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					reader.close();
					stream.close();
					return sb.toString();
				}
			}
			
			/*
			request = new HttpGet(BASEURL3 + addr);
			request.setHeader("User-Agent", USERAGENT);
			response = httpClient.execute(request, context);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
				return null;
			
			HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
			String redirect = currentReq.getURI().toString();
			Log.v("OAService", "redirected to " + redirect);
			
			request = new HttpGet(BASEURL3 + redirect);
			request.setHeader("User-Agent", USERAGENT);
			response = httpClient.execute(request, context);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
				return null;
			
			HttpEntity entity = response.getEntity();
			BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
			InputStream stream = bufHttpEntity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			stream.close();
			return sb.toString();
			*/
			
			/*
			URL url = new URL(addr);
			URLConnection connection = url.openConnection();
			HttpGet httpRequest = new HttpGet(url.toURI());
			httpRequest.setHeader("User-Agent", "iPhone");
			HttpContext localContext = new BasicHttpContext();
			HttpParams httpParams = new BasicHttpParams();
			HttpClientParams.setRedirecting(httpParams, true);
			HttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpResponse response = httpclient.execute(httpRequest, localContext);
			HttpEntity entity = response.getEntity();
			BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
			InputStream stream = bufHttpEntity.getContent();
			String ct = connection.getContentType();
			BufferedReader reader;
			if (ct.indexOf("charset=") != -1) {
				ct = ct.substring(ct.indexOf("charset=") + 8);
				reader = new BufferedReader(new InputStreamReader(stream, ct));
			} else {
				reader = new BufferedReader(new InputStreamReader(stream));
			}
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			stream.close();
			return sb.toString();
			*/
			
		} catch (Exception err) {
			Log.e("OAService", "getPage", err);
			return null;
		}
	}
}