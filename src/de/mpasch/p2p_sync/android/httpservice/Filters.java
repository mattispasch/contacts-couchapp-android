package de.mpasch.p2p_sync.android.httpservice;

import com.couchbase.cblite.CBLDatabase;
import com.couchbase.cblite.CBLFilterBlock;
import com.couchbase.cblite.CBLRevision;
import com.couchbase.cblite.CBLServer;

public class Filters {
	public static final String FILTER_CONTACTS = "contacts";
	public static final String FILTER_CONTACTS_INCL_DELETED = "contactsWithDeleted";
	public static final String FILTER_DELETED_CONTACTS = "deletedContacts";
	
	public void installContactFilters(CBLServer server) {
		CBLDatabase contactDB = server.getDatabaseNamed(FILTER_CONTACTS);
		contactDB.defineFilter(FILTER_CONTACTS, new CBLFilterBlock() {

			@Override
			public boolean filter(CBLRevision revision) {
				String type = (String) revision.getProperties().get("type");
				return (type != null && type.equals("contact"));
			}
		});
		contactDB.defineFilter(FILTER_DELETED_CONTACTS, new CBLFilterBlock() {

			@Override
			public boolean filter(CBLRevision revision) {
				String type = (String) revision.getProperties().get("type");
				return (type != null && type.equals("contact-deleted"));
			}
		});
		contactDB.defineFilter(FILTER_CONTACTS_INCL_DELETED,
				new CBLFilterBlock() {

					@Override
					public boolean filter(CBLRevision revision) {
						String type = (String) revision.getProperties().get(
								"type");
						if (type == null) {
							return false;
						}
						return (type.equals("contact") && type
								.equals("contact-deleted"));
					}
				});
	}
}
