package com.torahsummary.betamidrash;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Book implements Parcelable {



	private static final int DEFAULT_WHERE_PAGE = 2;
	public Book(){
		//empty constructor
		wherePage = DEFAULT_WHERE_PAGE; //this is the default value for it.
	}
	public Book(Cursor cursor){
		wherePage = DEFAULT_WHERE_PAGE; //this is the default value for it.
		getFromCursor(cursor);

	}

	public Book(String title){
		wherePage = DEFAULT_WHERE_PAGE;
		get(title);
	}

	public Book(String title, SQLiteDatabase db){
		wherePage = DEFAULT_WHERE_PAGE;
		get(title, db);
	}


	public Book(int bid) {
		wherePage = DEFAULT_WHERE_PAGE;
		get(bid);
	}


	public int bid;
	public int commentsOn;
	public String  [] sectionNames;
	public String  [] sectionNamesL2B;
	public String  [] heSectionNamesL2B;
	public String [] categories;
	public int textDepth;
	public int wherePage;
	public String title;
	public String heTitle;
	public int languages;

	
	@Override
	public String toString() {		
		return "bid: " + bid  + " commentsOn: " + commentsOn + " sectionNames: " + sectionNames + " categories: "+ categories + 
				"textDepth: " + textDepth + "wherePage: " + wherePage +" title: "  + title + "heTitle: " + heTitle + Klanguages + languages;
	}
	
	public void log(){
		Log.d("sql_dis",toString());  
	}

	public static final String TABLE_BOOKS = "Books";
	public static final String KcommentsOn = "commentsOn";
	public static final String KsectionNames = "sectionNames";
	public static final String Kcategories = "categories";
	public static final String KtextDepth = "textDepth";
	public static final String KwherePage = "wherePage";
	public static final String Klengths = "lengths";
	public static final String Ktitle = "title";
	public static final String KheTitle = "heTitle";

	public static final String KdataVersion = "dataVersion";
	public static final String KversionTitle = "versionTitle";
	public static final String Kversions = "versions";
	public static final String Klanguages = "languages"; // en is 1, he is 2, both is 3.
	public static final String KheSectionNames = "heSectionNames"; // en is 1, he is 2, both is 3.


	public static void add(SQLiteDatabase db1, JSONObject json) throws JSONException {
		add(db1, json, false); // you want to add a completely new thing (without updating).
	}

	public static void add(SQLiteDatabase db, JSONObject json, boolean shouldUpdate) throws JSONException {
		//DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);
		//SQLiteDatabase db = dbHandler.getWritableDatabase();

		ContentValues values = new ContentValues();
		String title = json.getString(Ktitle);
		//values.put(KcommentsOn, json.getString(KcommentsOn)); // comments on is something that we will add on our own. 
		values.put(KsectionNames, json.getString(KsectionNames).replace("\"\"", "\"Section\"")); //replaced empty SectionsNames with the default name "Section"
		//	values.put(Kcategories, json.getString(Kcategories)); 
		values.put(KtextDepth, json.getInt(KtextDepth));
		if(json.getInt(KtextDepth) == 1)
			values.put(KwherePage, 1);
		//values.put(KwherePage, json.getString(KwherePage)); // wherePage is something that we will add on our own.
		//	values.put(Klengths, json.getString(Klengths));
		//TODO MAKE THESE VALUES WORK>>>>>>>>>!!!!!
		values.put(Ktitle, title);
		try{
			values.put(KheTitle,json.getString(KheTitle));
		}catch(JSONException e){
			MyApp.sendException(e);
			values.put(KheTitle,json.getString(Ktitle));		
		}

		String langString = json.getString("language");
		int lang = getNum(title, Klanguages) | returnLangNums(langString); //the updated langs value
		values.put(Klanguages, String.valueOf(lang));

		if(shouldUpdate){
			if(db.update(TABLE_BOOKS, values, Ktitle + "=?", new String [] {title}) == 1)
				;//Log.d("sql_add_book", "Updated book info: " + title + " " + langString);
			else
				Log.e("sql_add_book", "Couldn't update book info: " + title + " " + langString);
		}
		else{ //you are adding a new thing (not updating it).
			if(db.insert(TABLE_BOOKS, null, values) != -1) 
				;//Log.d("sql_add_book", "Added new book info: " + title  + " " + langString);
			else
				Log.e("sql_add_book", "Couldn't add book info: " + title + " " + langString);
		}
		//db.close(); // Closing database connection
	}

	public static int returnLangNums(String langString){
		int langs = 0;
		if(langString.equals("en"))
			langs = 1;
		else if(langString.equals("he"))
			langs = 2;
		return langs;

	}

	/*public static void remove(String title){
		DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getWritableDatabase();
		db.delete(TABLE_BOOKS, Ktitle + "=?", new String [] {title});
		db.close(); // Closing database connection
		Log.d("sql_remove_book", title + " removed.");
	}*/



	private void getFromCursor(Cursor cursor){
		try{
			bid = cursor.getInt(0);
			commentsOn = cursor.getInt(1);
			sectionNames = Util.str2strArray(cursor.getString(2));
			sectionNamesL2B = new String [sectionNames.length];
			for(int i = 0; i<sectionNames.length;i++)
				sectionNamesL2B[i] = sectionNames[sectionNames.length - i -1];
			categories = Util.str2strArray(cursor.getString(3));
			textDepth = cursor.getInt(4);
			wherePage = cursor.getInt(5);
			title = cursor.getString(7);
			heTitle = cursor.getString(8);
			languages = cursor.getInt(cursor.getColumnIndex(Klanguages));
			String [] heSectionNamesTemp = Util.str2strArray(cursor.getString(cursor.getColumnIndex(KheSectionNames)));
			heSectionNamesL2B = new String [heSectionNamesTemp.length];
			for(int i = 0; i<heSectionNamesTemp.length;i++)
				heSectionNamesL2B[i] = heSectionNamesTemp[heSectionNamesTemp.length - i -1];
		}
		catch(Exception e){
			MyApp.sendException(e);
			bid = 0;
			return;
		}

	}	


	public void get(String title){
		get(title, null);
	}

	public void get(String title, SQLiteDatabase db){
		if(db == null){
			Database2 dbHandler = Database2.getInstance();
			db = dbHandler.getReadableDatabase();
		}
		Cursor cursor = db.query(TABLE_BOOKS, null, Ktitle + "=?",
				new String[] { title }, null, null, null, null);


		if (cursor != null){
			cursor.moveToFirst();
			getFromCursor(cursor);
		}
		else
			bid = 0;
	}

	public void get(int bid){
		Database2 dbHandler = Database2.getInstance();
		SQLiteDatabase	db = dbHandler.getReadableDatabase();
		Cursor cursor = db.query(TABLE_BOOKS, null, "_id" + "=?",
				new String[] { String.valueOf(bid) }, null, null, null, null);

		if (cursor != null){
			cursor.moveToFirst();
			getFromCursor(cursor);
		}
		else
			bid = 0;


	}

	public static int getBid(String title){
		Database2 dbHandler = Database2.getInstance();
		SQLiteDatabase db = dbHandler.getReadableDatabase();
		return getBid(title, db);
	}

	public static int getBid(String title, SQLiteDatabase db){
		Cursor cursor = db.query(TABLE_BOOKS, new String [] {"_id"}, Ktitle + "=?",
				new String[] { title }, null, null, null, null);

		if (cursor != null){
			cursor.moveToFirst();
			try{
				return cursor.getInt(0);//the _id
			}catch(Exception e){
				MyApp.sendException(e, title);
				return 0; //I'm having a problem... I assume it means that this book isn't in the database.
			}

		}
		return 0;
	}

	public static String getTitle(int bid){
		Database2 dbHandler = Database2.getInstance();
		SQLiteDatabase db = dbHandler.getReadableDatabase();
		Cursor cursor = db.query(TABLE_BOOKS, new String [] {"title"},  "_id=?",
				new String[] { String.valueOf(bid) }, null, null, null, null);

		if (cursor != null){
			cursor.moveToFirst();
			try{
				return cursor.getString(0);//the title
			}catch(Exception e){
				MyApp.sendException(e, "" + bid);
				return ""; //I'm having a problem... I assume it means that this book isn't in the database.
			}

		}
		return "";
	}

	//I'm not sure if this function is ever used.
	private static int getNum(String title, String type){
		Database2 dbHandler = Database2.getInstance();
		SQLiteDatabase db = dbHandler.getReadableDatabase();

		Cursor cursor = db.query(TABLE_BOOKS, new String [] {type}, Ktitle + "=?",
				new String[] { title }, null, null, null, null);

		if (cursor != null){
			cursor.moveToFirst();
			try{
				return cursor.getInt(0);//the type you wanted
			}catch(Exception e){
				MyApp.sendException(e, title);
				return 0; //I'm having a problem... I assume it means that this book isn't in the database.
			}

		}
		return 0;
	}

	public static List<Book> getAll() {
		Database2 dbHandler = Database2.getInstance();
		List<Book> bookList = new ArrayList<Book>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_BOOKS;

		SQLiteDatabase db = dbHandler.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				bookList.add(new Book(cursor));
			} while (cursor.moveToNext());
		}

		/*  //LOGING:
	    for(int i = 0; i < bookList.size(); i++)
			bookList.get(i).log();
		 */
		return bookList;
	}
	
	public static ArrayList<String> getAllBookNames(boolean isHebrew) {
		Database2 dbHandler = Database2.getInstance();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_BOOKS;

		SQLiteDatabase db = dbHandler.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		ArrayList<String> bookNameList = new ArrayList<String>(cursor.getCount());
		if (cursor.moveToFirst()) {
			do {
				if (isHebrew) {
					
					bookNameList.add(Util.getRemovedNikudString(cursor.getString(8))); //8 is index of hebook title
				} else {
					bookNameList.add(cursor.getString(7)); //7 is index of enbook title
				}
			} while (cursor.moveToNext());
		}

		/*  //LOGING:
	    for(int i = 0; i < bookList.size(); i++)
			bookList.get(i).log();
		 */
		return bookNameList;
	}


	public static void getAllSectionNames(){
		List<Book> bookList = getAll();
		HashMap<String, Integer> secList = new HashMap<String, Integer>() ;
		for(int i = 0; i< bookList.size(); i++){
			for(int j = 0; j<bookList.get(i).sectionNames.length; j++){
				if(bookList.get(i).sectionNames[j].equals("Sheilta")) //TODO what is this doing here?
					Log.i("sql_sectionList", "Sheilta: " + bookList.get(i).title);
				secList.put(bookList.get(i).sectionNames[j], 1);
			}

		}
		Object []  keyList = secList.keySet().toArray();
		for(int i = 0;i< keyList.length; i++ ){
			Log.i("sql_sectionList", keyList[i].toString());

		}

	}

	//PARCELABLE------------------------------------------------------------------------

	public static final Parcelable.Creator<Book> CREATOR
	= new Parcelable.Creator<Book>() {
		public Book createFromParcel(Parcel in) {
			return new Book(in);
		}

		public Book[] newArray(int size) {
			return new Book[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(bid);
		dest.writeInt(commentsOn);		
		dest.writeStringArray(sectionNames);
		dest.writeStringArray(sectionNamesL2B);
		dest.writeStringArray(heSectionNamesL2B);
		dest.writeStringArray(categories);
		dest.writeInt(textDepth);
		dest.writeInt(wherePage);
		dest.writeString(title);
		dest.writeString(heTitle);
		dest.writeInt(languages);
	}
	
	private Book(Parcel in) {
		bid = in.readInt();
		commentsOn = in.readInt();
		sectionNames = in.createStringArray();
		sectionNamesL2B = in.createStringArray();
		heSectionNamesL2B = in.createStringArray();
		categories = in.createStringArray();
		
		textDepth = in.readInt();
		wherePage = in.readInt();
		title = in.readString();
		heTitle = in.readString();
		languages = in.readInt();
	}

}





/*	public void add() {
		DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);

	SQLiteDatabase db = dbHandler.getWritableDatabase();

	ContentValues values = new ContentValues();
	values.put(KcommentsOn, commentsOn); 
	values.put(KsectionNames, sectionNames);
	values.put(Kcategories, categories); 
	values.put(KtextDepth, textDepth); 
	values.put(KwherePage, wherePage);
	values.put(Klengths, lengths); 
	values.put(Ktitle, title); 
	values.put(KheTitle, heTitle); 

	// Inserting Row
	db.insert(TABLE_BOOKS, null, values);
	db.close(); // Closing database connection
}
 */

