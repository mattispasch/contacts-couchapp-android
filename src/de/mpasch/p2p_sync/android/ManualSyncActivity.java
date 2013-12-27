package de.mpasch.p2p_sync.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.mpasch.p2p_sync.android.db.DBListener;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;
import de.mpasch.p2p_sync.android.httpservice.CBClientTestClass;

public class ManualSyncActivity extends Activity {

	public static final String TAG = "ManualSyncActivity";

	private DatabaseDAO database;

	// View
	private Button syncButton;
	private Button showContactButton;
	private TextView messageText;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manual_sync);

		// start CouchDB Service:
		CBClientTestClass cbClientTestClass = new CBClientTestClass(getApplicationContext());
		cbClientTestClass.doBindService();
		
		// Initialize DB
		database = ((MyApplication) getApplication()).getDatabase();
		syncButton.setClickable(true);
		
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
