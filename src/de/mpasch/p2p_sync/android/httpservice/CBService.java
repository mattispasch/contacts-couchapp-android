package de.mpasch.p2p_sync.android.httpservice;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.javascript.CBLJavaScriptViewCompiler;
import com.couchbase.cblite.listener.CBLListener;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

public class CBService extends Service {
	private static final String TAG = "CBService";

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = null; // new LocalBinder();

	// couch internals
	protected static CBLServer server;

	// static inializer to ensure that touchdb:// URLs are handled properly
	{
		CBLURLStreamHandlerFactory.registerSelfIgnoreError();
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		String filesDir = getApplicationContext().getFilesDir()
				.getAbsolutePath();
		try {
			server = new CBLServer(filesDir);
			CBLURLStreamHandlerFactory.registerSelfIgnoreError();
			CBLView.setCompiler(new CBLJavaScriptViewCompiler());
			
			// server.getDatabaseNamed("contacts");
			int port = 5984;
			CBLListener listener = new CBLListener(server, port);
			Thread listenThread = new Thread(listener);
			listenThread.start();
			new Filters().installContactFilters(server);	
		} catch (IOException e) {
			Log.e(TAG, "Error starting TDServer", e);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
