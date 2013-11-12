package de.mpasch.p2p_sync.android.sync;

public class AndroidContactHeader {

	private final long rawContactId;
	private final String serverContactId;
	private final boolean isDirty;
	private final boolean isDeleted;

	public AndroidContactHeader(long rawContactId, String serverContactId,
			boolean isDirty, boolean isDeleted) {
		this.rawContactId = rawContactId;
		this.serverContactId = serverContactId;
		this.isDeleted = isDeleted;
		this.isDirty = isDirty;
	}

	public long getRawContactId() {
		return rawContactId;
	}

	public String getServerContactId() {
		return serverContactId;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

}
