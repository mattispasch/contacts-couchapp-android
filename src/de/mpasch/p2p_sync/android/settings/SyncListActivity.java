package de.mpasch.p2p_sync.android.settings;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import de.mpasch.p2p_sync.android.MyApplication;
import de.mpasch.p2p_sync.android.R;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;
import de.mpasch.p2p_sync.android.httpservice.CBClientTestClass;

public class SyncListActivity extends ListActivity {
	private static final String TAG = SyncListActivity.class.getSimpleName();
	SyncListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// start CouchDB Service TODO: automatic start?:
		CBClientTestClass cbClientTestClass = new CBClientTestClass(
				getApplicationContext());
		cbClientTestClass.doBindService();

		// setUp this activity:
		final DatabaseDAO db = ((MyApplication) getApplication()).getDatabase();
		adapter = new SyncListAdapter(getApplicationContext(), db);
		setListAdapter(adapter);		
		// List is filled in "onResume()"
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// fill list
		adapter.resume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		adapter.cancel();
	}
	
	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
		Log.i(TAG, "Clicked on Item on position: " + position);
		
		// get ID, since id is a long, we can't use the supplied parameter
		final SyncPeer item = adapter.getItem(position);
		final String couchID = item.getId();
		
		final Intent editIntent = new Intent();
		editIntent.setClass(getApplicationContext(), SyncPeerEditActivity.class);
		editIntent.putExtra(SyncPeerEditActivity.INTENT_EXTRA_ID, couchID);
		
		startActivity(editIntent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_peer_sync, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    switch (item.getItemId()) {
	        case R.id.action_add_sync_peer:
	        	addSyncPeer();
	        	return true;
	        default:
	            return false;
	    }
	}

	private void addSyncPeer() {
		Log.i(TAG, "Menu Option 'Add' selected!");
		final Intent editIntent = new Intent();
		editIntent.setClass(getApplicationContext(), SyncPeerEditActivity.class);
//		editIntent.putExtra(SyncPeerEditActivity.INTENT_EXTRA_ID, couchID);
		
		startActivity(editIntent);
	}
	

}
