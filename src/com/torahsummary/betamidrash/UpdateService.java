package com.torahsummary.betamidrash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class UpdateService extends Service {
	public static final int UPDATE_STAGE_1_COMPLETE = 0;
	public static final int UPDATE_STAGE_2_COMPLETE = 1;
	public static final int UPDATE_STAGE_3_COMPLETE = 2;
	
	public static final int NOTIFICATION_ID = 613267;

	public static final String DATABASE_ZIP_DOWNLOAD_LOC = Downloader.FULL_DOWNLOAD_PATH + Database2.DB_NAME + ".zip";
	public static final String INDEX_DOWNLOAD_LOC = Downloader.FULL_DOWNLOAD_PATH + Downloader.INDEX_JSON_NAME;

	private static final String LAST_ASK_TO_UPGRADE_TIME = "LAST_ASK_TIME_UPGADE_TIME";


	//these two vars are stupid, but work. they are persistent vars which I need for checking version num. before and after this, they serve no purpose
	public static int updatedVersionNum;
	public static int currentVersionNum;
	public static boolean userInitiated;
	public static boolean inUpdateStage3 = false;
	private static long startedUpdateTime = 0;

	private static Intent intentYo;
	private static Service serviceYo;
	private static PowerManager.WakeLock powerLock;
	private static WifiManager.WifiLock wifiLock;
	
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		Downloader.init(this);

		
		
		
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock= wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wifiTag");
		wifiLock.acquire();
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		powerLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "powerTag");
		powerLock.acquire();
		
		intentYo = intent;
		serviceYo = this;
		
		boolean isPre = intent.getBooleanExtra("isPre", false);
		boolean userInit = intent.getBooleanExtra("userInit", false);
		
		

		if (isPre) {
			preupdateLibrary(MyApp.currActivityContext, userInit);
		} else {
			updateLibrary(MyApp.currActivityContext,userInit);
		}

		Log.d("bs","HEREERERER userInit: " + userInit);
		
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;


	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		

		
		Downloader.unregisterDownloader(this);
		endService();
	}
	
	//check internet status before update, and if necessary, inform user of problems
	public static void preupdateLibrary(Activity context, boolean userInit) {


		int netStat = Downloader.getNetworkStatus();
		if (netStat == Downloader.NO_INTERNET) {
			DialogManager.showDialog(DialogManager.NO_INTERNET);
		} else if (netStat == Downloader.DATA_CONNECTED) {
			DialogManager.showDialog(DialogManager.USING_DATA);
		} else if (netStat == Downloader.WIFI_CONNECTED) {
			updateLibrary(context,userInit);
		}		
	}
	
	//suppressing because I handle sdk check myself
	@SuppressLint("NewApi")
	public static void updateLibrary(Activity context, boolean userInit) {
		
		//Toast.makeText(context, "Start background update", Toast.LENGTH_SHORT).show();
		//Toast.makeText(context, "background update only headers.", Toast.LENGTH_SHORT).show();
		MyApp.sendEvent("Download", "getting_update_csv");
		startedUpdateTime = System.currentTimeMillis();
		lockOrientation(context);
		
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		Notification.Builder notBuild = new Notification.Builder(serviceYo)
	     .setTicker("Updating Beta Midrash")
	     .setSmallIcon(R.drawable.beta_icon_noti)
	     .setWhen(System.currentTimeMillis())
	     .setContentTitle("Updating Beta Midrash")
	     .setContentText("Library downloading and installing");
		
		
		Notification notification;
		if (SDK_INT >= 11 && SDK_INT <= 15) notification = notBuild.getNotification();
		else notification = notBuild.build();
		
		Intent notificationIntent = new Intent(serviceYo, UpdateService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(serviceYo, 0, notificationIntent, 0);
		serviceYo.startForeground(NOTIFICATION_ID, notification);
		
		//save last check time
		SharedPreferences settings = context.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putLong("lastUpdateTime", System.currentTimeMillis());
		editor.apply();

		MyApp.currActivityContext = context;
		userInitiated = userInit;
		currentVersionNum = settings.getInt("versionNum", -1);

		
		new Thread(new Runnable() {
			public void run() {
					updateStage1();
				};
		}).start();
		

	}

	//this stage downloads csv, checks version and if that checks out, passes the url to updateStage2()
	public static void updateStage1() {
		//if (userInitiated) Toast.makeText(contextYo, "Library Download Started", Toast.LENGTH_LONG).show();
		File csvFile = new File(Downloader.FULL_DOWNLOAD_PATH + Downloader.CSV_FILE_NAME);
		if (csvFile.exists()) csvFile.delete();

		SharedPreferences settings = MyApp.currActivityContext.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		String csvURL = settings.getString("csvURL", Downloader.CSV_REAL_URL);
		Downloader.download(csvURL,Downloader.CSV_DOWNLOAD_TITLE,Downloader.DB_DOWNLOAD_PATH,Downloader.CSV_FILE_NAME,true);
	}


	public static void postUpdateStage1() {
		File csvFile = new File(Downloader.FULL_DOWNLOAD_PATH + Downloader.CSV_FILE_NAME);
		BufferedReader csvReader;
		try {
			csvReader = new BufferedReader(new FileReader(csvFile));
			String[] firstLine = csvReader.readLine().split(",");
			int dbVersion = Integer.parseInt(firstLine[0]);
			String zipUrl = firstLine[1];
			String indexURL = firstLine[2];
			int newestAppVersionNum  = Integer.parseInt(firstLine[3]);

			updatedVersionNum = dbVersion; //save this for later
			csvReader.close();
			//cover your tracks
			csvFile.delete();

			if(updatedVersionNum == MyApp.KILL_SWITCH_NUM){ 
				MyApp.killSwitch();
				if(userInitiated)
					DialogManager.dismissCurrentDialog(); //dismiss progressDialog
				unlockOrientation(MyApp.currActivityContext);
				return;
			}

			if((newestAppVersionNum > MyApp.context.getPackageManager().getPackageInfo(MyApp.appPackageName, 0).versionCode)
					&& !MyApp.askedForUpgradeThisTime
					){
				Toast.makeText(MyApp.currActivityContext, MyApp.currActivityContext.getString(R.string.upgrade_to_newest) + " " + MyApp.APP_NAME, Toast.LENGTH_SHORT).show();
				try {
					MyApp.currActivityContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MyApp.appPackageName)));
				} catch (android.content.ActivityNotFoundException anfe) {
					MyApp.currActivityContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + MyApp.appPackageName)));
				}
				MyApp.askedForUpgradeThisTime = true;
				if(userInitiated)
					DialogManager.dismissCurrentDialog(); //dismiss progressDialog
				unlockOrientation(MyApp.currActivityContext);
				return;

			}//else just continue to check if there's an update

			//check versions before continuing
			if (updatedVersionNum > currentVersionNum && userInitiated) {
				DialogManager.dismissCurrentDialog();
				DialogManager.showDialog(DialogManager.UPDATE_STARTED);
				updateStage2(zipUrl, indexURL);
			} else if (updatedVersionNum > currentVersionNum && !userInitiated) {
				if (currentVersionNum == -1) DialogManager.showDialog(DialogManager.FIRST_UPDATE);
				else DialogManager.showDialog(DialogManager.NEW_UPDATE);
			} else if (updatedVersionNum <= currentVersionNum && userInitiated) {
				DialogManager.dismissCurrentDialog(); //dismiss progressDialog
				DialogManager.showDialog(DialogManager.NO_NEW_UPDATE);
			} else {
				//no new update and not user initiated
				unlockOrientation(MyApp.currActivityContext);
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
			MyApp.sendException(ioe, "postUpdateStage1_ioe");
			return;
		}catch (Exception e){
			e.printStackTrace();
			MyApp.sendException(e, "postUpdateStage1");
			return;
		}
	}

	//...which uses it download the zip. Once done, it goes to updateStage3()
	public static void updateStage2(String zipUrl, String indexURL) {
		MyApp.sendEvent("Download", "updateStage2 - Starting DB download");

		//delete any file with the same name to avoid confusion
		File testFile = new File(DATABASE_ZIP_DOWNLOAD_LOC);
		if (testFile.exists()) testFile.delete();

		File testIndexFile = new File(INDEX_DOWNLOAD_LOC);
		if (testIndexFile.exists()) testIndexFile.delete();
		Downloader.eitherDBorIndexFinished = false;
		Downloader.download(indexURL, Downloader.JSON_INDEX_TITLE, Downloader.DB_DOWNLOAD_PATH, Downloader.INDEX_JSON_NAME, false);			
		Downloader.download(zipUrl,Downloader.DB_DOWNLOAD_TITLE,Downloader.DB_DOWNLOAD_PATH,Database2.DB_NAME + ".zip",false);
		//this guy calls a complete handler in Downloader to inform us we're down and move on to stage2
	}

	//...which unzips update into the databse location.
	public static void updateStage3() {
		Log.d("up","stage 3 started");
		//if (!inUpdateStage3) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					inUpdateStage3 = true;
					try {
						Database2 myDbHelper;
						myDbHelper = new Database2(MyApp.currActivityContext);

						myDbHelper.getReadableDatabase();
						try {
							Database2.deleteDatabase();
							myDbHelper.unzipDatabase(DATABASE_ZIP_DOWNLOAD_LOC, Database2.DB_PATH);

							//move index.json file into right location
							Util.moveFile(Downloader.FULL_DOWNLOAD_PATH, Downloader.INDEX_JSON_NAME, MyApp.INTERNAL_FOLDER, AppMenu.JSONMenuPath);


							//success, set version num
							SharedPreferences settings = MyApp.currActivityContext.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
							Editor edit = settings.edit();
							edit.putInt("versionNum", updatedVersionNum);
							edit.apply();

							Thread.sleep(200);


						} catch (IOException ioe) { //maybe add more exception handling
							MyApp.sendException(ioe, "updateStage3. THROWING");
							throw ioe;
						} /*catch (Exception	e){
							Toast.makeText(contextYo,e.getMessage(),Toast.LENGTH_LONG).show();
							MyApp.sendException(e, "updateStage3. THROWING");
							throw new Error("Unable to create database");
						}*/
						//testing...
						//Text.getAllTextsFromDB2();
						myDbHelper.close();

						//clean up by deleting zip that you downloaded...
						//File updateFile = new File(DATABASE_ZIP_DOWNLOAD_LOC);
						//if (updateFile.exists()) updateFile.delete();
						Util.deleteNonRecursiveDir(Downloader.FULL_DOWNLOAD_PATH);
								
						long timeToCompleteUpdate = System.currentTimeMillis() - startedUpdateTime;
						if(startedUpdateTime != 0 && timeToCompleteUpdate > 0)
							MyApp.sendEvent("Download", "Update Finished",timeToCompleteUpdate);

						handler.sendEmptyMessage(UPDATE_STAGE_3_COMPLETE);


					} catch (Exception e) {
						MyApp.sendException(e,"updateStage3. THROWING");
						throw new Error(e);
					}
					
				}
			}).start();
			
		//} 

	}

	public static Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_STAGE_1_COMPLETE:
				postUpdateStage1();
				break;
			case UPDATE_STAGE_2_COMPLETE:
				//let's move on to stage 2
				//Toast.makeText(contextYo, "Download complete. Installing...", Toast.LENGTH_SHORT).show();
				updateStage3();
				break;
			case UPDATE_STAGE_3_COMPLETE:
				endService();

				restart();


				break;
			case Downloader.INTERNET_LOST:
				endService();
				DialogManager.showDialog(DialogManager.DL_INTERNET_LOST);
				break;
			case Downloader.NOT_ENOUGH_SPACE:
				endService();
				DialogManager.showDialog(DialogManager.DL_NOT_ENOUGH_SPACE);
				break;
			case Downloader.UNKNOWN_ERROR:
				endService();
				DialogManager.showDialog(DialogManager.DL_UNKNOWN_ERROR);
				break;
			}
		}
	};


	public static void lockOrientation(Activity ac) {

		if(ac.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	public static void unlockOrientation(Activity ac) {
		ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	public static void restart() {
		inUpdateStage3 = false;
		
		DialogManager.dismissCurrentDialog();
		AppMenu.inited = false;
		AppMenu.bidTreeInited = false; //this needs to be updated as well, but for some reason it causes an error if I do it here....
		//Toast.makeText(MyApp.currActivityContext, "Installation complete. Enjoy the Torah!", Toast.LENGTH_SHORT).show();
		unlockOrientation(MyApp.currActivityContext);
		//total restart. To be safe, restart so the database is readable.
		Intent mStartActivity = new Intent(MyApp.currActivityContext, MenuLevelActivity.class);
		int mPendingIntentId = 31415;
		PendingIntent mPendingIntent = PendingIntent.getActivity(MyApp.currActivityContext, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager mgr = (AlarmManager)MyApp.currActivityContext.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
		System.exit(0);
	}
	
	public static void endService() {
		try {
			serviceYo.stopForeground(true);
			UpdateReceiver.completeWakefulIntent(intentYo);
	        wifiLock.release();
	        powerLock.release();
		} catch( Exception e) {
			
		}
		
		

		
		String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) MyApp.context.getSystemService(ns);
        nMgr.cancel(UpdateService.NOTIFICATION_ID);
        
	}


}


