package de.mpasch.p2p_sync.android.httpservice;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.javascript.CBLJavaScriptViewCompiler;
import com.couchbase.cblite.listener.CBLListener;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

import de.mpasch.p2p_sync.android.R;

public class CBService extends Service {
	private static final String TAG = "CBService";

	private NotificationManager mNM;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.local_service_started;

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
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
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
		} catch (IOException e) {
			Log.e(TAG, "Error starting TDServer", e);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = "blubb"; // getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		// Notification notification = new Notification(R.drawable.ic_launcher,
		// "Couch-Service running...", System.currentTimeMillis());

		// Toast toast = Toast.makeText(getApplicationContext(),
		// "Couch-Service starting...", 3000);

		// The PendingIntent to launch our activity if the user selects this
		// notification
		// PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		// new Intent(this, LocalServiceActivities.Controller.class), 0);

		// Set the info for the views that show in the notification panel.
		// notification.setLatestEventInfo(this,
		// getText(R.string.local_service_label),
		// text, contentIntent);

		// Send the notification.
		// mNM.notify(NOTIFICATION, notification);
	}

}
