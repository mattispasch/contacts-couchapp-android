package de.mpasch.p2p_sync.android.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import de.mpasch.p2p_sync.android.sync.couch2android.Couch2AndroidSyncer;

/**
 * This class implements an android SyncAdapter
 * @author mpasch
 *
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "p2p-SyncAdapter";



	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		Log.i(TAG, "SyncAdapter()");
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, "onPerformSync started");
		
		Couch2AndroidSyncer couch2AndroidSyncer = new Couch2AndroidSyncer(getContext());
		couch2AndroidSyncer.sync(account, syncResult);
		Log.i(TAG, "Sync finished.");
	}

	



	
}
