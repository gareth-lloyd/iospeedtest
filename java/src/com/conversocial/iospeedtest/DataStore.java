package com.conversocial.iospeedtest;

import java.net.UnknownHostException;

import sun.security.jca.GetInstance;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class DataStore {
	private static DataStore instance = new DataStore();
	
	private String DATASTORE_NAME = "iospeedtestjava";
	private Mongo m;
	private DB db;
	
	private DBCollection posts;
	private DBCollection comments;
	
	private DataStore() {
		try {
			m = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	
		setUpDb();
	}
	
	public static synchronized DataStore getInstance() {
		return instance;
	}
	
	private void setUpDb() {
		m.dropDatabase(DATASTORE_NAME);
		db = m.getDB(DATASTORE_NAME);
		posts = db.getCollection("posts");
		comments = db.getCollection("comments");
	}

	public void clear() {
		m.dropDatabase(DATASTORE_NAME);
	}
	
	public void saveComment(Comment comment) {
		BasicDBObject doc = new BasicDBObject();
		doc.put("uid", comment.getUid());
		doc.put("message", comment.getMessage());
		doc.put("post", comment.getPost().getUid());
		
		comments.insert(doc);
		System.out.println("saved comment");
	}
	
	public void savePost(Post post) {
		BasicDBObject doc = new BasicDBObject();
		doc.put("uid", post.getUid());
		doc.put("message", post.getMessage());
		doc.put("sourceId", post.getSourceId());
		
		posts.insert(doc);
		System.out.println("saved post");
	}
}
