package com.torahsummary.betamidrash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CacheDB extends SQLiteOpenHelper{

	private static CacheDB sInstance;

	public static String DB_NAME = "cacheDB.db";
	public static String DB_PATH = MyApp.INTERNAL_FOLDER + "databases/" ;//	MyApp.getStorageLocation(DB_NAME); 
	static int DB_VERSION = 1;
	private static final String TABLE_NAME = "Cache"; 

	private SQLiteDatabase myDataBase;

	private final Context myContext;

	/**
	 * Constructor
	 * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	 * @param context
	 */
	public CacheDB(Context context) {
		super(context, DB_PATH + DB_NAME, null, DB_VERSION);
		this.myContext = context;
	}	


	public static void deleteDatabase() {
		File oldDB = new File(DB_PATH + DB_NAME);
		if (oldDB.exists()) {
			Log.d("db","deleting");
			oldDB.delete();
		}		
	}
	
	
	public static CacheDB getInstance(Context context) {
		// Use the application context, which will ensure that you 
		// don't accidentally leak an Activity's context.
		// See this article for more information: http://bit.ly/6LRzfx
		if (sInstance == null) {
			sInstance = new CacheDB(context.getApplicationContext());
		}
		return sInstance;
	}

	public static boolean checkDataBase(){
		SQLiteDatabase checkDB = null;
		try{
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		}catch(Exception e){
			MyApp.sendException(e, "database does't exist");
			//database does't exist yet.
		}
		if(checkDB != null){
			checkDB.close();
		}
		return checkDB != null ? true : false;
	}
	
	public void openDataBase() throws SQLException{
		//Open the database
		String myPath = DB_PATH + DB_NAME ;
		myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	}

	@Override
	public synchronized void close() {

		if(myDataBase != null)
			myDataBase.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(
	      "create table " + TABLE_NAME +
	      "(url text primary key,data text,time int)"
		      );
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}


	public static void add() {
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getReadableDatabase();
		ContentValues values = new ContentValues();

		values.put("url", "testing");
		values.put("data", "words words words");
		values.put("time", "123");
		db.insert(TABLE_NAME, null, values);
		
	}

	// Add your public helper methods to access and get content from the database.
	// You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
	// to you to create adapters for your views.

}
