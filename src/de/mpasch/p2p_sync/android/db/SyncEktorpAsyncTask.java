package de.mpasch.p2p_sync.android.db;

import org.ektorp.DbAccessException;
import org.ektorp.android.util.EktorpAsyncTask;

import de.mpasch.p2p_sync.android.ManualSyncActivity;

import android.util.Log;

public abstract class SyncEktorpAsyncTask extends EktorpAsyncTask {

	@Override
	protected void onDbAccessException(DbAccessException dbAccessException) {
		Log.e(ManualSyncActivity.TAG, "DbAccessException in background", dbAccessException);
	}

}
