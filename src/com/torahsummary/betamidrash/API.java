package com.torahsummary.betamidrash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class API {
	final static String BASE_URL = "http://www.sefaria.org/api/texts/";
	final static String ZERO_CONTEXT = "?context=0";
	protected static List<Text> textList = null;

	//see if function works
	protected String fetchSefariaData(String urlString){
		
		String sefariaData = null;

        try {
           URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.connect();

            InputStream stream = conn.getInputStream();
            String data = convertStreamToString(stream);

            String[] dataArr = parseJSON(data);
            if (dataArr[0] != null && dataArr[1] != null) {
                sefariaData = "Text: " + dataArr[0] +
                        "\nhe: " + dataArr[1];
            }
           Text text = new Text(dataArr[0], dataArr[1]);
           textList.add(text);
           
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d("ERROR", "malformed url");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ERROR", "io exception");
        }
        return sefariaData;
	}
	
	 static String convertStreamToString(java.io.InputStream is) {
	        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
	        return scanner.hasNext() ? scanner.next() : "";
	    }
	
	
	 protected String[] parseJSON(String in) {
	        String[] dataArr = {null, null};

	        try {
	            JSONObject jsonData = new JSONObject(in);
	            JSONArray textArray = jsonData.getJSONArray("text");
	            JSONArray heArray = jsonData.getJSONArray("he");
	            
	            for (int i = 0; i < textArray.length(); i++) {
	                dataArr[0] += textArray.getString(i);
	                Log.v("JSONthing", dataArr[0]);
	            }
	          
	            for (int i = 0; i < heArray.length(); i++){
	                dataArr[1] += heArray.getString(i);
	                Log.v("JSONthing", dataArr[1]);
	            }

	        } catch (JSONException e) {
	            e.printStackTrace();
	            Log.e("JSONthing", "error processing json data");
	        }
	        return  dataArr;

	    }
	 
	 
	
	 //CHANGED RETURN VALUE TO VOID: STRING WITH JSON DATA WILL BE RETURNED IN fetchSefariaData METHOD (ES):
	public static void getTextsFromAPI(String bookTitle, int[] levels){ //(String booktitle, int []levels)
		
		//place should really come from book title and levels ex. [5,1]
		//
		
		//loop through with place += "." + level[i];
		String place = bookTitle + "." + levels[1] + "." + levels[0];
		String completeUrl = BASE_URL + place + ZERO_CONTEXT;
		API api = new API();
		api.new JSONParserTask().execute(completeUrl);
		
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
		/*String json = getJSON(place);
		Text text = new Text();*/
		//parse json
		//this is where you need to use jackson stuff
		//not really sure how to use the library, but here's some pseudocode
		//JSONobject = jsonCOInverter(jsonString);
		//String content = jsonObject.getArray("text");
		//loop each verse
		//String verse = content.getItem(0)
		//text.bid = 101;//we'll figure out what this number is later //Book(title).bid
		//text.text = verse;
		//text.level1 = 5;
		//text.level2 = 1;
		///text.heText = from json.getArray("he"),get(0);
		
		/*
		 * 
	public int tid; //0 
	public int bid;
	public String enText;
	public String heText;
	public int level1;
	public int level2;
	public int level3;
	public int level4;
	public int level5;
	public int level6;
	public int [] levels;
	//public int hid;
	public boolean displayNum;
		 * 
		 * 
		 * 
		 */
		

		//return text;

	//}

	//once you can get a single Text from a json, we can make it work for a whole perek:
	//CHANGED RETURNT TO VOID, 
	public static List<Text>  getTextsFromAPI(){ //(String booktitle, int []levels){
		List<Text> textList = new ArrayList<Text>();
		//similar things to single function... once this is working it should be able to be used instead of single function:
		// and if you ask for an exact text say Genesis, [5,1] it will be a List of 1. And if you ask for the whole 1st perek, say, Genesis [0,1] it will give you a List of texts
		return textList;
	}
	 private class JSONParserTask extends AsyncTask <String, Void, String> {
	        @Override
	        protected String doInBackground(String... params) {
	            String result = fetchSefariaData(params[0]);
	            return result;
	        }

	        @Override
	        protected void onPostExecute(String result) {
	        	//TODO: FILL IN: 
	        	//How about using intent to push the List<Text> to Text.java using Parcelable, as Text class already implements it? (ES)
	        }
	    }
	}

