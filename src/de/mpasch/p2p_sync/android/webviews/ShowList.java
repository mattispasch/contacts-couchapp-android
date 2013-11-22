package de.mpasch.p2p_sync.android.webviews;

import de.mpasch.p2p_sync.android.R;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ShowList extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);
		
		WebView myWebView = (WebView) findViewById(R.id.webview);
		myWebView.loadUrl("http://localhost:5984/contacts/_design/Contacts/_list/all/");
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
	}
}
