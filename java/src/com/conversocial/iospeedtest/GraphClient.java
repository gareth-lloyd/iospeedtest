package com.conversocial.iospeedtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
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


public class GraphClient {
	PoolingClientAsyncConnectionManager manager;
	private HttpAsyncClient httpclient;
	private String token;
	public final String HOST = "https://graph.facebook.com";
	
	public GraphClient(String token) throws IOReactorException {
		this.token = token;
		
		IOReactorConfig config = new IOReactorConfig();
		config.setShutdownGracePeriod(3000);
		config.setConnectTimeout(1000);
		config.setSoTimeout(1000);
		ConnectingIOReactor reactor = new DefaultConnectingIOReactor(config);
		
		manager = new PoolingClientAsyncConnectionManager(reactor);
		manager.setMaxTotal(200);
		manager.setDefaultMaxPerRoute(200);
		
		
		this.httpclient = new DefaultHttpAsyncClient(manager);
		this.httpclient.start();
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
	 * @return
	 */
	public Future<HttpResponse> get(List<String> paths, Map<String, String> queryParams, FutureCallback<HttpResponse> callback) {
		paths = new ArrayList<String>(paths);
		paths.add(0, this.HOST);
		String path = StringUtils.join(paths, "/");
	
		queryParams.put("access_token", this.token);
		List<String> pairs = new ArrayList<String>();
		for (Map.Entry<String, String> kv : queryParams.entrySet()) {
			pairs.add(kv.getKey() + "=" + kv.getValue());
		}
		String queryString = StringUtils.join(pairs, "&");
		
        HttpGet request = new HttpGet(path + "?" + queryString);

        return httpclient.execute(request, callback);
	}
	
	public void shutDownWhenFinished() throws InterruptedException, IOException {
		while (true) {
			Thread.sleep(500);
			PoolStats stats = manager.getTotalStats();
			System.out.println(stats.getLeased());
			if (0 == stats.getLeased()) {
				break;
			}
		}
		httpclient.shutdown();
	}
}
