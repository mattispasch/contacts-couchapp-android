package de.mpasch.p2p_sync.android.settings;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;

public class SyncPeer {
	
	public static final String TYPE = "sync-peer";

	@TypeDiscriminator
	@JsonProperty("_id")
	private String id;

	@JsonProperty("_rev")
	private String rev;

	@JsonProperty
	private String type = TYPE;

	@JsonProperty
	private String remoteUrl;

	@JsonProperty
	private boolean replicate = false;

	@JsonProperty
	private String name;

	@JsonProperty
	private String lastSuccessfulSync;

	public String getLastSuccessfulSync() {
		return lastSuccessfulSync;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLastSuccessfulSync(String lastSuccessfulSync) {
		this.lastSuccessfulSync = lastSuccessfulSync;		
	}

}
