package com.torahsummary.betamidrash;

import java.io.IOException;
import java.io.InputStream;
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
import android.widget.Toast;

public class API {
	final static String BASE_URL = "http://www.sefaria.org/api/texts/";
	final static String ZERO_CONTEXT = "&context=0";
	final static String ZERO_COMMENTARY = "&commentary=0";
	//TODO possibly add reference userID so sefaria can get some user data
	
	protected List<Text> textList = new ArrayList<Text>();
	public boolean isDone = false;
	String sefariaData = null;

	//see if function works
	protected List<Text> fetchSefariaData(String urlString){
		textList = new ArrayList<Text>(); //make sure to clear the list to prevent weird things from happening
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.connect();

			InputStream stream = conn.getInputStream();
			String data = convertStreamToString(stream);
			//Log.d("api", data);
			textList = parseJSON(data);
			Log.d("api","textList in fetchSefariaData: size:" + textList.size());


		} catch (MalformedURLException e) {
			e.printStackTrace();
			Log.d("ERROR", "malformed url");
			//TODO make toast or message to user
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("ERROR", "io exception");
		}

		isDone = true;
		return textList;
	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}


	private static List<Text> parseJSON(String in) {
		List<Text> textList = new ArrayList<Text>();

		try {
			JSONObject jsonData = new JSONObject(in);
			JSONArray textArray = jsonData.getJSONArray("text");
			JSONArray heArray = jsonData.getJSONArray("he");
			//Log.d("api", textArray.toString());
			int maxLength = Math.max(textArray.length(),heArray.length());
			//TODO generalize to more than 2D array. Probably make recursive function call
			for (int i = 0; i < maxLength; i++) {
				//get the texts if i is less it's length (otherwise use "") 
				String enText = i < textArray.length() ? textArray.getString(i) : "";
				String heText = i < textArray.length() ? heArray.getString(i)  : "";
				Text text = new Text(enText, heText);
				//Log.d("api", i + text.toString());
				//text.levels = levels; //TODO get full level info in there
				text.levels[0] = text.level1 = i+1;

				textList.add(text);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e("JSONthing", "error processing json data");
		}       
		return textList;

	}

	//CHANGED RETURN VALUE TO VOID: STRING WITH JSON DATA WILL BE RETURNED IN fetchSefariaData METHOD (ES):
	static public List<Text> getTextsFromAPI(String bookTitle, int[] levels){ //(String booktitle, int []levels)

		//place should really come from book title and levels ex. [5,1]
		//

		//loop through with place += "." + level[i];
		/*String place = bookTitle + "." + levels[1] + "." + levels[0];
		String completeUrl = BASE_URL + place + ZERO_CONTEXT;
		API api = new API();
		api.new JSONParserTask().execute(completeUrl);

		return textList;*/
		Log.d("api",bookTitle + " " +  levels.toString() );
		String place = bookTitle.replace(" ", "_"); //the api call doesn't have spaces
		for(int i= levels.length-1;i>=0;i--){
			//TODO error check on bad input (like [1,0,1] which doesn't make any sense)
			if(levels[i]== 0)
				continue;
			place += "." + levels[i];
		}
	
		String completeUrl = BASE_URL + place + "?" + ZERO_CONTEXT + ZERO_COMMENTARY;
		Log.d("api",completeUrl);
		API api = new API();
		api.new JSONParserTask().execute(completeUrl);
		try {
			while(!api.isDone){
				//TODO maybe use something smarter to do this
				Thread.sleep(10);
			}
		}catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d("api", "in getTextsFromAPI: api.textlist.size:" + api.textList.size() + "api.done:" + api.isDone);
		
		return api.textList;

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


	private class JSONParserTask extends AsyncTask <String, Void, List<Text>> {
		@Override
		protected List<Text> doInBackground(String... params) {
			List<Text> result = fetchSefariaData(params[0]);
			return result;
		}

		@Override
		protected void onPostExecute(List<Text> result) {
			//TODO: FILL IN: 
			Log.d("api", "in onPostExecute... result.size():" + result.size() + "... textList.size():"+ textList.size() + "done:" + isDone);
			//How about using intent to push the List<Text> to Text.java using Parcelable, as Text class already implements it? (ES)
			Intent intent = new Intent();
		}
	}
}

