package de.mpasch.p2p_sync.android.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbInfo;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.ektorp.android.http.AndroidHttpClient;
import org.ektorp.android.http.AndroidHttpClient.Builder;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.DocumentChange;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.content.Context;
import android.util.Log;

import com.couchbase.cblite.CBLDatabase;
import com.couchbase.cblite.CBLFilterBlock;
import com.couchbase.cblite.CBLRevision;
import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.CBLView;
import com.couchbase.cblite.ektorp.CBLiteHttpClient;
import com.couchbase.cblite.javascript.CBLJavaScriptViewCompiler;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;

import de.mpasch.p2p_sync.android.Callback;
import de.mpasch.p2p_sync.android.httpservice.Filters;
import de.mpasch.p2p_sync.android.settings.SyncPeerRepository;

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

	private ContactRepository contactRepo;

	private SyncPeerRepository syncPeerRepo;

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

		CBLView.setCompiler(new CBLJavaScriptViewCompiler());

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

	/**
	 * 
	 * @param remoteDatabaseUrl
	 * @return true, if DB changed locally, false, if not changed
	 * @throws ReplicationException
	 *             if there was an error
	 */
	public boolean replicateContacts(String remoteDatabaseUrl)
			throws ReplicationException {

		// TODO: filter pull replication (filter has to exist on remote
		// Database..)

		final ReplicationCommand pull = new ReplicationCommand.Builder()
				.source(remoteDatabaseUrl).target(DATABASE_NAME)
				// .filter(Filters.FILTER_CONTACTS_INCL_DELETED)
				.continuous(false).build();
		final ReplicationCommand push = new ReplicationCommand.Builder()
				.source(DATABASE_NAME).target(remoteDatabaseUrl)
				.filter(Filters.FILTER_CONTACTS_INCL_DELETED).continuous(false)
				.build();

		ReplicationStatus pullStatus = dbInstance.replicate(pull);
		if (!pullStatus.isOk()) {
			throw new ReplicationException("pull was not successful!");
		}

		ReplicationStatus pushStatus = dbInstance.replicate(push);
		if (!pushStatus.isOk()) {
			throw new ReplicationException("push was not successful!");
		}
		return !pullStatus.isNoChanges();
	}

	public void start(Context context) {
		Log.v(TAG, "Starting Database...");
		String filesDir = context.getFilesDir().getAbsolutePath();
		try {
			server = new CBLServer(filesDir);

			startEktorp();
			Log.v(TAG, "Database started.");
		} catch (IOException e) {
			Log.e(TAG, "Error starting TDServer", e);
		}
		new Filters().installContactFilters(server);
	}

	// NOT WORKING!
	private void startNetwork() {
		Log.v(TAG, "starting ektorp (network)");

		new Thread(new Runnable() {

			@Override
			public void run() {

				if (httpClient != null) {
					httpClient.shutdown();
				}

				// HttpClient httpClient = new StdHttpClient.Builder().build();

				final Integer port = 5984;
				final String hostName = "localhost";
				final Builder builder = new AndroidHttpClient.Builder()
						.host(hostName).port(port).useExpectContinue(false);
				httpClient = builder.build();

				// new
				// AndroidHttpClient.Builder().url("http://localhost:5984").build();

				// httpClient = new CBLiteHttpClient(server);
				dbInstance = new StdCouchDbInstance(httpClient);

				CBLView.setCompiler(new CBLJavaScriptViewCompiler());
				couchDbConnector = dbInstance.createConnector(DATABASE_NAME,
						true);
				Log.d(TAG, "Ektorp connected via network.");
				listener.onConnected();
			}

		}).start();

	}

	public ContactRepository getContactRepository() {
		if (contactRepo == null) {
			contactRepo = new ContactRepository(couchDbConnector);
		}
		return contactRepo;
	}

	public SyncPeerRepository getSyncPeerRepository() {
		if (syncPeerRepo == null) {
			syncPeerRepo = new SyncPeerRepository(couchDbConnector);
		}
		return syncPeerRepo;
	}

	public JsonNode getContact(String id) {
		// return couchDbConnector.get(JsonNode.class, id);
		return getContactRepository().get(id);
	}

	public long getUpdateSeq() {
		DbInfo dbInfo = couchDbConnector.getDbInfo();
		return dbInfo.getUpdateSeq();
	}

	public List<JsonNode> getChangedContacts(long updateSequence) {
		return getContactRepository().getChangedContacts(updateSequence);
	}

	public List<JsonNode> getDeletedContacts(long updateSequence) {
		return getContactRepository().getDeletedContacts(updateSequence);
	}

	public void update(JsonNode updatedContact) {
		couchDbConnector.update(updatedContact);
	}

	public CouchDbConnector getConnector() {
		return couchDbConnector;
	}

	public void close() {
		if (httpClient != null) {
			httpClient.shutdown();
		}
		if (server != null) {
			server.close();
		}
	}

}
