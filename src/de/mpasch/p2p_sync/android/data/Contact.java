package de.mpasch.p2p_sync.android.data;

import java.util.List;
import java.util.Map;

import org.ektorp.support.CouchDbDocument;
import org.ektorp.support.TypeDiscriminator;

public class Contact extends CouchDbDocument{

	public class Email {
		private String address;
		private String primary;
		
		public String getAddress() {
			return address;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		public String getPrimary() {
			return primary;
		}
		public void setPrimary(String primary) {
			this.primary = primary;
		}
	}

	private static final long serialVersionUID = 1L;

	@TypeDiscriminator(value = "contact")
	private String type;
	
	private String name;
	private String content;
	private String birthday;
	
	private Map google;
	
	private List<Email> emails;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

	public List<Email> getEmails() {
		return emails;
	}

	public void setEmails(List<Email> emails) {
		this.emails = emails;
	}

	public Map getGoogle() {
		return google;
	}

	public void setGoogle(Map google) {
		this.google = google;
	}
	
}
