package com.conversocial.iospeedtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.impl.nio.conn.PoolingClientAsyncConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.conn.ClientAsyncConnectionManager;
import org.apache.http.nio.conn.ManagedClientAsyncConnection;
import org.apache.http.nio.conn.scheme.AsyncSchemeRegistry;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.BasicHttpContext;


public class GraphClient {
	PoolingClientAsyncConnectionManager manager;
	private HttpAsyncClient httpclient;
	private String token;
	public final String HOST = "https://graph.facebook.com";
	
	private final AtomicInteger requestCounter = new AtomicInteger();
	private final BlockingQueue<Task> queue = new LinkedBlockingQueue<Task>();
	private GetterThread getterThread;
	
	public GraphClient(String token, int maxPerRoute, int ioThreads) throws IOReactorException {
		this.token = token;
		
		IOReactorConfig config = new IOReactorConfig();
		config.setIoThreadCount(ioThreads);
		config.setShutdownGracePeriod(3000);
		config.setConnectTimeout(1000);
		config.setSoTimeout(1000);
		ConnectingIOReactor reactor = new DefaultConnectingIOReactor(config);
		
		manager = new PoolingClientAsyncConnectionManager(reactor);
		manager.setMaxTotal(200);
		manager.setDefaultMaxPerRoute(maxPerRoute);
		
		
		this.httpclient = new DefaultHttpAsyncClient(manager);
		this.httpclient.start();
		
		getterThread = new GetterThread();
		getterThread.start();
	}
	
	private final class CallbackThing implements FutureCallback<HttpResponse> {
		private Task task;
		public CallbackThing(Task task) {
			this.task = task;
		}
		
    	@Override
    	public void completed(HttpResponse result) {
    		try {
    			task.callback.completed(result);
    		} finally {
				requestCounter.decrementAndGet();
    		}
    	}

		@Override
		public void cancelled() {
			requestCounter.decrementAndGet();
		}

		@Override
		public void failed(Exception ex) {
			requestCounter.decrementAndGet();
		}
	}
	
	private final class GetterThread extends Thread {
		public AtomicBoolean die = new AtomicBoolean(false);
		
		@Override
		public void run() {
			this.setName("Getter");
			
			while (!die.get()) {
				try {
					final Task task = queue.poll(100, TimeUnit.MILLISECONDS);
					System.out.println(queue.size() + "," + requestCounter.get());
					
					if (task == null) {
						continue;
					}
					
					ArrayList<String> paths = new ArrayList<String>(task.paths);
					paths.add(0, HOST);
					String path = StringUtils.join(paths, "/");
				
					List<String> pairs = new ArrayList<String>();
					pairs.add("access_token=" + token);
					for (Map.Entry<String, String> kv : task.queryParams.entrySet()) {
						pairs.add(kv.getKey() + "=" + kv.getValue());
					}
					String queryString = StringUtils.join(pairs, "&");
					
			        HttpGet request = new HttpGet(path + "?" + queryString);
			        request.addHeader("Accept-encoding", "gzip");

			        httpclient.execute(request, new CallbackThing(task));

					
				} catch (InterruptedException e) {
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	private final static class Task {
		protected final List<String> paths;
		protected final Map<String, String> queryParams;
		protected final FutureCallback<HttpResponse> callback;
		
		public Task(List<String> paths, Map<String, String> queryParams,
				FutureCallback<HttpResponse> callback) {
			super();
			this.paths = paths;
			this.queryParams = queryParams;
			this.callback = callback;
		}
	}


	/**
	 * 
	 * Makes a get request to the Facebook Graph API. The url will be built by
	 * joining paths and appending them to https://graph.facebook.com. The 
	 * query parameters will be added to the URL.
	 * 
	 * @param paths
	 * @param queryParams
	 * @param callback
	 */
	public void get(List<String> paths, Map<String, String> queryParams, FutureCallback<HttpResponse> callback) {
		requestCounter.incrementAndGet();
		queue.add(new Task(paths, queryParams, callback));
	}
	
	public void shutDownWhenFinished() throws InterruptedException, IOException {
		while (true) {
			Thread.sleep(500);
			PoolStats stats = manager.getTotalStats();
			if (0 == stats.getLeased() && 0 == requestCounter.get()) {
				break;
			}
			
		}
		getterThread.die.set(true);
		getterThread.join();
		httpclient.shutdown();
	}
}
