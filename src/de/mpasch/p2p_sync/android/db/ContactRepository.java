package de.mpasch.p2p_sync.android.db;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.DocumentChange;
import org.ektorp.support.CouchDbRepositorySupport;


public class ContactRepository extends CouchDbRepositorySupport<JsonNode>{

	public ContactRepository(CouchDbConnector db) {
		super(JsonNode.class, db);
		initStandardDesignDocument();
		
	}
	
	
	public List<JsonNode> getChangedContacts(long updateSequence) {
		final List<JsonNode> changedContacts = new ArrayList<JsonNode>();

		final ChangesCommand cmd = new ChangesCommand.Builder()
				.since(updateSequence).includeDocs(true)
				.filter("contacts")
				.build();

		final List<DocumentChange> changes = this.db.changes(cmd);
		for (DocumentChange documentChange : changes) {
			changedContacts.add(documentChange.getDocAsNode());
		}
		return changedContacts;
	}

	public List<JsonNode> getDeletedContacts(long updateSequence) {
		final List<JsonNode> deletedContacts = new ArrayList<JsonNode>();

		final ChangesCommand cmd = new ChangesCommand.Builder()
				.since(updateSequence).includeDocs(true)
				.filter("deletedContacts")
				.build();

		final List<DocumentChange> changes = this.db.changes(cmd);
		for (DocumentChange documentChange : changes) {
			deletedContacts.add(documentChange.getDocAsNode());
		}
		return deletedContacts;
	}


}
