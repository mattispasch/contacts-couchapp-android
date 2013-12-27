package de.mpasch.p2p_sync.android.settings;

import java.util.ArrayList;
import java.util.List;

import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import de.mpasch.p2p_sync.android.R;
import de.mpasch.p2p_sync.android.db.DBListener;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;

/**
 * Connects the list-view to couchbaselite
 * 
 * For performance issues, see (German):
 * http://www.webmaid.de/2010/04/listactivity-und-listadapter/
 * 
 * @author mpasch
 * 
 */
public class SyncListAdapter implements ListAdapter {

	private static final String TAG = "SyncListAdapter";

	private final DatabaseDAO db;
	private final LayoutInflater layoutInflater;

	private SyncPeerRepository repo;
	private List<SyncPeer> list = null;

	private List<DataSetObserver> dataSetObservers = new ArrayList<DataSetObserver>();
	
	private ChangesFeed feed;
	private Thread listenThread = new Thread(new Runnable() {

		@Override
		public void run() {
//			final ChangesCommand cmd = new ChangesCommand.Builder().build();

//			feed = db.getConnector().changesFeed(cmd);

//			try {
//				while (feed.isAlive()) {
//					feed.next();
					// for now we just re-retrieve the list every time there is
					// ANY change
//					retrieveList();
//				}
//			} catch (InterruptedException e) {
				// feed was killed
//			}
		}
	});

	public SyncListAdapter(Context context, DatabaseDAO db) {
		layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.db = db; 

		repo = db.getSyncPeerRepository();

		// retrieve initial list
		retrieveList();

		// start listening
//		listenThread.start();

	}

	private void retrieveList() {
		if (repo != null) {
			list = repo.getAll();
			Log.i(TAG, "Found " + list.size() + " sync Peers");
			
			for (DataSetObserver o : dataSetObservers) {
				o.onChanged();
			}
		}
	}

	public void cancel() {
		if (feed != null) {
			feed.cancel();
		}
	}

	public void resume() {
		retrieveList();
	}

	@Override
	public int getCount() {
		Log.d(TAG, "getCount() called");
		if (list == null) {
			return 0;
		}
		return list.size();
	}

	@Override
	public SyncPeer getItem(int location) {
		return list.get(location);
	}

	@Override
	public long getItemId(int location) {
		// because no Strings are supported, there is no id we could return
		// here...
		// return list.get(location).getId();
		return location;
	}

	@Override
	public View getView(int location, View reusableView, ViewGroup parent) {

		final LinearLayout view;
		if (reusableView == null || !(reusableView instanceof LinearLayout)) {
			view = (LinearLayout) layoutInflater.inflate(
					R.layout.sync_peer_list_item, parent, false);
		} else {
			view = (LinearLayout) reusableView;
		}

		TextView lineOneView = (TextView) view.findViewById(R.id.text1);
		TextView lineTwoView = (TextView) view.findViewById(R.id.text2);

		final SyncPeer peer = list.get(location);
		lineOneView.setText(peer.getName());
		lineTwoView
				.setText(peer.getLastSuccessfulSync() == null ? "never synced"
						: peer.getLastSuccessfulSync());

		return view;
	}

	@Override
	public int getViewTypeCount() {
		// always returns the same view
		return 1;
	}

	@Override
	public int getItemViewType(int arg0) {
		// always returns the same view
		return 0;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return list == null || list.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
		dataSetObservers.add(arg0);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		dataSetObservers.remove(arg0);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}

}
