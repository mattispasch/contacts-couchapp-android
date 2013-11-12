package de.mpasch.p2p_sync.android;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.mpasch.p2p_sync.android.data.Contact;
import de.mpasch.p2p_sync.android.db.DBListener;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;
import de.mpasch.p2p_sync.android.httpservice.CBClientTestClass;
import de.mpasch.p2p_sync.android.httpservice.CBService;

public class ManualSyncActivity extends Activity {

	public static final String TAG = "ManualSyncActivity";

	private DatabaseDAO database;

	// View
	private Button syncButton;
	private Button showContactButton;
	private TextView messageText;

	private DBListener dbListener = new DBListener() {

		@Override
		public void onConnected() {
			syncButton.setClickable(true);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manual_sync);

		// start CouchDB Service:
		CBClientTestClass cbClientTestClass = new CBClientTestClass(getApplicationContext());
		cbClientTestClass.doBindService();
		
		// Initialize DB
		database = new DatabaseDAO(dbListener);
		database.start(this);

		// Setup View
		syncButton = (Button) findViewById(R.id.button_sync);
		showContactButton = (Button) findViewById(R.id.show_contact);
		messageText = (TextView) findViewById(R.id.textView1);

		// deactivate Buttons until DB is ready
		syncButton.setClickable(false);
		showContactButton.setClickable(false);

		syncButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				database.startPull();
				database.startPush();
			}
		});
		showContactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final JsonNode c = database
						.getContact("aff9dbb4-8b6d-45d1-9e3c-3ab68da2a58e");
				// final Contact c = database.getContacts().get(15);
				messageText.setText(c.toString());

			final String name = c.get("name").asText();
				
				final ArrayList<ContentValues> contactData = new ArrayList<ContentValues>();
				final ContentValues header = new ContentValues();

				header.put(RawContacts.ACCOUNT_TYPE, "p2p-sync");
				header.put(RawContacts.ACCOUNT_NAME, "mattis@example.com");
				header.put(RawContacts.CONTACT_ID, c.get("_id").asText());
				header.put(StructuredName.DISPLAY_NAME, name);
				
				contactData.add(header);
				
				final Intent insert = new Intent(ContactsContract.Intents.Insert.ACTION);
				insert.setType(ContactsContract.RawContacts.CONTENT_TYPE);
				insert.putExtra(ContactsContract.Intents.Insert.NAME, name);
				insert.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, contactData);
				
				startActivity(insert);
				
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.manual_sync, menu);
		return true;
	}

}
