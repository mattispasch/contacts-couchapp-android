package de.mpasch.p2p_sync.android.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import de.mpasch.p2p_sync.android.data.ContactConverter;

public class ContactManager {

	private static final String TAG = null;
	final ContactConverter converter = new ContactConverter();

	public void updateContact(String account,
			ArrayList<ContentProviderOperation> ops, JsonNode contact,
			String androidContactId, ContentResolver resolver)
			throws RemoteException {

		final List<ContentProviderOperation.Builder> builders = new ArrayList<ContentProviderOperation.Builder>();

		// Name
		updateName(builders, contact, androidContactId);

		// Phones
		updateListItem(builders, androidContactId,
				converter.getPhones(contact),
				ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
				resolver);

		// Emails
		updateListItem(builders, androidContactId,
				converter.getEmails(contact),
				ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				resolver);

		// PostalAddresses
		updateListItem(
				builders,
				androidContactId,
				converter.getPostalAddresses(contact),
				ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
				resolver);

		// Increase Performance: Yield Allowed after each contact (last Element)
		builders.get(builders.size() - 1).withYieldAllowed(true);
		for (final Builder b : builders) {
			ops.add(b.build());
		}

	}

	private void updateName(
			final List<ContentProviderOperation.Builder> builders,
			JsonNode contact, String androidContactId) {
		final Uri nameContentUri = ContactsContract.Data.CONTENT_URI
				.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
						"true").build();
		final ContentProviderOperation.Builder nameBuilder = ContentProviderOperation
				.newUpdate(nameContentUri);
		final ContentValues nameValues = converter.getName(contact);
		nameBuilder.withValues(nameValues);
		final String nameElementSelection = selectByItemType(
				androidContactId,
				ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		nameBuilder.withSelection(nameElementSelection, null);
		builders.add(nameBuilder);
	}

	private String selectByItemType(String androidContactId,
			String contentItemType) {
		final String nameElementSelection = ContactsContract.Data.RAW_CONTACT_ID
				+ " = '"
				+ androidContactId
				+ "' AND "
				+ ContactsContract.Data.MIMETYPE
				+ " = '"
				+ contentItemType
				+ "'";
		return nameElementSelection;
	}

	public void insertContact(String account,
			ArrayList<ContentProviderOperation> ops, final JsonNode contact) {
		// ops.size() will be the index of the header element
		final int headerBackRef = ops.size();

		final List<ContentProviderOperation.Builder> builders = new ArrayList<ContentProviderOperation.Builder>();

		final Uri contentUri = RawContacts.CONTENT_URI
				.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
						"true").build();

		// header
		final ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(contentUri);
		final ContentValues header = converter.convertToAndroidHeader(contact,
				account);
		builder.withValues(header);
		builders.add(builder);

		// Name
		final ContentProviderOperation.Builder nameBuilder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		final ContentValues nameValues = converter.getName(contact);
		nameBuilder.withValues(nameValues);
		nameBuilder.withValueBackReference(
				ContactsContract.Data.RAW_CONTACT_ID, headerBackRef);
		builders.add(nameBuilder);

		// Birthday
		final ContentValues bdayValues = converter.getBDay(contact);
		if (bdayValues != null) {
			final ContentProviderOperation.Builder bDayBuilder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			bDayBuilder.withValues(bdayValues);
			bDayBuilder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, headerBackRef);
			builders.add(bDayBuilder);
		}

		// Phones
		insertListItem(builders, headerBackRef, converter.getPhones(contact));

		// Emails
		insertListItem(builders, headerBackRef, converter.getEmails(contact));

		// PostalAddresses
		insertListItem(builders, headerBackRef,
				converter.getPostalAddresses(contact));

		// Increase Performance: Yield Allowed after each contact (last Element)
		builders.get(builders.size() - 1).withYieldAllowed(true);
		for (final Builder b : builders) {
			ops.add(b.build());
		}
	}

	private void insertListItem(
			List<ContentProviderOperation.Builder> builders,
			final int headerBackRef, final List<ContentValues> valuesList) {
		for (ContentValues values : valuesList) {
			ContentProviderOperation.Builder builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValues(values);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, headerBackRef);
			builders.add(builder);
		}
	}

	private void updateListItem(
			List<ContentProviderOperation.Builder> builders,
			final String androidContactId,
			final List<ContentValues> valuesList, final String contentItemType,
			ContentResolver resolver) throws RemoteException {
		final String selection = selectByItemType(androidContactId,
				contentItemType);

		// TODO: is DATA1 ok? Since DATA1 contains indexed Data, it should be
		// the right column to distinguish by
		// TODO: find a clean solution to update List Items
		final String[] projection = { ContactsContract.Data._ID,
				ContactsContract.Data.DATA1 };
		final Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				projection, selection, null, null);

		/** Maps DATA1 => _ID */
		final Map<String, Integer> itemsInAndroid = new HashMap<String, Integer>();
		if (cursor != null && cursor.moveToFirst()) {
			do {
				itemsInAndroid.put(cursor.getString(1), cursor.getInt(0));
			} while (cursor.moveToNext());
		}

		// Iterate through values in CouchDB and create and modify those which
		// are different in Android
		for (ContentValues values : valuesList) {
			final String indexColumnValue = values
					.getAsString(ContactsContract.Data.DATA1);

			final Integer androidIndexColumnValue = itemsInAndroid
					.get(indexColumnValue);

			if (androidIndexColumnValue == null) {

				final ContentProviderOperation.Builder builder = ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI);
				builder.withValues(values);
				builder.withValue(ContactsContract.Data.RAW_CONTACT_ID,
						androidContactId);
				builders.add(builder);
			} else {
				// TODO: Updates even if not necessary (just useless, but no
				// problem..)
				final ContentProviderOperation.Builder builder = ContentProviderOperation
						.newUpdate(ContactsContract.Data.CONTENT_URI);
				builder.withValues(values);

				final String idSelection = ContactsContract.Data._ID + " = "
						+ androidIndexColumnValue;
				builder.withSelection(idSelection, null);

				builders.add(builder);
			}
			// done
			itemsInAndroid.remove(indexColumnValue);
		}

		// Iterate through itemsInAndroid which haven't been matched to a
		// corresponding Row in CouchDB
		for (final Integer androidRowId : itemsInAndroid.values()) {
			final ContentProviderOperation.Builder builder = ContentProviderOperation
					.newDelete(ContactsContract.Data.CONTENT_URI);

			final String idSelection = ContactsContract.Data._ID + " = ?";
			builder.withSelection(idSelection,
					new String[] { "" + androidRowId });

			builders.add(builder);
		}
	}

	public Cursor findContactById(String accountName,
			final ContentResolver resolver, final String id)
			throws IOException, OperationApplicationException {
		final String[] projection = { RawContacts._ID, RawContacts.SOURCE_ID };
		// use selectionArgs for security reasons (no SQL Injection
		// possible)
		final String selection = RawContacts.SOURCE_ID + " = ? AND "
				+ RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE
				+ "' AND " + RawContacts.ACCOUNT_NAME + "= ?";
		final String[] selectionArgs = { id, accountName };
		final String sortOrder = null;
		final Cursor cursor = resolver.query(RawContacts.CONTENT_URI,
				projection, selection, selectionArgs, sortOrder);

		if (cursor == null) {
			throw new IOException(
					"Querying ContentResolver failed: cursor == null");
		} else if (cursor.getCount() > 1) {
			cursor.close();
			throw new OperationApplicationException(
					"ERROR: There are two contacts in Android-DB with CouchDB-ID "
							+ id);
		}
		return cursor;
	}

	public List<AndroidContactHeader> getAndroidDirtyContacts(Context context,
			Account account) {
		Log.i(TAG, "*** Looking for local dirty contacts");
		List<AndroidContactHeader> dirtyContacts = new ArrayList<AndroidContactHeader>();

		final ContentResolver resolver = context.getContentResolver();
		final Cursor c = resolver.query(DirtyQuery.CONTENT_URI,
				DirtyQuery.PROJECTION, DirtyQuery.SELECTION,
				new String[] { account.name }, null);
		try {
			while (c.moveToNext()) {
				final long rawContactId = c
						.getLong(DirtyQuery.COLUMN_RAW_CONTACT_ID);
				final String serverContactId = c
						.getString(DirtyQuery.COLUMN_SERVER_ID);
				final boolean isDirty = "1".equals(c
						.getString(DirtyQuery.COLUMN_DIRTY));
				final boolean isDeleted = "1".equals(c
						.getString(DirtyQuery.COLUMN_DELETED));

				// The system actually keeps track of a change version number
				// for
				// each contact. It may be something you're interested in for
				// your
				// client-server sync protocol. We're not using it in this
				// example,
				// other than to log it.
				final long version = c.getLong(DirtyQuery.COLUMN_VERSION);

				Log.i(TAG, "Dirty Contact: " + Long.toString(rawContactId));
				Log.i(TAG, "Contact Version: " + Long.toString(version));

				final AndroidContactHeader contact = new AndroidContactHeader(
						rawContactId, serverContactId, isDirty, isDeleted);
				dirtyContacts.add(contact);

			}

		} finally {
			if (c != null) {
				c.close();
			}
		}
		return dirtyContacts;
	}

	/**
	 * Constants for a query to find SampleSyncAdapter contacts that are in need
	 * of syncing to the server. This should cover new, edited, and deleted
	 * contacts.
	 */
	final private static class DirtyQuery {

		private DirtyQuery() {
		}

		public final static String[] PROJECTION = new String[] {
				RawContacts._ID, RawContacts.SOURCE_ID, RawContacts.DIRTY,
				RawContacts.DELETED, RawContacts.VERSION };

		public final static int COLUMN_RAW_CONTACT_ID = 0;
		public final static int COLUMN_SERVER_ID = 1;
		public final static int COLUMN_DIRTY = 2;
		public final static int COLUMN_DELETED = 3;
		public final static int COLUMN_VERSION = 4;

		public static final Uri CONTENT_URI = RawContacts.CONTENT_URI
				.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
						"true").build();

		public static final String SELECTION = RawContacts.DIRTY + "=1 AND "
				+ RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE
				+ "' AND " + RawContacts.ACCOUNT_NAME + "=?";
	}

	public ContentProviderOperation removeDirtyFlag(ContentResolver resolver,
			AndroidContactHeader androidContactHeader) {

		final Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				androidContactHeader.getRawContactId());

		ContentValues values = new ContentValues();
		values.put(RawContacts.DIRTY, 0);

		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newUpdate(uri);
		builder.withValues(values);
		
		return builder.build();
	}
}
