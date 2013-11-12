package de.mpasch.p2p_sync.android.data;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.GenerateView;
import org.ektorp.support.View;

@View(name="showAll", map="function(doc) { if('contact'===doc.type) { emit(doc.name, doc); } }")
public class ContactRepository extends CouchDbRepositorySupport<Contact> {

	public ContactRepository(CouchDbConnector db) {
		super(Contact.class, db);
		initStandardDesignDocument();		
	}
	
	@GenerateView
	public List<Contact> getAll() {
//		return queryView("showAll");
		ViewQuery q = createQuery("showAll").descending(true);
		return db.queryView(q, Contact.class);
	}
	
	
	
}
