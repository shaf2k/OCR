package com.shaf2kstudios.ocr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import org.json.JSONObject;

import android.location.Location;
import android.util.Log;


public class MapAPI {

	private static final String geoPointConverterLink = "https://maps.googleapis.com/maps/api/geocode/json?";
	
	private static final String apiKey = "AIzaSyDAlhGDGoZevsXvkpcC0rVCvw2NJFyX6s0";


	public static String getAddress(Location loc) {
		Properties props = new Properties();

		try {
			props.setProperty("latLng", String.valueOf(loc.getLatitude())+","+String.valueOf(loc.getLongitude()));
			props.setProperty("key", apiKey);
			
			String query = "";
			for(Object key : props.keySet()) {
				query+=(String) key+"="+props.getProperty((String) key)+"&";
			}
			// Set to google to get geo-point
			URL url = new URL(geoPointConverterLink+query);
			Log.i("TAG", "Posting api requst to "+query);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String output =""; String data = "";
			
			while((output = reader.readLine()) != null ) {
				data += output;
			}
			Log.i("TAG", "Got output: "+data);
			JSONObject obj = new JSONObject(data);
			Log.i("TAG", "Status result: "+obj.getString("status"));
			JSONObject part1 = obj.getJSONArray("results").getJSONObject(0);
			String add = part1.getString("formatted_address");
			return add;
		} catch(Exception e) {
			Log.e("TAG", "Error getting geo point: "+e);
		}
		return "";
	}
	
}