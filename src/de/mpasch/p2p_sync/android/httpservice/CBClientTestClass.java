package de.mpasch.p2p_sync.android.httpservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class CBClientTestClass {

	protected static final String TAG = "CBClientTestClass";
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	private Context context;
	
	public CBClientTestClass(Context context) {
		this.context = context;
	}
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        Log.d(TAG, "Connected to remote Service");
	    	
	    	// This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
//	        mCallbackText.setText("Attached.");

	   
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	    }
	};
	

	public void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
		Intent intent = new Intent(context,CBService.class);
	    context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    context.startService(intent);
	    
	    mIsBound = true;
	}
}
