package com.conversocial.iospeedtest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SpeedTest {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String token = args[0];
		System.out.println(token);
		
        BufferedReader reader = new BufferedReader(
        		new FileReader("/home/glloyd/scratch/iospeedtest/files/source_ids.txt"));
        
        Set<String> sourceIds = new HashSet<String>();
        String sourceId = null;
		while((sourceId = reader.readLine()) != null) {
			sourceIds.add(sourceId);
		}
		
        System.out.println(sourceIds);		
	}

}
