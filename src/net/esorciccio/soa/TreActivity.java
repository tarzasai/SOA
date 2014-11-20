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
import android.util.Log;
import android.view.WindowManager;
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
		setContentView(R.layout.activity_tre);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		
		session = OASession.getInstance(this);
		
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
	
	public static void saveTestPage(String html) {
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
	
	class JSCheck3 {
		@JavascriptInterface
		public void processHTML(String html) {
			Log.v(TAG, "checking page...");
			session.setLast3time(System.currentTimeMillis());
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
			}
		}
	}
}