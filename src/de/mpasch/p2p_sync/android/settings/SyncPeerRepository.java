package de.mpasch.p2p_sync.android.settings;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;

@View( name = "all", map = "function(doc) { if (doc.type == 'sync-peer' ) emit( null, doc._id )}")
public class SyncPeerRepository extends CouchDbRepositorySupport<SyncPeer> {

	public SyncPeerRepository(CouchDbConnector db) {
		super(SyncPeer.class, db);
		initStandardDesignDocument();
	}

}
