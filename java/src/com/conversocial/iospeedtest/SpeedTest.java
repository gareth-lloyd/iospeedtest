package com.conversocial.iospeedtest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SpeedTest {
	
	private static Set<String> getSourceIds(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        
        Set<String> sourceIds = new HashSet<String>();
        String sourceId = null;
		while((sourceId = reader.readLine()) != null) {
			sourceIds.add(sourceId);
		}
		
		return sourceIds;
	}
	
	public static void main(String[] args) throws Exception {
		String token = args[0];
		Set<String> sourceIds = getSourceIds("../files/source_ids.txt");
		
		long start = System.currentTimeMillis();
		
		GraphClient graphClient = null;
		
		graphClient = new GraphClient(token);			
		for (String sourceId : sourceIds) {
			new PollingTask(sourceId, graphClient).performTask();
		}
		graphClient.shutDownWhenFinished();
		
		long end = System.currentTimeMillis();
		
		DataStore.getInstance().printStats();
		System.out.println("took " + Long.toString((end - start) / 1000) + "s");
	}
}
