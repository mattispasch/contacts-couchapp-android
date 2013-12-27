package de.mpasch.p2p_sync.android.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.mpasch.p2p_sync.android.MyApplication;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;

public class SyncService extends Service {
	private static final Object sSyncAdapterLock = new Object();

	private static final String TAG = "SyncService";

	private static SyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate()");
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				final DatabaseDAO db = ((MyApplication) getApplication()).getDatabase();
				sSyncAdapter = new SyncAdapter(getApplicationContext(), db, true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

}
