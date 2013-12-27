package de.mpasch.p2p_sync.android.sync.couch2android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.MissingNode;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.text.TextUtils;
import android.util.Log;
import de.mpasch.p2p_sync.android.Constants;
import de.mpasch.p2p_sync.android.MyApplication;
import de.mpasch.p2p_sync.android.data.ContactConverter;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;

public class Couch2AndroidSyncer {
	private static final String TAG = "Couch2AndroidSyncer";

	private static final String SYNC_MARKER_KEY = "de.mpasch.p2p_sync.android.sync.marker";
	private static final boolean NOTIFY_AUTH_FAILURE = true;

	private final AccountManager accountManager;

	private final Context context;

	private final DatabaseDAO database;
	
	public Couch2AndroidSyncer(Context context, DatabaseDAO database) {
		this.context = context;
		this.accountManager = AccountManager.get(context);
		this.database = database;
	}

	public void sync(Account account, SyncResult syncResult) {
		final ContactManager contactManager = new ContactManager();

		try {
			// see if we already have a sync-state attached to this account. By
			// handing
			// This value to the server, we can just get the contacts that have
			// been updated on the server-side since our last sync-up
			long lastSyncMarker = getServerSyncMarker(account);
			Log.d(TAG, "lastSyncMarker: " + lastSyncMarker);
			// By default, contacts from a 3rd party provider are hidden in the
			// contacts
			// list. So let's set the flag that causes them to be visible, so
			// that users
			// can actually see these contacts.
			if (lastSyncMarker == 0) {
				setAccountContactsVisibility(context, account, true);
			}

			List<AndroidContactHeader> androidDirtyContacts;
			// Use the account manager to request the AuthToken we'll need
			// to talk to our sample server. If we don't have an AuthToken
			// yet, this could involve a round-trip to the server to request
			// and AuthToken.
			final String authtoken = accountManager.blockingGetAuthToken(
					account, Constants.AUTHTOKEN_TYPE, NOTIFY_AUTH_FAILURE);

			// 1) get Contacts that Android marked as "dirty" (changed or new)
			androidDirtyContacts = contactManager.getAndroidDirtyContacts(
					context, account);

			// 2) send dirtyContacts to CouchDB
			sendDirtyToCouch(account, context.getContentResolver(),
					contactManager, authtoken, database, androidDirtyContacts);

			// 3) get Contacts which changed in CouchDB
			final List<JsonNode> couchDirtyContacts = getCouchDirtyContacts(
					database, lastSyncMarker);

			// 4) get Contacts which changed in CouchDB
			final List<JsonNode> couchDeletedContacts = getCouchDeletedContacts(
					database, lastSyncMarker);

			// 5) get Current Couch UpdateSeqence: TODO: Race-Condition?
			long newSyncMarker = getCouchCurrentUpdateSeq(database);

			// 6) send Couch-Changes to Android-DB
			updateContactsInAndroid(context, account.name, couchDirtyContacts,
					couchDeletedContacts);

			// 7) set SyncMarker in Android-DB to the current CouchDB
			// "syncmarker"
			setServerSyncMarker(account, newSyncMarker);

		} catch (final AuthenticatorException e) {
			Log.e(TAG, "AuthenticatorException", e);
			syncResult.stats.numParseExceptions++;
		} catch (final OperationApplicationException e) {
			Log.e(TAG, "OperationApplicationException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		} catch (final AuthenticationException e) {
			Log.e(TAG, "AuthenticationException", e);
			syncResult.stats.numAuthExceptions++;
		} catch (final ParseException e) {
			Log.e(TAG, "ParseException", e);
			syncResult.stats.numParseExceptions++;
		} catch (final JSONException e) {
			Log.e(TAG, "JSONException", e);
			syncResult.stats.numParseExceptions++;
		} catch (final RemoteException e) {
			Log.e(TAG, "RemoteException:", e);
		} catch (final DataFormatException e) {
			Log.e(TAG, "DataFormatException: ", e);
		}
	}

	private void sendDirtyToCouch(Account account, ContentResolver resolver,
			ContactManager contactManager, String authtoken,
			DatabaseDAO database,
			List<AndroidContactHeader> androidDirtyContacts)
			throws DataFormatException, RemoteException,
			OperationApplicationException {

		final ContactMerger merger = new ContactMerger(resolver);

		for (final AndroidContactHeader androidContactHeader : androidDirtyContacts) {
			final String couchUUID = androidContactHeader.getServerContactId();
			final JsonNode contact = database.getContact(couchUUID);

			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			if (contact != null) {
				boolean changed = merger.merge(
						androidContactHeader.getRawContactId(), contact);
				if (changed) {
					database.update(contact);
				}
				ops.add(contactManager.removeDirtyFlag(resolver,
						androidContactHeader));
			} else {
				Log.d(TAG, "Creating contacts in Android no supported yet");
			}

			// Batch
			ContentProviderResult[] results = resolver.applyBatch(
					ContactsContract.AUTHORITY, ops);
			// results contains URIs of changed contacts..
		}

	}

	/**
	 * @param couchDeletedContacts
	 * @throws IOException
	 */
	private synchronized ContentProviderResult[] updateContactsInAndroid(
			Context context, String accountName,
			List<JsonNode> couchDirtyContacts,
			List<JsonNode> couchDeletedContacts) throws RemoteException,
			OperationApplicationException, IOException {
		final ContactConverter converter = new ContactConverter();
		final ContactManager contactManager = new ContactManager();

		final ContentResolver resolver = context.getContentResolver();

		Log.d(TAG, "In SyncContacts");
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (final JsonNode contact : couchDirtyContacts) {

			final String id = converter.getId(contact);
			if (contact instanceof MissingNode
					|| (contact.get("_deleted") != null && contact.get(
							"_deleted").asBoolean())) {

				Log.e(TAG,
						"ERROR: this shouldn't happen... -  Contact deleted in Couch: "
								+ id);
				// delete contact?
			} else {

				final Cursor cursor = contactManager.findContactById(
						accountName, resolver, id);

				if (cursor.getCount() < 1) {
					// no results => create new Contact
					contactManager.insertContact(accountName, ops, contact);
				} else {
					// there should be exactly one contact => Update
					cursor.moveToFirst();
					final String androidContactId = cursor.getString(0);
					contactManager.updateContact(accountName, ops, contact,
							androidContactId, resolver);
				}
				cursor.close();
			}
		}
		for (JsonNode deletedContact : couchDeletedContacts) {
			final String id = converter.getId(deletedContact);
			final Cursor cursor = contactManager.findContactById(accountName,
					resolver, id);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				final String androidContactId = cursor.getString(0);
				contactManager.deleteContact(accountName, ops, deletedContact,
						androidContactId, resolver);
			} else {
				Log.i(TAG, "A contact was deleted in couchdb which didn't exist in android yet: " + id);
			}
		}

		// Batch
		ContentProviderResult[] results = resolver.applyBatch(
				ContactsContract.AUTHORITY, ops);
		// results contains URIs of created contacts..

		Log.d(TAG, results.length + " contacts changes in Android-DB.");

		return results;
	}

	private long getCouchCurrentUpdateSeq(DatabaseDAO database) {
		return database.getUpdateSeq();
	}

	private List<JsonNode> getCouchDirtyContacts(DatabaseDAO database,
			long androidUpdateSeq) throws JSONException, ParseException,
			IOException, AuthenticationException {

		final List<JsonNode> couchDirtyList = database
				.getChangedContacts(androidUpdateSeq);

		Log.d(TAG, "There are " + couchDirtyList.size()
				+ " changed contacts in CouchDB since last sync.");
		return couchDirtyList;
	}

	private List<JsonNode> getCouchDeletedContacts(DatabaseDAO database,
			long androidUpdateSeq) throws JSONException, ParseException,
			IOException, AuthenticationException {
		Log.d(TAG, "syncContacts (with couchDB)");

		final List<JsonNode> couchDirtyList = database.getContactRepository()
				.getDeletedContacts(androidUpdateSeq);

		Log.d(TAG, "There are " + couchDirtyList.size()
				+ " deleted contacts in CouchDB since last sync.");
		return couchDirtyList;
	}

	/**
	 * (Copied from Android-SDK-Examples) When we first add a sync adapter to
	 * the system, the contacts from that sync adapter will be hidden unless
	 * they're merged/grouped with an existing contact. But typically we want to
	 * actually show those contacts, so we need to mess with the Settings table
	 * to get them to show up.
	 * 
	 * @param context
	 *            the Authenticator Activity context
	 * @param account
	 *            the Account who's visibility we're changing
	 * @param visible
	 *            true if we want the contacts visible, false for hidden
	 */
	private void setAccountContactsVisibility(Context context, Account account,
			boolean visible) {
		ContentValues values = new ContentValues();
		values.put(RawContacts.ACCOUNT_NAME, account.name);
		values.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

		context.getContentResolver().insert(Settings.CONTENT_URI, values);
	}

	/**
	 * (Copied from Android-SDK-Examples) This helper function fetches the last
	 * known high-water-mark we received from the server - or 0 if we've never
	 * synced.
	 * 
	 * @param account
	 *            the account we're syncing
	 * @return the change high-water-mark
	 */
	private long getServerSyncMarker(Account account) {
		String markerString = accountManager.getUserData(account,
				SYNC_MARKER_KEY);
		if (!TextUtils.isEmpty(markerString)) {
			return Long.parseLong(markerString);
		}
		return 0;
	}

	/**
	 * (Copied from Android-SDK-Examples) Save off the high-water-mark we
	 * receive back from the server.
	 * 
	 * @param account
	 *            The account we're syncing
	 * @param marker
	 *            The high-water-mark we want to save.
	 */
	private void setServerSyncMarker(Account account, long marker) {
		accountManager.setUserData(account, SYNC_MARKER_KEY,
				Long.toString(marker));
	}

}
