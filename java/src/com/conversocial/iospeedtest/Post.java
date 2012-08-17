package com.conversocial.iospeedtest;

public class Post {

	private String sourceId;
	private String uid;
	private String message;

	public Post(String sourceId, String uid, String message) {
		this.setSourceId(sourceId);
		this.setUid(uid);
		this.setMessage(message);
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
