package com.torahsummary.betamidrash;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class API {
	final static String TEXT_URL = "http://www.sefaria.org/api/texts/";
	final static String COUNT_URL = "http://www.sefaria.org/api/counts/";
	final static String SEARCH_URL = "http://search.sefaria.org:788/sefaria/_search/";
	final static String ZERO_CONTEXT = "&context=0";
	final static String ZERO_COMMENTARY = "&commentary=0";
	//TODO possibly add reference userID so sefaria can get some user data
	
	private String data = "";
	private boolean isDone = false;
	String sefariaData = null;
	final static int READ_TIMEOUT = 3000;
	final static int CONNECT_TIMEOUT = 3000;
	//TODO determine good times

	
	static protected String fetchData(String urlString){
		String data = "";
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setReadTimeout(READ_TIMEOUT);
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.connect();
			InputStream stream = conn.getInputStream();
			data = convertStreamToString(stream);
			
			//TODO handle timeouts ... messages, or maybe increase timeout time, etc.
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Log.d("ERROR", "malformed url");
			//TODO make toast or message to user
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("ERROR", "io exception");
		}
		return data;
	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}


	private static List<Text> parseJSON(String in,int [] levels) {
		List<Text> textList = new ArrayList<Text>();

		try {
			JSONObject jsonData = new JSONObject(in);
			Log.d("api", "jsonData:" + jsonData.toString());
			
			//TODO make work for 1 and 3 (or more) levels of depth (exs. Hadran, Arbaah Turim)
			JSONArray textArray = jsonData.getJSONArray("text");
			JSONArray heArray = jsonData.getJSONArray("he");
			
			
			int maxLength = Math.max(textArray.length(),heArray.length());
			Log.d("api",textArray.toString() + " " + heArray.toString());
			for (int i = 0; i < maxLength; i++) {
				//get the texts if i is less it's within the length (otherwise use "") 
				String enText = "";
				enText = textArray.getString(i);
				String heText = "";
				heText = heArray.getString(i);
				Text text = new Text(enText, heText);
				
				//Log.d("api", i + text.toString());
				text.levels = levels; //TODO get full level info in there
				text.levels[0] = text.level1 = i+1;

				textList.add(text);
			}			
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e("api", "error processing json data");
		}       
		return textList;

	}

	//add ability to not wait for task (and return api; so that api.data can be used later) 
	public static String getDataFromURL(String url){
		Log.d("api",url);
		API api = new API();
		api.new GetDataTask().execute(url);
		//creating an instance of api which will fetch data then wait until this is done to return data
		try {
			while(!api.isDone){
				//TODO maybe use something smarter to do this - make a timeout just in case
				Thread.sleep(10);
			}
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		String data = api.data;
		Log.d("api","in getDataFromURL: data length: " + data.length() );
		return data;
	}
	
	private static List<Text> getSearchResults(String query,int from) {
		List<Text> texts = new ArrayList<Text>();
		String url = SEARCH_URL + "?" + "q=%D7%90%D7%95%D7%9E%D7%A8" + "&from=" +from; 
		 
		//TODO make working function
		return texts;
		
	}
	
	
	public static ArrayList<Integer> getChaps(String bookTitle, int [] levels){
		String place = bookTitle.replace(" ", "_"); 
		String url = COUNT_URL + place;
		String data = getDataFromURL(url);
		
		ArrayList<Integer> chapList = new ArrayList<Integer>();
		try {
			JSONObject jsonData = new JSONObject(data);
			JSONArray counts = jsonData.getJSONObject("_all").getJSONArray("availableTexts");
			for(int i=levels.length-1;i>=0;i--){
				if(levels[i] == 0)
					continue;
				counts = counts.getJSONArray(levels[i]-1);//-1 b/c the first chap of levels is 1, the array is zero indexed
			}
			int totalChaps = counts.length();
			for(int i=0;i<totalChaps;i++){
				try{
					if(counts.getJSONArray(i).length()>0)
						chapList.add(i+1);
				}catch(JSONException e){//most likely it's b/c it only has one level
					chapList.add(i+1);
				}
			}
		}catch(Exception e){
			Log.e("api","Error: " + e.toString());
		}
		return chapList;
		
	}
	
	
	static public List<Text> getTextsFromAPI(String bookTitle, int[] levels){ //(String booktitle, int []levels)
		String place = bookTitle.replace(" ", "_"); //the api call doesn't have spaces
		for(int i= levels.length-1;i>=0;i--){
			//TODO error check on bad input (like [1,0,1] which doesn't make any sense)
			if(levels[i]== 0)
				continue;
			place += "." + levels[i];
		}
		String completeUrl = TEXT_URL + place + "?" + ZERO_CONTEXT + ZERO_COMMENTARY;
		String data = getDataFromURL(completeUrl);
		List<Text> textList = parseJSON(data,levels);
		Log.d("api", "in getTextsFromAPI: api.textlist.size:" + textList.size());
		return textList;
	}
	//Sefaria levels:
	// booktitle.biggestlevel.smaller.smallest
	//Genesis.perek.pasuk
	//level1 = smallest //pasuk
	//level2 = bigger //perek 
	//[5,1]
	//[0,1] -- the whole perek
	//[0,1,2] -- mistake in Chumash
	//[0,0] 

	private class GetDataTask extends AsyncTask <String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String result = fetchData(params[0]);
			data = result;//put into data so that the static function can pull the data
			isDone = true; 
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			//TODO: FILL IN:
			//Log.d("api", "in onPostExecute: data length: " + result.length());
			//How about using intent to push the List<Text> to Text.java using Parcelable, as Text class already implements it? (ES)
			//Intent intent = new Intent();
		}
	}

	
}

