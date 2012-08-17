package com.conversocial.iospeedtest;

public class Comment {

	private Post post;
	private String uid;
	private String message;

	public Comment(Post post, String uid, String message) {
		this.setPost(post);
		this.setUid(uid);
		this.setMessage(message);
	}
	
	public void setPost(Post post) {
		this.post = post;
	}

	public Post getPost() {
		return post;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
