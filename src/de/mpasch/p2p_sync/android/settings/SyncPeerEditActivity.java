package de.mpasch.p2p_sync.android.settings;

import java.util.UUID;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import de.mpasch.p2p_sync.android.MyApplication;
import de.mpasch.p2p_sync.android.R;
import de.mpasch.p2p_sync.android.db.DBListener;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;

public class SyncPeerEditActivity extends Activity {

	public static final String INTENT_EXTRA_ID = "id";

	private static final String TAG = SyncPeerEditActivity.class
			.getSimpleName();

	private SyncPeerRepository repo;
	private DatabaseDAO db;
	private SyncPeer syncPeer;

	// Views
	private TextView textId;
	private EditText editUrl;
	private EditText editName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_peer_edit);
		initializeView();

		if (getIntent() != null) {
			final String id = getIntent().getStringExtra(INTENT_EXTRA_ID);

			// start DB
			db = ((MyApplication) getApplication()).getDatabase();
			repo = db.getSyncPeerRepository();
			
			if (id == null) {
				syncPeer = null;
			} else {
				syncPeer = repo.get(id);
				populateView(syncPeer);
			}

		} else {
			syncPeer = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		saveSyncPeer();

		db.close();

		// make sure, the object is loaded from db next time this activity is
		// started
		finish();
	}

	private void saveSyncPeer() {
		if (syncPeer == null) {
			syncPeer = new SyncPeer();
			syncPeer.setId(UUID.randomUUID().toString());
		}
		syncPeer.setName(editName.getText().toString());
		syncPeer.setRemoteUrl(editUrl.getText().toString());

		repo.update(syncPeer);
		Log.i(TAG, "SyncPeer updated: " + syncPeer.getName());
	}

	private void initializeView() {
		editName = (EditText) findViewById(R.id.editText_name);
		editUrl = (EditText) findViewById(R.id.editText_url);
		// showContactButton = (Button) findViewById(R.id.show_contact);
		textId = (TextView) findViewById(R.id.textViewId);
	}

	private void populateView(SyncPeer syncPeer) {

		editName.setText(syncPeer.getName());
		editUrl.setText(syncPeer.getRemoteUrl());
		textId.setText("ID: " + syncPeer.getId());

	}
}
