package com.torahsummary.betamidrash;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.database.SQLException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.torahsummary.betamidrash.API.APIException;

//this class saves menu data and keeps track of menu history
public class AppMenu {

	public static final String JSONMenuPath = "sefariaMenu2.json";
	public static String title; //title of the current menu level
	public static ArrayList<String> lastTitles;
	private static Context context;
	public static JsonNode menuRoot;
	public static boolean inited = false;
	public static boolean bidTreeInited = false;
	public static int currLevel;
	private static JsonNode currNodeArray; //the array of strings that are the values of the menu items
	private static ArrayList<JsonNode> currPath;
	public static ArrayList<Integer> currIntPath;
	public static ArrayList<Integer> prevLevels; //for text menu, so that you can get a text using levels array
	public static boolean inJsonMenu; //true before you reach the text levels of a book
	public static boolean isBookmarkable; //true if you're on the cusp of a book. good for bookmarking
	public static int textMenuLvl; //the level of the menu that you switched from json to text
	public static Book currBook;
	public static ArrayList<Header> currChaps;
	public static ArrayList<Integer> currChapInts;
	public static ArrayList<String> engItems; //saves the eng titles per menu level so that they can be looked up in db
	public static String[] currChapStrings;
	public static HashMap<String,String> bidMap; //stores all bids in a given top-menu-level.
	public static Typeface customFont;
	public static int customFontId = SettingsActivity.SBL;
	public static int displayLang = 0;
	public static int topMenuIndex = 0; //so that you can go to same list position when going back from book
	public static ArrayList<String> enBookNames; //a list of all book names for autocomplete tv in menu
	public static ArrayList<String> heBookNames;
	public static int restoredByActivity; //the activity which last restored the variables
	
	//initialize menu json
	//returns children of root menu level
	static String[] init(Context con) {
		inited = true;
		context = con;
		currLevel = 0;
		title = "BetaMidrash";
		lastTitles = new ArrayList<String>();
		lastTitles.add(title);
		inJsonMenu = true;
		isBookmarkable = false;
		prevLevels = new ArrayList<Integer>();
		engItems = new ArrayList<String>();
		initFont(SettingsActivity.SBL);
		
		try {
			enBookNames = Book.getAllBookNames(false);
			heBookNames = Book.getAllBookNames(true);
		} catch (SQLException e) {
			enBookNames = new ArrayList<String>();
			heBookNames = new ArrayList<String>();
			//DialogManager.showDialog(DialogManager.LIBRARY_EMPTY);
		}
		ObjectMapper m = new ObjectMapper();  //jackson json getter
		try {
			File indexFile = new File(MyApp.INTERNAL_FOLDER + "/" + JSONMenuPath);
			if(indexFile.exists()) {    
				menuRoot = m.readTree(indexFile);
			}
			else{
				menuRoot = m.readTree(context.getResources().getAssets().open(JSONMenuPath));
			}
		/*try {
			File indexFile = new File(MyApp.INTERNAL_FOLDER + "/" + JSONMenuPath);
			if(indexFile.exists()) {    
				menuRoot = m.readTree("http://www.sefaria.org/api/index");
				Log.d("API TEST", "Success");*/

			currNodeArray = menuRoot;
			
			currIntPath = new ArrayList<Integer>();
			currPath = new ArrayList<JsonNode>();
			currPath.add(currNodeArray);
			return getNamesOfChildren(currNodeArray,true);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			MyApp.sendException(e);
			//Log.d("API TEST", "failed: JSON processing exception");
			return new String[0];
		} catch (IOException e) {
			MyApp.sendException(e);
			//Log.d("APT TEST", "failed: IO Exception");
			e.printStackTrace();
			return new String[0];
		}


	}

	static String[] back() {
		lastTitles.remove(lastTitles.size()-1);
		title = lastTitles.get(lastTitles.size()-1);
		
		
		int currTextLevel;
		if (currBook != null) currTextLevel = currBook.textDepth - (currLevel - textMenuLvl);
		else currTextLevel = (currLevel - textMenuLvl);
		if (currLevel == 0) return new String[0]; //dont go back at home
		else if (inJsonMenu || currBook == null || currTextLevel == currBook.textDepth) {
			if (currBook != null && currTextLevel == currBook.textDepth) {
				inJsonMenu = true;
				currBook = null;
				currLevel--;
				textMenuLvl = 0;

				return getNamesOfChildren(currNodeArray,true);
			} else {
				currIntPath.remove(currIntPath.size()-1);
				currPath.remove(currPath.size()-1);
				currNodeArray = currPath.get(currPath.size()-1);
				currLevel--;

				isBookmarkable = false; //by this point it should always be false, I think
				//Toast.makeText(context, "CURR LEVEL " + currLevel, Toast.LENGTH_SHORT).show();
				//Log.d("MainActivity","CHILDREN BACK: " + getNamesOfChildren(currNodeArray));
				return getNamesOfChildren(currNodeArray,true);
			}
		} else { //you're going back a text menu level
			//try{
				//title = title.substring(0,title.indexOf(" " + lastTitles.get(lastTitles.size()-1)));
				
			/*} catch(StringIndexOutOfBoundsException e){
				Log.e("title", "" + e); //TODO this error should try to be avoided. JH-> NOAH.
				title = "Title";
				MyApp.sendException(e);
			}*/

			
			currLevel--;
			prevLevels.remove(0);
			return getNamesOfChildren(currTextLevel);
		}
	}
	//catName = name of item you clicked on
	//pos = position in list
	static String[] forward(String catName, int pos) {
		JsonNode jsonArray = currNodeArray; //the child node strings 
		if (inJsonMenu && !jsonArray.get(pos).findPath("contents").isMissingNode()) { //not a book
			//Log.d("MainActivity","CURR LVL SIZE: " + ((Integer)jsonArray.size()).toString());
			currNodeArray = jsonArray.get(pos).findPath("contents");
			currIntPath.add(pos);
			currPath.add(currNodeArray);
			//if next level has book, then you can bookmark
			if (currNodeArray.get(0).findPath("contents").isMissingNode()) isBookmarkable = true;

			title = catName;
			lastTitles.add(title);
			currLevel++;
			//Toast.makeText(context, "CURR LEVEL " + currLevel, Toast.LENGTH_SHORT).show();
			return getNamesOfChildren(currNodeArray,true);
		} else { //inTextMenu or switching to text menu right now

			if (inJsonMenu && jsonArray.get(pos).findPath("category").isMissingNode()) {//switching to text menu right now
				String engName = engItems.get(pos);
				Log.d("MainActivity","Name " + engName);
				currBook = new Book(engName);
				currLevel++;
				inJsonMenu = false;
				title = catName;
				lastTitles.add(catName);
				textMenuLvl = currLevel;
				if (currBook.bid == 0) return new String[0];
			} else {
				currLevel++;
				prevLevels.add(0,Integer.valueOf(pos+1)); //add to front so that it's in the right order for levels
				title = title + " " + catName; //this is kinda a guess, but it should look nice
				lastTitles.add(catName);
			}
			int currTextLevel = currBook.textDepth - (currLevel - textMenuLvl);
			if (currTextLevel+1 == currBook.wherePage) { //display book
				//Toast.makeText(context, "BOOOOOK", Toast.LENGTH_SHORT).show();
				//prevLevels.add(0,0);
				return new String[]{"__atBook__"};
			}
			return getNamesOfChildren(currTextLevel);

		}
		//return new String[0];
	}
	
	//used for autocomplete right now
	//jumps directly to given book, and deletes any history up until now
	static String[] jumpToBook(String bookName) {
		home(); //reset all variables
		int index = heBookNames.indexOf(bookName);
		
		String enBookName;
		String heBookName;
		if (index != -1) { //you entered hebrew
			enBookName = enBookNames.get(index); //in case you entered a hebrew name, convert it to english
			heBookName = bookName;
			if (displayLang == SettingsActivity.HE_CODE) {
				title = heBookName;
				lastTitles.add(heBookName);
			} else {
				title =  enBookName;
				lastTitles.add(enBookName);
			}
		} else { //english entered
			enBookName = bookName;
			heBookName = heBookNames.get(enBookNames.indexOf(bookName));
			if (displayLang == SettingsActivity.HE_CODE) {
				title = heBookName;
				lastTitles.add(heBookName);
			} else {
				title =  enBookName;
				lastTitles.add(enBookName);
			}
		}
		currBook = new Book(enBookName);
		currLevel++;
		inJsonMenu = false;

		textMenuLvl = currLevel;
		//just in case things go wrong
		if (currBook.bid == 0) return new String[0];
		
		int currTextLevel = currBook.textDepth - (currLevel - textMenuLvl);
		return getNamesOfChildren(currTextLevel);
	}

	static String[] home() {
		currNodeArray = menuRoot;
		currIntPath = new ArrayList<Integer>();
		currPath = new ArrayList<JsonNode>();
		currPath.add(currNodeArray);
		currLevel = 0;
		inJsonMenu = true;
		isBookmarkable = false;
		currBook = null;
		textMenuLvl = 0;
		prevLevels = new ArrayList<Integer>();
		title = "BetaMidrash";
		//Toast.makeText(context, "CURR LEVEL " + currLevel, Toast.LENGTH_SHORT).show();
		return getNamesOfChildren(currNodeArray,true);
	}

	static String[] getNamesOfChildren(boolean removeSparse) {
		return getNamesOfChildren(currNodeArray, removeSparse);
	}

	static String[] getNamesOfChildren(JsonNode node, boolean removeSparse) {
		return getNamesOfChildren(node, removeSparse, false, engItems);
	}

	//get names for json level of menu
	//TODO should alwaysGetHetitle actually be called alwaysGetEntitle???
	static String[] getNamesOfChildren(JsonNode node, boolean removeSparse, boolean alwaysGetHetitle, ArrayList<String> engItems) {
		if (engItems != null) engItems.clear();
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < node.size(); i++) {
			JsonNode tempEn = null;
			JsonNode temp = null;
			if(displayLang == 2 && !alwaysGetHetitle && node.get(i).has("heCategory")){//hebrew
				temp = node.get(i).findPath("heCategory");
				tempEn = node.get(i).findPath("category");
				//can use for test (instead of .has()): if(!temp.asText().equals("")) temp = temp = node.get(i).findPath("category");
			}
			else {
				temp = node.get(i).findPath("category");
				tempEn = temp;
			}


			if (temp.isMissingNode()){//upto book title
				if(displayLang == 2 && !alwaysGetHetitle && node.get(i).has("heTitle")) {//hebrew
					temp = node.get(i).findPath("heTitle");
					tempEn = node.get(i).findPath("title");
				}else {
					temp = node.get(i).findPath("title");
					tempEn = temp;
				}

			}


			/*if (removeSparse) {
				JsonNode percAvail = node.get(i).findPath("percentAvailable");
				if (!percAvail.isMissingNode()) {
					float enPerc = percAvail.get("en").floatValue();
					float hePerc = percAvail.get("he").floatValue();
					if (!(enPerc > 10 || hePerc > 10)) {
						continue;
					}
				}
			}*/

			names.add(temp.textValue());
			if (engItems != null) engItems.add(tempEn.textValue());
			//
		}
		//Toast.makeText(context, Arrays.toString(names), Toast.LENGTH_SHORT).show();
		String[] arrayNames = names.toArray(new String[0]);
		return arrayNames;
	}

	//get names for text level of menu
	//lvlNum is the current lvl of the text structure you're at
	//prevLvls is an array of the previous lvls in the text structure
	static String[] getNamesOfChildren(int lvlNum){ //, int[] prevLvls) {
		//List<Integer> numInLvl = Text.getChaps(currBook.bid, getLevels());
		
		try {
			currChaps = Header.getHeaderChaps(currBook, getLevels());
			currChapInts = Text.getChaps(currBook.bid, getLevels());
		} catch (APIException e) {
			currChaps = new ArrayList<Header>();
			currChapInts = new ArrayList<Integer>();
			Toast.makeText(MyApp.currActivityContext,R.string.apiexception, Toast.LENGTH_SHORT).show();
		}
		
		String[] names = new String[currChaps.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = currChaps.get(i).enHeader;
		}
		currChapStrings = names;
		//Log.d("lv",Arrays.toString(names));
		return names;
	}

	static int[] getLevels() {
		int[] levels = new int[currBook.textDepth];
		//put in all previous levels
		for (int j = 0; j < prevLevels.size(); j++) {
			levels[levels.length-1-j] = (int) prevLevels.get(prevLevels.size()-1-j);
		}
		return levels;
	}

	static void saveBms() {

	}
	static void initBidTree() {
		//init datastructure
		bidMap = new HashMap<String,String>();
		//TODO  you need to make sure this is always english
		String[] names = getNamesOfChildren(menuRoot,false,true, null);
		for (int i = 0; i < menuRoot.size(); i++) {
			//recursively get bids
			ArrayList<String> tempBids = getBids(menuRoot.get(i).findPath("contents"));
			String tempName = names[i];
			for (int j = 0; j < tempBids.size(); j++) {
				bidMap.put(tempBids.get(j), tempName);
			}
		}

		bidTreeInited = true;
	}

	private static ArrayList<String> getBids(JsonNode node) {
		ArrayList<String> tempBids = new ArrayList<String>();
		//TODO you need to make sure this is always english
		String[] names = getNamesOfChildren(node,false, true, null);
		for (int i = 0; i < names.length; i++) {
			//this is a book, so add its bid
			JsonNode tempNode = node.get(i).findPath("contents");

			if (tempNode.isMissingNode()) {
				tempBids.add(names[i]);
			} else {
				//recurse
				tempBids.addAll(getBids(tempNode));
			}
		}

		return tempBids;
	}
	
	public static void initFont(int fontId) {
		String fontPath;
		if (fontId == SettingsActivity.SBL) {
			fontPath = "fonts/hebFont.ttf";
		} else {
			return;
		}
		customFont = Typeface.createFromAsset(context.getAssets(), fontPath);
	}
	
	//when activity is recovered, we need to recover full jsonNode path from currStringPath
	public static void recoverJsonNodes() {
		currNodeArray = menuRoot;
		currPath = new ArrayList<JsonNode>();
		currPath.add(menuRoot);
		for (int i = 0; i < currIntPath.size(); i++) {
			int pos = currIntPath.get(i);
			currNodeArray = currNodeArray.get(pos).findPath("contents");
			currPath.add(currNodeArray);
		}
		
	}
	
	public static void saveState(Bundle outState) {
		outState.putStringArrayList("lastTitles", AppMenu.lastTitles);
		outState.putString("title", AppMenu.title);
		outState.putBoolean("inited",AppMenu.inited);
		outState.putBoolean("bidTreeInited", AppMenu.bidTreeInited);
		outState.putInt("currLevel", AppMenu.currLevel);
		outState.putIntegerArrayList("prevLevels", AppMenu.prevLevels);
		outState.putBoolean("inJsonMenu", AppMenu.inJsonMenu);
		outState.putBoolean("isBookmarkable", AppMenu.isBookmarkable);
		outState.putInt("textMenuLvl",AppMenu.textMenuLvl);
		outState.putParcelable("currBook", AppMenu.currBook);
		outState.putParcelableArrayList("currChaps", AppMenu.currChaps);
		outState.putIntegerArrayList("currChapInts", AppMenu.currChapInts);
		outState.putStringArrayList("engItems", AppMenu.engItems);
		outState.putStringArray("currChapStrings", AppMenu.currChapStrings);
		outState.putSerializable("bidMap", AppMenu.bidMap);
		outState.putInt("customFontId",AppMenu.customFontId);
		outState.putInt("displayLang",AppMenu.displayLang);
		outState.putInt("topMenuIndex", AppMenu.topMenuIndex);
		outState.putIntegerArrayList("currIntPath", AppMenu.currIntPath);
	}
	
	public static void restoreState(Context context, Bundle in) {
		AppMenu.init(context);
		
		AppMenu.lastTitles = in.getStringArrayList("lastTitles");
		AppMenu.title = in.getString("title");
		AppMenu.inited = in.getBoolean("inited");
		AppMenu.bidTreeInited = in.getBoolean("bidTreeInited");
		AppMenu.currLevel = in.getInt("currLevel");
		AppMenu.prevLevels = in.getIntegerArrayList("prevLevels");
		AppMenu.inJsonMenu = in.getBoolean("inJsonMenu");
		AppMenu.isBookmarkable = in.getBoolean("isBookmarkable");
		AppMenu.textMenuLvl = in.getInt("textMenuLvl");
		AppMenu.currBook = in.getParcelable("currBook");
		AppMenu.currChaps = in.getParcelableArrayList("currChaps");
		AppMenu.currChapInts = in.getIntegerArrayList("currChapInts");
		AppMenu.engItems = in.getStringArrayList("engItems");
		AppMenu.currChapStrings = in.getStringArray("currChapStrings");
		AppMenu.bidMap = (HashMap<String,String>) in.getSerializable("bidMap");
		AppMenu.customFontId = in.getInt("customFontId");
		AppMenu.displayLang = in.getInt("displayLang");
		AppMenu.topMenuIndex = in.getInt("topMenuIndex");
		AppMenu.currIntPath = in.getIntegerArrayList("currIntPath");
		//recover JsonNodes!
		AppMenu.recoverJsonNodes();
	}


}
