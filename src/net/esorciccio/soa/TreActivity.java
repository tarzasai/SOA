package net.esorciccio.soa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TreActivity extends Activity {
	private static final String TAG = "TreActivity";
	
	private WebView wv;

	public static long lastrun = 0;
	public static boolean running = false;
	public static String errore = null;
	public static String credito = null;
	public static String traffico = null;
	
	public static boolean failed() {
		return !TextUtils.isEmpty(errore);
	}
	
	public static boolean canRun() {
		return !failed() && !running && (lastrun <= 0 || ((System.currentTimeMillis() - lastrun) > (30 * 60000)));
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!canRun())
			finish();
		else {
			lastrun = System.currentTimeMillis();
			running = true;
			wv = new WebView(this);
			wv.getSettings().setJavaScriptEnabled(true);
			wv.addJavascriptInterface(new JSCheck3(), "HTMLOUT");
			wv.setWebChromeClient(new WebChromeClient());
			wv.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					wv.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
				}
				@Override
				public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
					errore = description;
					lastrun = System.currentTimeMillis();
					running = false;
					finish();
				}
			});
			wv.loadUrl("http://ac3.tre.it/133/costi-e-soglie.jsp");
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		running = false;
	}
	
	class JSCheck3 {
		@JavascriptInterface
		public void processHTML(String html) {
			try {
				Document doc = Jsoup.parse(html);
				Elements lst = doc.getElementsByClass("box_Credito");
				credito = lst.first().child(0).child(0).child(0).text().replace(" ", ""); // 999.99€
				lst = doc.getElementsByClass("box_Note");
				String tmp = lst.first().text();
				tmp = tmp.substring(0, tmp.indexOf("GB ") + 2);
				traffico = tmp.substring(tmp.lastIndexOf(" ") + 1); // 9,99GB
				errore = null;
			} catch (Exception err) {
				Log.e(TAG, "processHTML", err);
				errore = err.getLocalizedMessage();
				savePage(html);
			} finally {
				lastrun = System.currentTimeMillis();
				running = false;
				finish();
			}
		}
		public void savePage(String html) {
			try {
				File file = new File(Environment.getExternalStorageDirectory(), "tre_error.html");
	            file.createNewFile();
	            FileOutputStream stream = new FileOutputStream(file);
	            OutputStreamWriter writer = new OutputStreamWriter(stream);
	            writer.append(html);
	            writer.close();
	            stream.close();
	            Log.v(TAG, "saved tre_error.html");
			} catch (Exception err) {
				Log.e(TAG, "savePage", err);
			}
		}
	}
}