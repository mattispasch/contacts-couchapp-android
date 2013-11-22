package de.mpasch.p2p_sync.android.webviews;

import java.net.URI;

import de.mpasch.p2p_sync.android.Constants;
import de.mpasch.p2p_sync.android.R;
import de.mpasch.p2p_sync.android.sync.couch2android.Couch2AndroidSyncer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class EditContact extends Activity {
	private static final String TAG = "EDIT_CONTACT";
	private static final String BASE_URL = "http://localhost:5984/contacts/_design/Contacts/no-menu.html";
	private String accountName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);

		WebView myWebView = (WebView) findViewById(R.id.webview);

		final Intent intent = getIntent();
		final String uriString = intent.getDataString();
		Log.d(TAG, "Intent URI: " + uriString);
		final Uri uri = Uri.parse(uriString);

		
		final String url;
		if (uri.getLastPathSegment().equals("contacts")) {
			url = BASE_URL + "#/new/";
		} else {
			final String id = queryContact(uri);
			url = BASE_URL + "#/edit/"
					+ id;
		}
		Log.d(TAG, "Opening URL: " + url);

		myWebView.loadUrl(url);
		// myWebView.loadUrl("http://localhost:5984/contacts/_design/Contacts/_show/detail/"
		// + id);

		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
	}

	/**
	 * Retrieves the header of the requested android-contact
	 * 
	 * @param uri2
	 * @return
	 */
	private String queryContact(Uri uri) {

		// Get Android-Contact-ID
		final Cursor contactsCursor = getContentResolver().query(uri,
				new String[] { Contacts._ID }, null, null, null);
		contactsCursor.moveToFirst();
		final String androidContactId = contactsCursor.getString(0);
		if (!contactsCursor.isLast()) {
			Log.e(TAG, "Expected just one contact for the given lookup id!");
		}
		Log.i(TAG, "Android Contact ID: " + androidContactId);

		// Get Raw Contact
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
				// .appendQueryParameter(RawContacts.ACCOUNT_TYPE,
				// Constants.ACCOUNT_TYPE)
				.appendQueryParameter(RawContacts.CONTACT_ID, androidContactId)
				.build();
		final Cursor cursor = getContentResolver().query(
				rawContactUri,
				new String[] { RawContacts.SOURCE_ID, RawContacts.ACCOUNT_NAME,
						RawContacts.ACCOUNT_TYPE, RawContacts.CONTACT_ID },
				RawContacts.CONTACT_ID + "=?",
				new String[] { androidContactId }, null);
		try {
			cursor.moveToFirst();
			final String sourceId = cursor.getString(0);
			final String accountName = cursor.getString(1);
			final String accountType = cursor.getString(2);

			if (!Constants.ACCOUNT_TYPE.equals(accountType)) {
				Log.e(TAG, "ERROR: AccountType is not as expected, was: "
						+ accountType + ", expected: " + Constants.ACCOUNT_TYPE);
			}
			if (!cursor.isLast()) {
				Log.e(TAG, "ERROR: more than one rawContact found..");
			}

			this.accountName = accountName;
			return sourceId;
		} finally {
			cursor.close();
		}
	}

	@Override
	public void onPause() {
		AccountManager accountManager = AccountManager
				.get(getApplicationContext());
		final Account[] accounts = accountManager
				.getAccountsByType(Constants.ACCOUNT_TYPE);

		if (accounts.length != 1) {
			Log.e(TAG, "ERROR: Excepted one account of this type, got: "
					+ accounts.length);
		}

		final SyncResult syncResult = new SyncResult();
		final Couch2AndroidSyncer couch2AndroidSyncer = new Couch2AndroidSyncer(
				getApplicationContext());
		couch2AndroidSyncer.sync(accounts[0], syncResult);

		super.onPause();
	}
}
