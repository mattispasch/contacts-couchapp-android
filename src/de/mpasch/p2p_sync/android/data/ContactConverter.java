package de.mpasch.p2p_sync.android.data;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import de.mpasch.p2p_sync.android.Constants;

public class ContactConverter {

	private static final String TAG = "ContactConverter";

	public ContentValues convertToAndroidHeader(JsonNode json,
			String accountName) {

		final ContentValues header = new ContentValues();

		header.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		header.put(RawContacts.ACCOUNT_NAME, accountName);
		//
		
		String contentItemType = RawContacts.CONTENT_ITEM_TYPE;
		header.put(RawContacts.SOURCE_ID, getId(json));

		
		return header;
	}

	public ContentValues getName(JsonNode json) {
		final String name = json.get("name") == null ? null : json.get("name")
				.asText(); // not required
		final ContentValues values = new ContentValues();

		values.put(
				ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		values.put(
				ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
				name);
		return values;

	}

	public List<ContentValues> getEmails(JsonNode contact) {
		final JsonNode list = contact.get("emails");

		if (list == null) {
			return new ArrayList<ContentValues>();
		}

		final List<ContentValues> values = new ArrayList<ContentValues>();
		for (int i = 0; i < list.size(); i++) {
			final ContentValues cv = new ContentValues();
			final JsonNode item = list.get(i);
			final String address = item.get("address") == null ? null : item
					.get("address").asText();
			final boolean primary = item.get("primary") == null ? false : item
					.get("primary").asBoolean();
			cv.put(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
			cv.put(ContactsContract.CommonDataKinds.Email.ADDRESS, address);
			if (primary) {
				cv.put(ContactsContract.CommonDataKinds.Email.IS_PRIMARY, 1);
			}
			values.add(cv);
		}
		return values;
	}

	public List<ContentValues> getPhones(JsonNode contact) {
		final JsonNode phones = contact.get("phones");

		if (phones == null) {
			return new ArrayList<ContentValues>();
		}

		final List<ContentValues> values = new ArrayList<ContentValues>();
		for (int i = 0; i < phones.size(); i++) {
			final ContentValues cv = new ContentValues();
			final JsonNode phone = phones.get(i);
			final String number = phone.get("number") == null ? null : phone
					.get("number").asText();
			final String label = phone.get("label") == null ? null : phone.get(
					"label").asText();
			cv.put(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
			cv.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number);
			cv.put(ContactsContract.CommonDataKinds.Phone.TYPE,
					getPhoneType(label));
			values.add(cv);
		}
		return values;
	}

	private Integer getPhoneType(String label) {
		if ("Mobil".equals(label)) {
			return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
		} else if ("Privat".equals(label)) {
			return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
		} else if ("Arbeit".equals(label)) {
			return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
		} else if ("Sonstige".equals(label)) {
			return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
		}
		Log.i(TAG, "Phone Label not mapped to any Type: " + label);
		return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
	}

	public String getId(JsonNode json) {
		return json.get("_id").asText();
	}

	private String getRev(JsonNode json) {
		return json.get("_rev").asText();
	}

	public List<ContentValues> getPostalAddresses(JsonNode contact) {
		final JsonNode list = contact.get("postalAddresses");

		if (list == null) {
			return new ArrayList<ContentValues>();
		}

		final List<ContentValues> values = new ArrayList<ContentValues>();
		for (int i = 0; i < list.size(); i++) {
			final ContentValues cv = new ContentValues();
			final JsonNode item = list.get(i);
			final String address = item.get("address") == null ? null : item
					.get("address").asText();
			cv.put(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
			cv.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address);
			values.add(cv);
		}
		return values;
	}

	public ContentValues getBDay(JsonNode contact) {
		final JsonNode bday = contact.get("birthday");
		if(bday == null) {
			return null;
		}
		final String date = bday.asText();
		
		final ContentValues values = new ContentValues();
		values.put(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Event.MIMETYPE);
		values.put(ContactsContract.CommonDataKinds.Event.START_DATE, date);
		values.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
		
		return values;
	}

}
