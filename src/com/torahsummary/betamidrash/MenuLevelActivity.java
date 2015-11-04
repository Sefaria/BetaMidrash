package com.torahsummary.betamidrash;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import com.torahsummary.betamidrash.R;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

//displays menu and deals with user input
public class MenuLevelActivity extends ListActivity {

	public static final int BACK_CODE = 100;
	public static final int SETTINGS_RESULT_CODE = 101;
	public static final String MAIN_D = "MainActivity";

	private MenuItemAdapter adapter;
	private ArrayList<String> currCheckedBms; //assuming you are in 'bookmark' mode, has the correctly formatted bookmark strings, ready to be saved
	//public static final DatabaseHandler DB = new DatabaseHandler(this);
	private SharedPreferences settings;
	private Menu menu;
	private AutoCompleteTextView actv;
	private static String[] savedMenuItems;

	public void onCreate(Bundle icicle) { 
		super.onCreate(icicle);
		setContentView(R.layout.activity_main); 
		MyApp.currActivityContext = this;
		
		if (icicle != null && (AppMenu.restoredByActivity == R.string.activity_main || AppMenu.restoredByActivity == 0)) {
			if (icicle.getInt("currLevel") > AppMenu.currLevel) {
				AppMenu.restoreState(this, icicle);
				AppMenu.restoredByActivity = R.string.activity_main;
			}
			Log.d("restore","icicle");
		} else {
			Log.d("restore","no icicle");
		}
		UpdateService.endService();
		//try{

		//}catch(Exception e){//just in case this is what was breaking it at startup.
		//	Log.e("trying yo del update fileds", "" +e);
		//	MyApp.sendException(e,"Trying to Delete upFiles");
		//}
		//ActionBar actionBar = getActionBar();
		//actionBar.setBackgroundDrawable(new ColorDrawable(0xFF357035)); //cool, sefaria green
		//this.getListView().setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
		this.getListView().setDividerHeight(2);

		Intent intent = getIntent(); //check if there is intent
		String[] menuItems = intent.getStringArrayExtra("menuItems");
		boolean inChapPage = intent.getBooleanExtra("inChapPage", false);

		ArrayList<String> tempMenuItems;
		if (menuItems != null) {
			tempMenuItems = new ArrayList<String>(Arrays.asList(menuItems));
			
		} else if (savedMenuItems != null) {
			tempMenuItems = new ArrayList<String>(Arrays.asList(savedMenuItems));
			savedMenuItems = null;
			Log.d("restore","savedMenuItems");
		} else if (icicle != null){
			try {
				tempMenuItems = new ArrayList<String>(icicle.getStringArrayList("menuItems"));
			} catch (NullPointerException e) {
				tempMenuItems = new ArrayList<String>(); //just to initialize so eclipse stops whining...
				AppMenu.inited = false; //force a reinit
			}
		
		} else { //last resort!
			tempMenuItems = new ArrayList<String>(); //just to initialize so eclipse stops whining...
			AppMenu.inited = false; //force a reinit
		}
		settings = getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		
		if(!Database2.checkDataBase())
			Database2.createAPIdb();

		
		if (!AppMenu.inited) { //you just started the app



			long IDLong = settings.getLong("randomID", 0);
			if(IDLong == 0){//randomID for this person is not yet defined

				Random random = new Random();
				IDLong  = random.nextLong(); //there's a really small chance that it's 0... we're going to ignore that.
				Editor editor = settings.edit();
				editor.putLong("randomID", IDLong);
				editor.apply();
			}
			MyApp.randomID = String.valueOf(IDLong); //place person's ID into the randomID feild
			MyApp.setTrackerID();

			AppMenu.displayLang = settings.getInt("menuLang", 0);
			if(AppMenu.displayLang == 0){
				int setMenuLang;
				if(Util.isSystemLangHe())//using bilang and has hebrew as language
					setMenuLang = SettingsActivity.HE_CODE;
				else
					setMenuLang = SettingsActivity.EN_CODE;
				Editor edit2 = settings.edit();
				edit2.putInt("menuLang",setMenuLang);
				edit2.apply();
				AppMenu.displayLang = setMenuLang;
			}

			int versionNum = settings.getInt("versionNum", -1);
			


			if (Downloader.getNetworkStatus() != Downloader.NO_INTERNET) {
				long lastUpdateTime = settings.getLong("lastUpdateTime", 0);
				long currTime = System.currentTimeMillis();
				long timeUntilNextUpdate = SettingsActivity.UPDATE_DELAY_TIME;
				if(currTime > lastUpdateTime + timeUntilNextUpdate || lastUpdateTime > currTime + 60000) {//it's been a while since last update (or something weird is happening with the time).
					Intent intent2 = new Intent(this,UpdateReceiver.class);
					intent2.putExtra("isPre",false);
					intent2.putExtra("userInit",false);
					sendBroadcast(intent2);
				}
				else{
					Util.deleteNonRecursiveDir(Downloader.FULL_DOWNLOAD_PATH); 
				}
			} else {
				if(versionNum == MyApp.KILL_SWITCH_NUM) MyApp.killSwitch();//if there's no Internet and the last time was a kill switch.
				if (versionNum == -1) {
					//you don't have internet to download first update. that's bad
					DialogManager.showDialog(DialogManager.FIRST_UPDATE_FAIL);
				}
			}
			tempMenuItems = new ArrayList<String>(Arrays.asList(AppMenu.init(this)));
		} 

		if (!AppMenu.bidTreeInited) {
			try {
				Log.d("up","bidTree");
				AppMenu.initBidTree();
			} catch (SQLiteException e) {
				Log.d("up","no tree");
				//boohoo. This probably happened because the user doesn't have a library
			}catch (Exception e1) {//JH added
				Log.d("up", "" + e1);
				MyApp.sendException(e1);
			}
		}

		if (AppMenu.currLevel != 0){
			//we are in a sub menu
			setTitle(AppMenu.title);
			//MyApp.sendScreen(AppMenu.title);	
			//Log.d("yo","setting title to " + AppMenu.title);
			getActionBar().setDisplayHomeAsUpEnabled(true); //turn on menu back button
			//Log.d(MAIN_D,Arrays.toString(menuItems));
		}
		//else MyApp.sendScreen("Home Page"); //AppMenu.currLevel == 0 which means in home screen



		adapter = new MenuItemAdapter(this,R.layout.item_menu,tempMenuItems,inChapPage);
		setListAdapter(adapter);

		actv = (AutoCompleteTextView) findViewById(R.id.actv);
		
		ArrayList<String> allBookNames = new ArrayList<String>(AppMenu.enBookNames);
		allBookNames.addAll(AppMenu.heBookNames);
		ArrayAdapter<String> autoComAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,allBookNames);
		actv.setAdapter(autoComAdapter);
		actv.setOnItemClickListener(autoComItemClick);
		actv.setOnFocusChangeListener(autoComFocus);
		actv.setOnEditorActionListener(autoComEnterClick);
		
		getListView().setOnTouchListener(listListener);
		findViewById(R.id.clearButton).setOnClickListener(clearClick);
		
		getListView().requestFocus();
		//hide keyboard
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(actv.getWindowToken(), 0);
		
		//set dropdown width to width of phone. 40 was chosen arbitrarily
		findViewById(R.id.root).measure(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
		actv.setDropDownWidth(findViewById(R.id.root).getMeasuredWidth()); //
		
		View root = findViewById(R.id.root);
		ViewTreeObserver viewTreeObserver = root.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
			viewTreeObserver
					.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

						@Override
						public void onGlobalLayout() {
							View root = MenuLevelActivity.this
									.findViewById(R.id.root);
							root.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
							actv.setDropDownWidth(root.getWidth());

						}
					});
		}
	}

	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		

		//outState.putStringArrayList("menuItems", adapter.items);
		//outState.putParcelableArrayList("chapItems",(ArrayList<Header>) adapter.chapItems);
		Object[] temp = adapter.items.toArray();
		savedMenuItems = Arrays.copyOf(temp, temp.length, String[].class);

		outState.putInt("updatedVersionNum",UpdateService.updatedVersionNum);
		outState.putInt("currentVersionNum", UpdateService.currentVersionNum);
		outState.putBoolean("userInitiated", UpdateService.userInitiated);
		//outState.putBoolean("inUpdateStage3",UpdateService.inUpdateStage3);
		outState.putBoolean("isShowingDialog",DialogManager.isShowingDialog);
		outState.putInt("currentDialog", DialogManager.currentDialog);
		AppMenu.saveState(outState);
		AppMenu.restoredByActivity = 0;
	}

	@Override
	protected void onRestoreInstanceState(Bundle in) {
		super.onRestoreInstanceState(in);

		UpdateService.updatedVersionNum = in.getInt("updatedVersionNum");
		UpdateService.currentVersionNum = in.getInt("currentVersionNum");
		UpdateService.userInitiated = in.getBoolean("userInitiated");
		//UpdateService.inUpdateStage3 = in.getBoolean("inUpdateStage3");
		SettingsActivity.contextYo = this;
		DialogManager.isShowingDialog = in.getBoolean("isShowingDialog");
		DialogManager.currentDialog = in.getInt("currentDialog");
		
		/*adapter.items = in.getStringArrayList("menuItems");
		adapter.chapItems = in.getParcelableArrayList("chapItems");
		adapter.clear();
		adapter.addAll(adapter.items);
		adapter.notifyDataSetChanged();
		if (AppMenu.restoredByActivity != 0 && AppMenu.restoredByActivity != R.string.activity_main) {
			Log.d("restore","TExTE Gd");
		} else { //some menu activity wants to restore. only restore if newest activity
			if (in.getInt("currLevel") > AppMenu.currLevel) {
				Log.d("restore", "high enough " + in.getInt("currLevel") + " > " + AppMenu.currLevel);
				Log.d("restore","hee hee it worked");
				AppMenu.restoreState(this,in);
				AppMenu.restoredByActivity = R.string.activity_main;
			} else {
				Log.d("restore", "not high enough " + in.getInt("currLevel") + " < " + AppMenu.currLevel);
			}
		}*/
		if(DialogManager.isShowingDialog) {
			if (DialogManager.currentDialog != -1) {
				int currDia = DialogManager.currentDialog;
				DialogManager.dismissCurrentDialog();
				//Log.d("dia","remaking dialog");
				DialogManager.showDialog(currDia);

			}
		} 
		//Log.d("up","restoring");
		/*File testFile = new File(UpdateService.DATABASE_ZIP_DOWNLOAD_LOC);
		if (testFile.exists()) {
			UpdateService.updateStage3();
		} else {
			UpdateService.restart();
		}*/

	}



	@Override
	public void onPause() {
		super.onPause();
		/*if(DialogManager.dialog != null && DialogManager.dialog.isShowing()) {
			DialogManager.dismissCurrentDialog();
		}*/
	}

	@Override
	public void onResume() {
		super.onResume();
		this.invalidateOptionsMenu();
		MyApp.currActivityContext = this;


		if(DialogManager.isShowingDialog) {
			if (DialogManager.currentDialog != -1) {
				int currDia = DialogManager.currentDialog;
				DialogManager.dismissCurrentDialog();
				//Log.d("up","remaking dialog");
				DialogManager.showDialog(currDia);

			}
		} 
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Log.d("up","destroyed");
		/*File testFile = new File(Downloader.FULL_DOWNLOAD_PATH + Downloader.CSV_FILE_NAME);
		if (testFile.exists()) testFile.delete();

		File testFile2 = new File(UpdateService.DATABASE_ZIP_DOWNLOAD_LOC);
		if (testFile2.exists()) testFile2.delete();*/
	}

	@Override
	protected void onStop() {	
		//Downloader.unregisterDownloader(this);
		super.onStop();
	}


	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		this.menu = menu;
		
		//for now
		menu.findItem(R.id.action_search).setVisible(false);
		
		if(AppMenu.displayLang == 2){//he
			menu.findItem(R.id.action_he).setVisible(false);
			menu.findItem(R.id.action_en).setVisible(true);

		}else{
			menu.findItem(R.id.action_he).setVisible(true);
			menu.findItem(R.id.action_en).setVisible(false);
		}
		
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//save menu position
		AppMenu.topMenuIndex = getListView().getFirstVisiblePosition();

		String item = (String) adapter.getItem(position);

		String[] children;
		if (AppMenu.inJsonMenu) {
			try {
				children = AppMenu.forward(item,position); //the position in the list is the menu item you want to display
			} catch (SQLiteException e) { 
				//the app has never been updated
				MyApp.sendException(e, "DB not downloaded");
				DialogManager.showDialog(DialogManager.LIBRARY_EMPTY);
				return;
			}
			catch (Exception e1) {
				Log.d("onListItemClick", "" + e1);
				MyApp.sendException(e1);
				return;
			}
		}
		else { //you're in text menu
			try {
				int chapNum = adapter.chapItems.get(position).chapNum;
				children = AppMenu.forward(item,chapNum-1); //the corresponding chapNum is what you want to display
			} catch (Exception e) {
				//Toast.makeText(this,"caught you!",Toast.LENGTH_SHORT).show();
				return;
			}
		}

		if (children.length > 0 && children[0].equals("__atBook__")) {

			actv.setText("");
			Intent intent = new Intent(MenuLevelActivity.this,TextActivity.class);

			intent.putExtra("bid", AppMenu.currBook.bid);
			intent.putExtra("levels", AppMenu.getLevels());
			intent.putExtra("currTextLevel", AppMenu.currLevel-AppMenu.textMenuLvl);
			intent.putExtra("title",AppMenu.title);
			intent.putExtra("langs", AppMenu.currBook.languages);
			startActivityForResult(intent, BACK_CODE);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		} else { //normal
			if (!AppMenu.inJsonMenu && AppMenu.currBook.bid == 0) {
				Toast.makeText(this, getString(R.string.book_not_available), Toast.LENGTH_SHORT).show();
				AppMenu.back();
				return;
			}
			//normal
			actv.setText("");
			Intent intent = new Intent(MenuLevelActivity.this, MenuLevelActivity.class);
			intent.putExtra("menuItems", children);
			intent.putExtra("inChapPage",!AppMenu.inJsonMenu);
			startActivityForResult(intent,BACK_CODE);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			//Log.d(MAIN_D,Arrays.toString(children));
		}
	}

	@Override
	public void onBackPressed() {
		if (AppMenu.currLevel == 0){
			super.onBackPressed();
			return;
		}
		String[] menuItems = AppMenu.back();

		Bundle bundle = new Bundle();
		bundle.putStringArray("menuItems", menuItems);
		savedMenuItems = menuItems;
		Intent intent = new Intent();
		intent.putExtras(bundle);
		setResult(BACK_CODE,intent); //not using this now...
		finish();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		return;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home://really the back button
			onBackPressed();
			return true;
		case R.id.action_btn_home:
			//real home button
			goHome();
			return true;
		case R.id.action_settings:
			Downloader.unregisterDownloader(this);


			Intent intent = new Intent(MenuLevelActivity.this, SettingsActivity.class);
			startActivityForResult(intent,SETTINGS_RESULT_CODE);
			return true;
		case R.id.action_view_bms:
		Intent intent2 = new Intent(MenuLevelActivity.this, BookmarksActivity.class);
			startActivity(intent2);
			return true;
		case R.id.action_help:
		Intent intent3 = new Intent(MenuLevelActivity.this,HelpFragmentActivity.class);
			startActivity(intent3);
			return true;
		case R.id.action_about:
		Intent intent4 = new Intent(MenuLevelActivity.this,AboutActivity.class);
			startActivity(intent4);
			return true;
			//		case R.id.action_add_bms:
			//			adapter.changeBmMode();
			//			invalidateOptionsMenu();
			//			Toast.makeText(this, "Use the checkmarks to toggle bookmarks. When you're done, click the save icon",Toast.LENGTH_LONG).show();
			//			return true;
			//		case R.id.action_save_bms:
			//			adapter.changeBmMode();
			//			AppMenu.saveBms();
			//			invalidateOptionsMenu();
			//			return true;
		case R.id.action_he:
			menu.findItem(R.id.action_en).setVisible(true);
			menu.findItem(R.id.action_he).setVisible(false);
			Editor edit = settings.edit();
			edit.putInt("menuLang", SettingsActivity.HE_CODE);
			edit.apply();
			adapter.changeLang(SettingsActivity.HE_CODE);
			return true;
		case R.id.action_en:
			menu.findItem(R.id.action_en).setVisible(false);
			menu.findItem(R.id.action_he).setVisible(true);
			Editor edit2 = settings.edit();
			edit2.putInt("menuLang", SettingsActivity.EN_CODE);
			edit2.apply();
			adapter.changeLang(SettingsActivity.EN_CODE);
			return true;
		case R.id.action_search:
			//Intent intent5 = new Intent(MenuLevelActivity.this,SearchActivity.class);
			//startActivity(intent5);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private void goHome() {
		AppMenu.topMenuIndex = 0;
		String[] menuItems = AppMenu.home();
		Intent intent = new Intent(MenuLevelActivity.this, MenuLevelActivity.class);
		intent.putExtra("menuItems", menuItems);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	//--------------
	//EVENT LISTENERS
	//---------------
	public void searchClick(View button) {
		//basically the same as autoComEnterClick
		String textEntered = String.valueOf(actv.getText());
		actv.setText("");
		Intent intent = new Intent(MenuLevelActivity.this,SearchActivity.class);
		intent.putExtra("query", textEntered);
		startActivity(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == BACK_CODE) {
			String[] menuItems;
			if (data != null) {
				Bundle bundle = data.getExtras();
				menuItems = bundle.getStringArray("menuItems");
			} else {
				menuItems = savedMenuItems;
			}
			//erase
			savedMenuItems = null;

			if (AppMenu.currLevel != 0){
				//we are in a sub menu
				//MyApp.sendScreen(AppMenu.title);
				setTitle(AppMenu.title);
				getActionBar().setDisplayHomeAsUpEnabled(true); //turn on menu back button
				//Log.d(MAIN_D,Arrays.toString(menuItems));
			}
			ArrayList<String> tempMenuItems = new ArrayList<String>(Arrays.asList(menuItems));
			adapter = new MenuItemAdapter(this,R.layout.item_menu,tempMenuItems,!AppMenu.inJsonMenu);
			setListAdapter(adapter);
			getListView().setSelection(AppMenu.topMenuIndex);

		} else if (requestCode == SETTINGS_RESULT_CODE) {
			if (data != null) {
				//you might have changed menuLang...update just in case
				adapter.changeLang(data.getIntExtra("menuLang", 0));
			}
		}
	}
	
	OnFocusChangeListener autoComFocus = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				findViewById(R.id.clearButton).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.clearButton).setVisibility(View.GONE);
			}
			
		}
	};
	
	OnItemClickListener autoComItemClick = new OnItemClickListener() {
		public void onItemClick(android.widget.AdapterView<?> parent, View v, int pos, long id) {
			String bookName = (String) ((TextView)v).getText();
			String[] children = AppMenu.jumpToBook(bookName);
			
			if (!AppMenu.inJsonMenu && AppMenu.currBook.bid == 0) {
				Toast.makeText(MenuLevelActivity.this, getString(R.string.book_not_available), Toast.LENGTH_SHORT).show();
				AppMenu.back();
				return;
			}
			
			actv.setText("");
			
			Intent intent = new Intent(MenuLevelActivity.this, MenuLevelActivity.class);
			intent.putExtra("menuItems", children);
			intent.putExtra("inChapPage",!AppMenu.inJsonMenu);
			startActivityForResult(intent,BACK_CODE);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		};
	};
	
	OnEditorActionListener autoComEnterClick = new OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				
				String textEntered = String.valueOf(((TextView)v).getText());
				actv.setText("");
				Intent intent = new Intent(MenuLevelActivity.this,SearchActivity.class);
				intent.putExtra("query", textEntered);
				startActivity(intent);
				return true;
			}
			return false;
		}
	};
	
	OnTouchListener listListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			getListView().requestFocus();
			//hide keyboard
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(actv.getWindowToken(), 0);
			return false;
		}
	};
	
	OnClickListener clearClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			actv.setText("");
			
		}
	};
	
	public void jewcerClick(View v) {
		String url = "http://www.jewcer.com/project/betamidrash";
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}
	
	//	public OnCheckedChangeListener cbListener = new OnCheckedChangeListener() {
	//
	//		@Override
	//		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	//			int pos = getListView().getPositionForView(buttonView);
	//			String tempBm;
	//			if (adapter.isChapPage) {
	//				tempBm = AppMenu.currBook.title + adapter.chapItems.get(pos).chapNum + "__" + Util.joinArrayList(AppMenu.prevLevels,"__");
	//			} else {
	//				tempBm = adapter.items.get(pos);
	//			}
	//			Toast.makeText(MenuLevelActivity.this,tempBm,Toast.LENGTH_SHORT).show();
	//
	//		}
	//	};

} 
