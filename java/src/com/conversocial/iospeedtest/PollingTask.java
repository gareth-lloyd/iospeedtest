package com.conversocial.iospeedtest;

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

import javax.management.RuntimeErrorException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PollingTask {
	private String sourceId;
	private GraphClient graphClient;
	private JSONParser jsonParser = new JSONParser();
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
			Reader reader = null;
			try {
				InputStream stream = body.getContent();
				reader = new InputStreamReader(stream);
			} catch (IllegalStateException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			JSONObject responseObject = null;
			try {
				responseObject = (JSONObject) jsonParser.parse(reader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			} catch (NoSuchElementException e) {
				throw new RuntimeException(e);
			}
			
			if (responseObject.containsKey("error")) {
				System.out.println(responseObject);
				throw new RuntimeException("Facebook error");
			}
			
			return responseObject;
		}
		
		@Override
		public void completed(HttpResponse response) {
			JSONObject responseObject = getJsonResponse(response);
			JSONArray postsJson = (JSONArray) responseObject.get("data");
			
			System.out.println("Retrieved posts for " + sourceId);

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
			System.out.println("Retrieved comments for " + this.post.getUid());
			
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
