package de.mpasch.p2p_sync.android.sync.couch2android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;

public class ContactMerger {

	private final ContentResolver resolver;

	public ContactMerger(final ContentResolver resolver) {
		this.resolver = resolver;
	}

	public boolean merge(long rawContactId, JsonNode couchContact) throws DataFormatException {
		final ObjectNode contact = (ObjectNode) couchContact;

		boolean changed = false;

		changed = changed || mergeEmails(rawContactId, contact);

		return changed;
	}

	public boolean mergeEmails(long rawContactId, ObjectNode contact) throws DataFormatException {
		boolean changed = false;
		
		final String[] projection = { Email.ADDRESS, Email.IS_PRIMARY };
		final List<Map<String, String>> rows = getDataRows(rawContactId,
				Email.CONTENT_ITEM_TYPE, projection);
		
		
		final ArrayNode list;

		final String listName = "emails";
		if (contact.get(listName) == null) {
			list = contact.putObject(listName).arrayNode();
		} else {
			if(contact.get(listName) instanceof ArrayNode) {
				list = (ArrayNode) contact.get(listName);
			} else {
				throw new DataFormatException("Contact contains an emails field which is not an array!");
			}
		}

		// index all addresses
		final Map<String, Integer> keyToIndex = new HashMap<String, Integer>();
		for (int i = 0; i < list.size(); i++) {
			final String key = list.get(i).get("address").asText();
			keyToIndex.put(key, i);
		}

		// Iterate through android-Emails
		for (Map<String, String> row : rows) {
			final String address = row.get(Email.ADDRESS);

			final Integer index = keyToIndex.get(address);
			final ObjectNode entry;
			if (index != null) {
				// update
				entry = (ObjectNode) list.get(index);
			} else {
				// insert
				entry = list.addObject();
				changed = true;
			}
			
			changed = changed || updateEntry(entry, "address", address);
			final String primary = row.get(Email.IS_PRIMARY);
			if (primary != null && !primary.equals("0")) {
				changed = changed || updateEntry(entry, "primary", "true");
			} else {
				if (entry.get(primary) != null) {
					entry.remove("primary");
					changed = true;
				}
			}
//			if (row.get(Email.DELETED) == "true") {
//				entry.put("deleted", "true");
//			}

		}
		return changed;
	}

	private boolean updateEntry( final ObjectNode entry, final String key, final String value) {
		if(entry.get(key) == null || entry.get(key).toString().equals(value)) {
			entry.put(key, value);
			return true;
		}
		return false;
	}

	private List<Map<String, String>> getDataRows(final long rawContactId,
			String contentItemType, final String[] projection) {
		final List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

		final String selection = ContactsContract.Data.RAW_CONTACT_ID
				+ " = ? and " + ContactsContract.Data.MIMETYPE
				+ " = ?";
		final String[] args = { "" + rawContactId, contentItemType };
		final Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				projection, selection, args, null);

		if (cursor == null || cursor.getCount() < 1) {
			if(cursor != null) {
				cursor.close();
			}
			return rows;
		}
		cursor.moveToFirst();
		do {
			final Map<String, String> row = new HashMap<String, String>();
			for (int i = 0; i < projection.length; i++) {
				final String colName = projection[i];
				row.put(colName, cursor.getString(i));
			}
			rows.add(row);
		} while (cursor.moveToNext());
		cursor.close();
		return rows;
	}

}
