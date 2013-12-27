package de.mpasch.p2p_sync.android.sync;

import java.util.Date;
import java.util.List;
import java.util.Stack;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;
import de.mpasch.p2p_sync.android.settings.SyncPeer;
import de.mpasch.p2p_sync.android.settings.SyncPeerRepository;
import de.mpasch.p2p_sync.android.sync.couch2android.Couch2AndroidSyncer;

/**
 * This class implements an android SyncAdapter
 * @author mpasch
 *
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "p2p-SyncAdapter";
	private final DatabaseDAO db;



	public SyncAdapter(Context context, DatabaseDAO db, boolean autoInitialize) {
		super(context, autoInitialize);
		Log.i(TAG, "SyncAdapter()");
		this.db = db;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, "onPerformSync started");
		
		localsync(account, syncResult);
				
		final boolean changed = trySyncPeers();
		
		if(changed) {
			localsync(account, syncResult);			
		}
		
		Log.i(TAG, "Sync finished.");
	}

	private boolean trySyncPeers() {
		final DatabaseDAO database = new DatabaseDAO();
		database.start(getContext());
		final SyncPeerRepository repo = database.getSyncPeerRepository();
		
		final List<SyncPeer> all = repo.getAll();
		
		boolean changed = false;
		
		for (final SyncPeer syncPeer : all) {
			try { 
				Log.i(TAG, "Replication to " + syncPeer.getName() + " started...");
				
				changed |= database.replicateContacts(syncPeer.getRemoteUrl());
				
				final String now = new Date().toLocaleString();
				syncPeer.setLastSuccessfulSync(now);
				
				repo.update(syncPeer);
				
				Log.i(TAG, "Replication to " + syncPeer.getName() + " finished.");
			} catch(Exception e) {
				Log.i(TAG, "Replication to " + syncPeer.getName() + " failed:", e);
			}
		}
		return changed;
	}

	private void localsync(Account account, SyncResult syncResult) {
		Log.i(TAG, "Local sync started.");
		
		final Couch2AndroidSyncer couch2AndroidSyncer = new Couch2AndroidSyncer(getContext(), db);
		couch2AndroidSyncer.sync(account, syncResult);
		Log.i(TAG, "Local sync finished.");
	}

	



	
}
