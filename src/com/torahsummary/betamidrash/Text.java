package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;




import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class Text implements Parcelable {

	public Text(){
		//empty
	}

	public Text(Cursor cursor ){
		getFromCursor(cursor);
	}
	
	//ADDED NEW CONSTRUCTOR FOR API:
	public Text(String enText, String heText) {
		this.enText = enText;
		this.heText = heText;		
	}

	public Text(int tid) {
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getReadableDatabase();
		Cursor cursor = db.query(TABLE_TEXTS, null, "_id" + "=?",
				new String[] { String.valueOf(tid) }, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()){
			getFromCursor(cursor);
		}
		else
			tid = 0;
	}

	public static final int MAX_LEVELS = 6;
	public static final double BILINGUAL_THRESHOLD = 0.05; //percentage of text that is bilingual for us to default to bilingual 

	public static int textsUpdatedCount = 0;

	public static SQLiteStatement stmt;
	//public static SQLiteDatabase DB;

	public static final String Kbid = "bid";
	public static final String KenText = "enText";
	public static final String KheText = "heText";
	public static final String Klevel1 = "level1";
	public static final String Klevel2 = "level2";
	public static final String Klevel3 = "level3";
	public static final String Klevel4 = "level4";
	public static final String Klevel5 = "level5";
	public static final String Klevel6 = "level6";
	//public static final String Khid = Header.Khid;


	public static final String ORDER_BY_LEVELS = Klevel6 + ", " + Klevel5 + ", " + Klevel4 + ", " + Klevel3 + ", " + Klevel2 + ", " + Klevel1;



	public int tid;
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

	public static final String TABLE_TEXTS = "Texts";

	public void getFromCursor(Cursor cursor){
		tid = cursor.getInt(0);
		bid = cursor.getInt(1);
		enText = cursor.getString(2);
		heText = cursor.getString(3);
		level1 = cursor.getInt(4);
		level2 = cursor.getInt(5);
		level3 = cursor.getInt(6);
		level4 = cursor.getInt(7);
		level5 = cursor.getInt(8);
		level6 = cursor.getInt(9);
		displayNum = (cursor.getInt(10) != 0);


		if(enText== null)
			enText = "";
		if(heText == null)
			heText = "";

		levels = new int [] {level1, level2, level3,level4, level5, level6};


	}	



	public static List<Text> getAll() {
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		List<Text> textList = new ArrayList<Text>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + TABLE_TEXTS;

		SQLiteDatabase db = dbHandler.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				textList.add(new Text(cursor));
			} while (cursor.moveToNext());
		}

		//LOGING:
		//for(int i = 0; i < textList.size(); i++)
		//	textList.get(i).log();

		return textList;
	}

	public static List<Text> get(Book book, int[] levels) {

		if(book.textDepth != levels.length){
			Log.e("Error_sql", "wrong size of levels.");
			//return new ArrayList<Text>();
		}
		//else
		return get(book.bid, levels);
	}

	//TODO remove this function...
	public static List<Text> getAllTextsFromDB2() {
		Database2 dbHandler = new Database2(MyApp.context);
		List<Text> textList = new ArrayList<Text>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + "Texts";

		SQLiteDatabase db = dbHandler.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				textList.add(new Text(cursor));
			} while (cursor.moveToNext());
		}

		//LOGING:
		//for(int i = 0; i < textList.size(); i++)
		//	textList.get(i).log();

		return textList;
	}


	public static int max(int bid, int[] levels) {

		Database2 dbHandler = Database2.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getReadableDatabase();
		int nonZeroLevel;
		for(nonZeroLevel = 0; nonZeroLevel < levels.length; nonZeroLevel++){
			if(levels[nonZeroLevel] != 0)
				break;
		}
		String sql = "SELECT MAX(level" + String.valueOf(nonZeroLevel) + ") FROM "+ TABLE_TEXTS + fullWhere(bid, levels);
		//		String sql = "SELECT COUNT(DISTINCT level" + String.valueOf(nonZeroLevel) + ") FROM "+ TABLE_TEXTS + fullWhere(bid, levels);

		Cursor cursor = db.rawQuery(sql, null);
		if (cursor != null)
			cursor.moveToFirst();

		////////TODO maybe compare max to count
		return cursor.getInt(0);
	}


	/*
	public static void removeFoundWordsColoring(List<Text> list){
		Text text = null;
		for (int i = 0; i< list.size(); i++){
			text = list.get(i);
			text.enText = text.enText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
			text.heText = text.heText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
		}
		return;
	}
	 */

		public static List<Text> get(int bid, int[] levels) {

		/*//EXAMPLE:
		int[] levels = new {0, 12};			
		Text.get(1, levels); //get book bid 1 everything in chap 12.
		 */
		
		List<Text> textList = new ArrayList<Text>();
		//ADDED AS PER JOSH'S SAMPLE (ES):
		try {
		 
		 		//TEST FOR INSTANCE OF THIS SEFER AND PEREK IN DATABASE
		 	Database2 dbHandler = Database2.getInstance(MyApp.context);
			SQLiteDatabase db = dbHandler.getReadableDatabase();
			
			Cursor cursor = db.rawQuery("SELECT DISTINCT * FROM "+ TABLE_TEXTS +" " + fullWhere(bid, levels) + " ORDER BY " + orderBy(bid, levels), null);
			
			if (cursor.moveToFirst()) {
				do {
					// Adding  to list
					textList.add(new Text(cursor));
				} while (cursor.moveToNext());
			}
			
		  
		  }catch(Exception e){
			  e.printStackTrace();
		  	//return API.getTextsFromAPI(Book(bid).title,level) 
			  //API.getTextsFromAPI(Kbid, levels); // NEED TO CONVERT BID TO TITLE OF BOOK
			  textList = API.getTextsFromAPI(Book.getTitle(bid), levels);
		  }
		  
		return textList;
		
		//Cursor cursor1 = db.query(TABLE_TEXTS, null, whereStatement, whereArgs, null, null, orderBy);

		// looping through all rows and adding to list
		/*if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				textList.add(new Text(cursor));
			} while (cursor.moveToNext());
		}**/

		/*	//LOGING:
		for(int i = 0; i < textList.size(); i++)
			textList.get(i).log();
		 */
		//findWordsInList(textList, "the");
		//findWordsInList(textList,"ש");

		
	}


	public static Text makeDummyChapText(Text text){	
		return makeDummyChapText(text, (new Book(text.bid)).wherePage);
	}

	public static Text makeDummyChapText0(Text text){	
		return makeDummyChapText0(text, (new Book(text.bid)).wherePage);
	}

	public static Text makeDummyChapText(Text text, int wherePage){
		Text dummyChapText = deepCopy(text);
		//dummyChapText.log();

		if(wherePage > 1) //I really should have used arrays (that was dumb).
			dummyChapText.level1 = -1;
		if(wherePage > 2)
			dummyChapText.level2 = -1;
		if(wherePage > 3)
			dummyChapText.level3 = -1;
		if(wherePage > 4)
			dummyChapText.level4 = -1;
		if(wherePage > 5)
			dummyChapText.level5 = -1;
		if(wherePage > 6)
			dummyChapText.level6 = -1;

		//TODO check that it's correct to use ">" for all types of where pages.
		Log.d("sql_dummytext", "wherePage: " + wherePage);
		Log.d("sql", "TODO check that it's correct to use '>' for all types of where pages.");
		dummyChapText.log();

		return dummyChapText;
	}

	public static Text makeDummyChapText0(Text text, int wherePage){
		Text dummyChapText = deepCopy(text);
		//dummyChapText.log();

		if(wherePage > 1) //I really should have used arrays (that was dumb).
			dummyChapText.level1 = 0;
		if(wherePage > 2)
			dummyChapText.level2 = 0;
		if(wherePage > 3)
			dummyChapText.level3 = 0;
		if(wherePage > 4)
			dummyChapText.level4 = 0;
		if(wherePage > 5)
			dummyChapText.level5 = 0;
		if(wherePage > 6)
			dummyChapText.level6 = 0;

		//TODO check that it's correct to use ">" for all types of where pages.
		Log.d("sql_dummytext", "wherePage: " + wherePage);
		Log.d("sql", "TODO check that it's correct to use '>' for all types of where pages.");
		dummyChapText.log();

		return dummyChapText;
	}


	public static void searchAppLevel(String langType, String word, int how) {
		new Thread(new Runnable() {
			public void run() {
				try {
					int chunkSize = 5000;
					Database2 dbHandler = Database2.getInstance(MyApp.context);
					SQLiteDatabase db = dbHandler.getReadableDatabase();

					List<Text> textList = new ArrayList<Text>();

					Cursor cursor;
					Log.d("sql_textFind", "start");
					String sql = "select heText from Texts "+ 
							" WHERE _id >= " + 0  + " AND _id <" + chunkSize ;

					// +
					//" where (bid = 1 OR bid = 11 OR bid = 12 OR bid = 13 OR bid = 14 OR bid = 51 OR bid = 61 OR bid = 71 OR bid = 81 OR bid = 91 OR bid = 111 OR bid = 2 OR bid = 22 OR bid = 22 OR bid = 23 OR bid = 24 OR bid = 52 OR bid = 62 OR bid = 72 OR bid = 82 OR bid = 92 OR bid = 222 " +
					//"OR bid = 3 OR bid = 33 OR bid = 33 OR bid = 33 OR bid = 34 OR bid = 53 OR bid = 63 OR bid = 73 OR bid = 83 OR bid = 93 OR bid = 333 " +
					///"OR bid = 4 OR bid = 44 OR bid = 44 OR bid = 43 OR bid = 44 OR bid = 54 OR bid = 64 OR bid = 74 OR bid = 84 OR bid = 94 OR bid = 444 " +
					//"OR bid = 5 OR bid = 55 OR bid = 55 OR bid = 53 OR bid = 54 OR bid = 55 OR bid = 65 OR bid = 75 OR bid = 85 OR bid = 95 OR bid = 555 " +
					//"OR bid = 6 OR bid = 66 OR bid = 66 OR bid = 63 OR bid = 64 OR bid = 56 OR bid = 66 OR bid = 76 OR bid = 86 OR bid = 96 OR bid = 666 " +
					//"OR bid = 7 OR bid = 77 OR bid = 77 OR bid = 73 OR bid = 74 OR bid = 57 OR bid = 67 OR bid = 77 OR bid = 87 OR bid = 97 OR bid = 777 " +
					//"OR bid = 8 OR bid = 88 OR bid = 88 OR bid = 83 OR bid = 84 OR bid = 58 OR bid = 68 OR bid = 78 OR bid = 88 OR bid = 98 OR bid = 888 " +
					//")"
					;// AND enText like '%gems%' LIMIT 10";
					cursor = db.rawQuery(sql, null);
					// looping through all rows and adding to list
					int count = 0;
					int foundCount = 0;
					String yo = null;
					if (cursor.moveToFirst()) {
						do {

							yo = cursor.getString(0);
							if(yo == null)
								continue;
							count++;
							if(Util.getRemovedNikudString(yo).contains("\u05d1\u05d2")) 
								//if(yo.replaceAll("[\u0591-\u05C7]", "").contains("\u05d1\u05d2"))
								foundCount++;

							//textList.add(new Text(cursor));

						} while (cursor.moveToNext());
					}
					Log.d("sql_textFind", "end.." + "finished!!! " + foundCount + " ..."  + count);
					//LOGING:
					//for(int i = 0; i < textList.size(); i++)
					//	textList.get(i).log();
				}catch(Exception e){
					MyApp.sendException(e, "moving index.json");
				}
			}
		}).start();
		return;
	}

	


	public static void removeLang(SQLiteDatabase db, String title, String lang){
		ContentValues values = new ContentValues();
		values.putNull(convertLangToLangText(lang));
		db.update(TABLE_TEXTS, values,Kbid+ "=?", new String [] {String.valueOf(Book.getBid(title))});
		Log.d("sql_removed_lang", title + " removed " + lang);
	}

	public static ArrayList<Integer> getChaps(int bid, int[] levels) {

		 Database2 dbHandler = Database2.getInstance(MyApp.context);
	        SQLiteDatabase db = dbHandler.getReadableDatabase();

	        ArrayList<Integer> chapList = new ArrayList<Integer>();

	        int nonZeroLevel;
	        for(nonZeroLevel = 0; nonZeroLevel < levels.length; nonZeroLevel++){
	            if(levels[nonZeroLevel] != 0)
	                break;
	        }
	        String sql = "SELECT DISTINCT level" + nonZeroLevel + " FROM "+ TABLE_TEXTS +" " + fullWhere(bid, levels) + " ORDER BY "  + "level" + nonZeroLevel;

	        try{
	            Cursor cursor = db.rawQuery(sql, null);

	            // looping through all rows and adding to list
	            if (cursor.moveToFirst()) {
	                do {
	                    // Adding  to list
	chapList.add(Integer.valueOf((cursor.getInt(0))));
	                } while (cursor.moveToNext());
	            }
	        }catch(Exception e){
	            Toast.makeText(MyApp.context, "couldn't find Texts so just showing chap 1",Toast.LENGTH_SHORT).show();
	            chapList.add(1);//this actually needs a way of getting chap numbers.. Lets think about that for a bit
	        }

	        /*//LOGING:
	        for(int i = 0; i < chapList.size(); i++)
	            Log.d("sql_chapList" , chapList.get(i).toString());
	         */
	        return chapList;
	}

	private static String[] whereArgs(int bid, int[] levels){
		String [] whereArgs = new String [levels.length + 1];
		whereArgs[0] = String.valueOf(bid);
		for(int i = 0; i < levels.length; i++){
			if(!(levels[i] == 0)){
				whereArgs[i + 1] = String.valueOf(levels[i]); ///plus one, b/c the first is bid
			}
		}

		return whereArgs;
	}

	private static String whereClause(int bid, int[] levels){
		String whereStatement = Kbid + "=? ";
		for(int i = 0; i < levels.length; i++){
			if(!(levels[i] == 0)){
				whereStatement += " AND level" + String.valueOf(i + 1) + "=? ";
			}
		}
		return whereStatement;
	}

	private static String fullWhere(int bid, int[] levels){
		String fullWhere =  " WHERE " + Kbid + "= " + String.valueOf(bid);
		for(int i = 0; i < levels.length; i++){
			if(!(levels[i] == 0)){
				fullWhere +=  " AND level" + String.valueOf(i + 1) + "= " + String.valueOf(levels[i]);
			}
		}
		return fullWhere;
	}

	private static String orderBy(int bid, int[] levels){
		String orderBy = Klevel1;
		for(int i = 0; i < levels.length - 2; i++){
			orderBy = "level"+ String.valueOf(i + 2) + ", " + orderBy;
		}
		return orderBy;
	}

	public void log() {
		Log.d("sql-TEXTS", /*Ktid + ": " + tid + */" " + Kbid + ": " + bid + " " + "level: "  + level6 + "." + level5 + "." +level4 + "." + level3 + "."+ level2 + "." + level1 + " "+ KenText + ": " + enText + " " + KheText + ": " + heText); 


	}

	private static String convertLangToLangText(String lang){
		if(lang.equals("en"))
			return KenText;
		else if(lang.equals("he"))
			return  KheText;
		Log.e( "sql_text_convertLang", "Unknown lang");
		return "";
	}

	public static int add(SQLiteDatabase db1, JSONObject json) throws JSONException {
		return add(db1,json,false);
	}

	public static int add(SQLiteDatabase db1, JSONObject json, boolean shouldUpdate) throws JSONException {
		//DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);
		//DB = dbHandler.getWritableDatabase();
		//SQLiteDatabase db1 = dbHandler.getWritableDatabase();

		String langType = convertLangToLangText(json.getString("language"));
		String title = json.getString("title");
		Book book = new Book(title, db1);

		if(book.bid == 0){
			Log.e("sql_text_add" , "Don't have book: " + title);
			//TODO try to add book first.
			return -1; //fail
		}
		//Log.d("sql_text_add", "Adding " + title + " " + langType + " isUpdating?" + shouldUpdate + ".....");


		int [] it = new int[MAX_LEVELS + 1];
		int forLoopNum = 6;
		if(forLoopNum != MAX_LEVELS)
			Log.e("ERROR: ", "forLoopNum is not teh same as MAX_LEVELS");
		boolean [] skipThisLoop = new boolean[forLoopNum + 1];
		JSONArray [] jsonArray = new JSONArray[forLoopNum + 1];
		textsUpdatedCount = 0;

		jsonArray[forLoopNum] = (JSONArray) json.get("text");


		for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
			if(book.textDepth >= forLoopNum)
				try{
					jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
				}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
					continue;
				}
			else{
				jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
				skipThisLoop[forLoopNum] = true;
			}
			forLoopNum = 5;
			for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
				if(book.textDepth >= forLoopNum)
					try{
						jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
					}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
						continue;
					}
				else{
					jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
					skipThisLoop[forLoopNum] = true;
				}
				forLoopNum = 4;
				for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
					if(book.textDepth >= forLoopNum)
						try{
							jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
						}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
							continue;
						}
					else{
						jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
						skipThisLoop[forLoopNum] = true;
					}
					forLoopNum = 3;
					for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
						if(book.textDepth >= forLoopNum){
							try{
								jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
							}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
								continue;
							}

						}
						else{
							jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
							skipThisLoop[forLoopNum] = true;
						}
						forLoopNum = 2;
						for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
							if(book.textDepth >= forLoopNum){
								try{
									jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
								}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
									continue;
								}
							}
							else{
								jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
								skipThisLoop[forLoopNum] = true;
							}
							forLoopNum = 1;
							for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
								try{
									insertValues(db1, book, jsonArray[forLoopNum], langType, it, shouldUpdate);
								}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
									MyApp.sendException(e);
									continue;
								}
							}
							forLoopNum = 2;
						}
						forLoopNum = 3;
					}
					forLoopNum = 4;
				}
				forLoopNum = 5;
			}
			forLoopNum = 6;
		}
		return 1; //it worked
	}

	private static long  insertValues(SQLiteDatabase db1, Book book, JSONArray jsonLevel1, String langType, int [] it, boolean shouldUpdate) throws JSONException{
		String theText;
		try{
			theText = jsonLevel1.getString(it[1]);
			if(theText.length() < 1){ //this means that it's useless to try to add to the database.
				return -1;
			}
			if(theText.equals("0") && !(jsonLevel1.get(it[1]) instanceof String)) //it's a 0, meaning no text
				return -1;	
		}catch(Exception e){ //if there was a problem getting the text, then it probably wasn't text anyways so just leave the function.
			Log.e("sql_adding_text", "Problem adding text " + book.title + " it[1] = " + it[1]);
			Database2.textsFailedToUpload++;
			MyApp.sendException(e);
			return -1;
		}		
		try{
			ContentValues values = new ContentValues();

			values.put(Kbid, book.bid);
			values.put(langType,theText); //place the text
			int [] levels = new int [book.textDepth];
			for(int i = 1; i<= book.textDepth; i++){
				values.put("level" + String.valueOf(i), it[i] + 1 );
				levels[i-1] = it[i] + 1;
			}
			if(shouldUpdate){
				//if(1 != db1.insertWithOnConflict(TABLE_TEXTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)){ //
				if(0 == db1.update(TABLE_TEXTS, values, whereClause(book.bid,levels), whereArgs(book.bid, levels))){ //it didn't update any texts (presumably b/c it didn't exist yet)
					if(db1.insert(TABLE_TEXTS, null, values) == -1){//the insert was -1 means that it failed to insert.
						Database2.textsFailedToUpload++;
						Log.e("sql_add_text", "Failed to updated verse" + langType + fullWhere(book.bid, levels));
					}else
						Database2.textsUploaded++;
				}
				else// it updated the text.
					Database2.textsUploaded++;
				return 0;

			}else{
				if(db1.insert(TABLE_TEXTS, null, values) == -1 )
					Database2.textsFailedToUpload++;
				else
					Database2.textsUploaded++;
				return 0;
			}

		} catch (Exception e) {
			MyApp.sendException(e, "textInsertVal");
			String levels = new String();
			levels = "";
			for(int i = 1; i<= MAX_LEVELS; i++){
				levels += (it[i] + 1) + ".";
			}
			Log.e("sql_text_insertValues",book.title + " " + langType + " "+  levels + " failed to upload properly. " + e);
			//e.printStackTrace();
		}
		return 0;
	}
	//could be -1 in order to actually getMaxLang
	public static int getMaxLang(List<Text> texts, int usersLang) {
		int minLang = 0;
		int numBi = 0; //num bilingual texts
		int numEn = 0;
		int numHe = 0;
		for (Text t : texts) {
			if (t.enText != "" && t.heText != "") {
				numBi++;
			} else if (t.heText != "") {
				numHe++;
			} else if (t.enText != "") {
				numEn++;
			}
		}
		if (numBi/((double)texts.size()) > BILINGUAL_THRESHOLD) minLang = SettingsActivity.BI_CODE;
		else {
			if (numEn > numHe) minLang = SettingsActivity.EN_CODE;
			else minLang = SettingsActivity.HE_CODE;
		}
		//default to users lang
		if (usersLang != -1) {
			if (minLang == SettingsActivity.BI_CODE || minLang == usersLang) {
				//bc bilingual contains both langs
				return usersLang;
			}
		}
		return minLang;
	}

	public static int getMaxLang(Text text) {
		if (text.enText != "" && text.heText != "") return SettingsActivity.BI_CODE;
		else if (text.enText != "") return SettingsActivity.EN_CODE;
		else if (text.heText != "") return SettingsActivity.HE_CODE;
		else return 0;
	}

	public static Text deepCopy(Text text) {
		Text newText = new Text();
		newText.bid = text.bid;
		newText.enText = text.enText;
		newText.heText = text.heText;
		newText.level1 = text.level1;
		newText.level2 = text.level2;
		newText.level3 = text.level3;
		newText.level4 = text.level4;
		newText.level5 = text.level5;
		newText.level6 = text.level6;
		newText.levels = text.levels.clone();
		newText.tid    = text.tid;
		newText.displayNum = text.displayNum;
		return newText;
	}

	//PARCELABLE------------------------------------------------------------------------

	public static final Parcelable.Creator<Text> CREATOR
	= new Parcelable.Creator<Text>() {
		public Text createFromParcel(Parcel in) {
			return new Text(in);
		}

		public Text[] newArray(int size) {
			return new Text[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(tid);
		dest.writeInt(bid);
		dest.writeString(enText);
		dest.writeString(heText);
		dest.writeInt(level1);
		dest.writeInt(level2);
		dest.writeInt(level3);
		dest.writeInt(level4);
		dest.writeInt(level5);
		dest.writeInt(level6);
		dest.writeIntArray(levels);
		dest.writeInt(displayNum ? 1 : 0);		
	}

	private Text(Parcel in) {
		tid = in.readInt();
		bid = in.readInt();
		enText = in.readString();
		heText = in.readString();
		level1 = in.readInt();
		level2 = in.readInt();
		level3 = in.readInt();
		level4 = in.readInt();
		level5 = in.readInt();
		level6 = in.readInt();
		levels = in.createIntArray();
		displayNum = in.readInt() != 0;
	}
}