package com.torahsummary.betamidrash;



/*import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;*/


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.widget.Toast;


public class MyApp extends Application {
	//private static final String GOOGLE_AN_ID = "UA-59002633-2"; ///THIS IS IS ONLY FOR TESTING!!!
	private static final String GOOGLE_AN_ID = "UA-60342564-2"; //OLD: "UA-60342564-1"; //new: -2
	public static Context context;
	public static Activity currActivityContext;
	public static final String APP_NAME = "BetaMidrash";
	public static final String CATEGORY_NEW_TEXT = "Opened Text Page";
	public static final String BUTTON_PRESS = "Button Press"; 
	public static final String SETTING_CHANGE = "Setting Change"; 
	public static String randomID = null;
	public static String INTERNAL_FOLDER = "/data/data/com.torahsummary.betamidrash/";
	public static final int KILL_SWITCH_NUM = -247;
	public static String appPackageName = "com.torahsummary.betamidrash";
	public static boolean askedForUpgradeThisTime = false;
	//public static int MIN_WORKING_DB_VERSION = 1; //this doesn't yet need to be made, b/c we don't have a db that will break it...
	
	


	@Override public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		appPackageName = context.getPackageName();
		//INTERNAL_FOLDER = context.getFilesDir().getPath();
		getTracker();
	}
	
	public static void killSwitch(){
		Toast.makeText(MyApp.context, MyApp.context.getString(R.string.kill_switch_message), Toast.LENGTH_LONG).show();
		SharedPreferences settings = context.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		Editor edit = settings.edit();
		edit.putInt("versionNum", KILL_SWITCH_NUM);
		edit.apply();
		Database2.deleteDatabase();	
		String url = "http://www.torahsummary.com/other/app/ver1/discontinued/?id=" + MyApp.randomID;
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		MyApp.currActivityContext.startActivity(browserIntent);
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////////////
	//THESE FAKE FUNCTIONS SHOULD BE UNCOMMENETED WHEN NOT USING ANYLITICS
	public static void setTrackerID(){}
	synchronized void getTracker() {}  
	static public void sendEvent(String cat, String act, long value){}
	static public void sendEvent(String cat, String act){}
	static public void sendScreen(String screenName){}
	static public void sendException(Exception e){}
	static public void sendException(Exception e, String addedText){e.printStackTrace();}
	/////////////////////////////////////////////////////////////////////////////////////
	

	
 	////////////////////////////////////////////////////////////////////////////
 	// THESE FUNCTIONS SHOULD BE COMMENETED WHEN NOT USING ANYLITICS
 	// ALSO COMMENT IMPORTS
 	////////////////////////////////////////////////////////////////////////////
 	/* 
	public static Tracker tracker = null;

	public static void setTrackerID(){
		try{
			tracker.set("randomID", randomID);
			tracker.set("01", randomID);
		} catch (Exception e){
			e.printStackTrace();
			sendException(e);
		}
	}

	synchronized void getTracker() {
		if(tracker != null)
			return;
		try{
			
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			Tracker t = analytics.newTracker(GOOGLE_AN_ID);
			t.enableAdvertisingIdCollection(true);
			t.enableExceptionReporting(true);
			t.enableAutoActivityTracking(true);
			tracker = t;
			
			sendScreen("Started App");
			//Toast.makeText(context, "Made tracker.", Toast.LENGTH_SHORT).show();
		} catch (Exception e){
			e.printStackTrace();
			sendException(e);
		}
		return;
	}  

	static public void sendEvent(String cat, String act, long value){
		try{
			if(tracker == null){
				return;
			}
			tracker.send(new HitBuilders.EventBuilder()
			.setCategory(cat).setAction(act).setLabel(randomID)
			.setValue(value)
			.build());
		}catch(Exception e){
			e.printStackTrace();
			sendException(e);
		}
	}
	
	static public void sendEvent(String cat, String act){
		try{
			if(tracker == null){
				return;
			}
			tracker.send(new HitBuilders.EventBuilder()
			.setCategory(cat).setAction(act).setLabel(randomID)
			//.setValue(value)
			.build());
		}catch(Exception e){
			e.printStackTrace();
			sendException(e);
		}
	}

	static public void sendScreen(String screenName){
		sendEvent("Screen Change", screenName);
		// Set screen name.
		tracker.setScreenName(screenName);

		// Send a screen view.
		tracker.send(new HitBuilders.AppViewBuilder().build());

	}


	static public void sendException(Exception e){
		sendException(e,"");
	}

	static public void sendException(Exception e, String addedText){
		String reportText = "_" + addedText + ";" + e + ";" + e.getStackTrace()[0].toString();
		e.printStackTrace();
		//Sending toast might break the app if run from other thread
		//Toast.makeText(MyApp.currActivityContext,"" + reportText.length() + ". " + reportText , Toast.LENGTH_SHORT).show(); //TODO comment out b/f release

		tracker.send(new HitBuilders.ExceptionBuilder()
		.setDescription(reportText)
		.setFatal(false)
		.build());
		
	}*/
	
}
