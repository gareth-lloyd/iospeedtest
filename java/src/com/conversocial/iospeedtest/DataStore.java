package com.conversocial.iospeedtest;

import java.net.UnknownHostException;

import sun.security.jca.GetInstance;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

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
			m.setWriteConcern(WriteConcern.SAFE);
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
		if (true) return;
		BasicDBObject doc = new BasicDBObject();
		doc.put("uid", comment.getUid());
		doc.put("message", comment.getMessage());
		doc.put("post", comment.getPost().getUid());
		
		comments.insert(doc);
	}
	
	public void savePost(Post post) {
		if (true) return;
		BasicDBObject doc = new BasicDBObject();
		doc.put("uid", post.getUid());
		doc.put("message", post.getMessage());
		doc.put("sourceId", post.getSourceId());
		
		posts.insert(doc);
	}

	public void printStats() {
		System.out.println();
		System.out.println("posts saved: " + Long.toString(posts.count()));
		System.out.println("comments saved: " + Long.toString(comments.count()));
	}
}
