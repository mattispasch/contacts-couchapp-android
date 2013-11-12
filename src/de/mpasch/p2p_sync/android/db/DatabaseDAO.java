package de.mpasch.p2p_sync.android.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbInfo;
import org.ektorp.ReplicationCommand;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.DocumentChange;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.content.Context;
import android.util.Log;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.ektorp.CBLiteHttpClient;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

public class DatabaseDAO {

	private static final String TAG = "DatabaseDAO";

	private static final String DATABASE_NAME = "contacts";
	private static final String remoteDatabaseUrl = "http://192.168.178.29:5984/contacts";

	// couch internals
	protected static CBLServer server;
	protected static HttpClient httpClient;

	// ektorp impl
	protected CouchDbInstance dbInstance;
	protected CouchDbConnector couchDbConnector;
	protected ReplicationCommand pushReplicationCommand;
	protected ReplicationCommand pullReplicationCommand;

	// static inializer to ensure that touchdb:// URLs are handled properly
	{
		CBLURLStreamHandlerFactory.registerSelfIgnoreError();
	}

	private final DBListener listener;

	private final boolean async;

	/**
	 * No Async excecution
	 */
	public DatabaseDAO() {
		this(null, false);
	}

	/**
	 * Async
	 * 
	 * @param listener
	 */
	public DatabaseDAO(DBListener listener) {
		this(listener, true);
	}

	/**
	 * if async is false DBListener is ignored.
	 * 
	 * @param listener
	 * @param async
	 */
	public DatabaseDAO(DBListener listener, boolean async) {
		this.async = async;
		if (!async) {
			this.listener = null;
		} else {
			this.listener = listener;
		}
	}

	private void startEktorp() {
		Log.v(TAG, "starting ektorp");

		if (httpClient != null) {
			httpClient.shutdown();
		}

		httpClient = new CBLiteHttpClient(server);
		dbInstance = new StdCouchDbInstance(httpClient);

		if (async) {
			SyncEktorpAsyncTask startupTask = new SyncEktorpAsyncTask() {

				@Override
				protected void doInBackground() {
					couchDbConnector = dbInstance.createConnector(
							DATABASE_NAME, true);
					Log.d(TAG, "Ektorp connected.");
				}

				@Override
				protected void onSuccess() {
					listener.onConnected();
				}
			};
			startupTask.execute();
		} else {
			couchDbConnector = dbInstance.createConnector(DATABASE_NAME, true);
			Log.d(TAG, "Ektorp connected.");
		}
	}

	public void startPush() {
		// SharedPreferences prefs =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		pushReplicationCommand = new ReplicationCommand.Builder()
				.source(DATABASE_NAME).target(remoteDatabaseUrl)
				.continuous(false).build();
		if (async) {
			SyncEktorpAsyncTask pushReplication = new SyncEktorpAsyncTask() {

				@Override
				protected void doInBackground() {
					dbInstance.replicate(pushReplicationCommand);
				}
			};

			pushReplication.execute();
		} else {
			dbInstance.replicate(pushReplicationCommand);
		}
	}

	public void startPull() {
		pullReplicationCommand = new ReplicationCommand.Builder()
				.source(remoteDatabaseUrl).target(DATABASE_NAME)
				.continuous(false).build();
		if (async) {
			SyncEktorpAsyncTask pullReplication = new SyncEktorpAsyncTask() {

				@Override
				protected void doInBackground() {
					dbInstance.replicate(pullReplicationCommand);
				}
			};

			pullReplication.execute();
		} else {
			dbInstance.replicate(pullReplicationCommand);
		}
	}

	public void start(Context context) {
		String filesDir = context.getFilesDir().getAbsolutePath();
		try {
			server = new CBLServer(filesDir);
			server.getDatabaseNamed("contacts");
			startEktorp();
		} catch (IOException e) {
			Log.e(TAG, "Error starting TDServer", e);
		}
	}

	public List<JsonNode> getContacts() {
		final List<String> allDocIds = couchDbConnector.getAllDocIds();
		final List<JsonNode> all = new ArrayList<JsonNode>();

		for (String id : allDocIds) {
			all.add(couchDbConnector.get(JsonNode.class, id));
		}

		return all;
	}

	public List<JsonNode> getContacts(int limit) {
		final List<String> allDocIds = couchDbConnector.getAllDocIds();
		final List<JsonNode> all = new ArrayList<JsonNode>();

		int count = 0;
		for (String id : allDocIds) {
			all.add(couchDbConnector.get(JsonNode.class, id));
			count++;
			if (count >= limit) {
				break;
			}
		}

		return all;
	}

	public JsonNode getContact(String id) {
		return couchDbConnector.get(JsonNode.class, id);

	}

	public long getUpdateSeq() {
		DbInfo dbInfo = couchDbConnector.getDbInfo();
		return dbInfo.getUpdateSeq();
	}

	public List<JsonNode> getChangedContacts(long updateSequence) {
		final List<JsonNode> changedContacts = new ArrayList<JsonNode>();

		final ChangesCommand cmd = new ChangesCommand.Builder().since(
				updateSequence).includeDocs(true).build();

		final List<DocumentChange> changes = couchDbConnector.changes(cmd);
		for (DocumentChange documentChange : changes) {
			changedContacts.add(documentChange.getDocAsNode());
		}
		return changedContacts;
	}

	public void update(JsonNode updatedContact) {
		couchDbConnector.update(updatedContact);
	}

}
