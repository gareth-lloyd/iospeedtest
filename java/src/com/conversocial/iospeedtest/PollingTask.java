package com.conversocial.iospeedtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import javax.management.RuntimeErrorException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;


public class PollingTask {
	private String sourceId;
	private GraphClient graphClient;
	private Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();
	
	public PollingTask(String sourceId, GraphClient graphClient) {
		this.sourceId = sourceId;
		this.graphClient = graphClient;
	}
	
	public void performTask() {
		List<String> paths = Arrays.asList(sourceId, "posts");
		this.graphClient.get(paths, EMPTY_PARAMS, new PostsCallback());
	}
	
	private class PostsCallback implements FutureCallback<HttpResponse> {
		@Override
		public void cancelled() {
			System.out.println("callback cancelled");
		}

		public JSONObject getJsonResponse(HttpResponse response) {
			HttpEntity body = response.getEntity();
			boolean gzipResponse = false;
			for (Header header : response.getHeaders("Content-encoding")) {
				if (header.getValue().equals("gzip")) {
					gzipResponse = true;
				}
			}
			Reader reader = null;
			try {
				InputStream stream = body.getContent();
				if (gzipResponse) {
					stream = new GZIPInputStream(stream);
				}
				reader = new InputStreamReader(stream);
			} catch (IllegalStateException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			
			JSONObject responseObject = (JSONObject) JSONValue.parse(reader);
			
			if (responseObject.containsKey("error")) {
				throw new RuntimeException("Facebook error");
			}
			
			return responseObject;
		}
		
		@Override
		public void completed(HttpResponse response) {
			JSONObject responseObject = getJsonResponse(response);
			JSONArray postsJson = (JSONArray) responseObject.get("data");
			
			for (Object postJson : postsJson) {
				JSONObject postObject = (JSONObject) postJson;
				String uid = (String) postObject.get("id");
				String message = (String) postObject.get("message");
				Post post = new Post(sourceId, uid, message);
				
				DataStore.getInstance().savePost(post);
				
				List<String> paths = Arrays.asList(uid, "comments");
				graphClient.get(paths, EMPTY_PARAMS, new CommentsCallback(post));
			}
			
		}

		@Override
		public void failed(Exception ex) {
			System.out.println("callback failed");
		}
	}

	private class CommentsCallback extends PostsCallback {
		private Post post;
		
		public CommentsCallback(Post post) {
			this.post = post;
		}
		
		@Override
		public void completed(HttpResponse response) {
			JSONObject responseObject = getJsonResponse(response);
			JSONArray commentsJson = (JSONArray) responseObject.get("data");
			
			for (Object commentJson : commentsJson) {
				JSONObject commentObject = (JSONObject) commentJson;
				String uid = (String) commentObject.get("id");
				String message = (String) commentObject.get("message");

				Comment comment = new Comment(this.post, uid, message);
				DataStore.getInstance().saveComment(comment);
				
			}
		}
	}
}
