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
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TreActivity extends Activity {
	private static final String TAG = "TreActivity";
	
	private OASession session;
	private WebView wv;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		session = OASession.getInstance(this);
		session.setLast3time(System.currentTimeMillis());
		
		wv = new WebView(this);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.addJavascriptInterface(new JSCheck3(), "HTMLOUT");
		//wv.setWebChromeClient(new WebChromeClient());
		wv.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				wv.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
			}
		});
		wv.loadUrl("http://ac3.tre.it/133/costi-e-soglie.jsp");
	}
	
	class JSCheck3 {
		@JavascriptInterface
		public void processHTML(String html) {
			if (TextUtils.isEmpty(html))
				return;
			Log.v(TAG, "checking page...");
			try {
				Document doc = Jsoup.parse(html);
				Elements lst = doc.getElementsByClass("box_Credito");
				String credito = lst.first().child(0).child(0).child(0).text().replace(" ", ""); // formato ok: "999.99€"
				session.setLast3cred(credito);
				lst = doc.getElementsByClass("box_Note");
				String traffico = lst.first().text();
				traffico = traffico.substring(0, traffico.indexOf("GB ") + 2);
				traffico = traffico.substring(traffico.lastIndexOf(" ") + 1); // formato ok: "9,99GB"
				session.setLast3traf(traffico);
				finish(); // dismiss the activity
			} catch (Exception err) {
				session.setLast3fail(err.getLocalizedMessage());
				Log.e(TAG, "processHTML", err);
				savePage(html);
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