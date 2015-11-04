package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.torahsummary.betamidrash.API.APIException;
import com.torahsummary.betamidrash.R;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

public class TextActivity extends ListActivity {


	public static final int FILTER_COMPLETE = 613;
	public static final int CHANGE_BMS = 26;



	private VerseAdapter adapter;
	private int bid;
	private String bookTitle;
	private int[] levels;
	private List<Integer> chapList;
	private int currLevel;
	private boolean isLink; //true if you clicked a link to get to this page
	private int chapPos; //current position in chapList
	private String title;
	private AutofitTextView titleTV; //the textview right below the actionbar
	private static SharedPreferences bookmarkSettings;
	private boolean isBookmarked;
	private Menu menu;
	private SearchView sv;
	private ListView lv;
	private int currQueryInt;
	private List<Integer> listOfQueryFinds;

	private GestureDetectorCompat mDetector; 


	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		MyApp.currActivityContext = this;
		setContentView(R.layout.activity_texts);

		mDetector = new GestureDetectorCompat(this, new TextGestureDetector());
		
		lv = getListView();
		//actionBar.setBackgroundDrawable(new ColorDrawable(SettingsActivity.SEFARIA_GREEN));
		//gets rid of lines between list items
		this.getListView().setDivider(null);
		titleTV = (AutofitTextView) this.findViewById(R.id.textTitleTV);
		titleTV.setTypeface(AppMenu.customFont);

		LinearLayout titleView = (LinearLayout) this.findViewById(R.id.titleView);
		titleView.setOnClickListener(chapterTitleClick);
		setTitle("");

		//getListView().setOnTouchListener(yo);
		ViewGroup yo = (ViewGroup) findViewById(R.id.textRoot);
		
		ScrollView titleScroll = (ScrollView) this.findViewById(R.id.titleScroll);
		titleScroll.setOnClickListener(preventScrollViewProbHandler);
		TextActivity.this.findViewById(R.id.titleClose).setVisibility(View.GONE);

		bookmarkSettings = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);

		SharedPreferences tempSettings = this.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		boolean firstTime = tempSettings.getBoolean("firstTime", true);
		if (firstTime) {
			Editor edit = tempSettings.edit();
			edit.putBoolean("firstTime", false);
			edit.apply();
			Intent intent = new Intent(TextActivity.this,HelpFragmentActivity.class);
			intent.putExtra("firstTime", true);

			startActivity(intent);
		}

		//setContentView(R.layout.activity_texts);
		Intent intent = getIntent(); //check if there is intent
		bid = intent.getIntExtra("bid", 0);
		isLink = intent.getBooleanExtra("isLink", false);
		levels = intent.getIntArrayExtra("levels");

		Book book = new Book(bid);
		bookTitle = book.title;
		currLevel = book.wherePage-1;
		int[] tempLevels = levels.clone(); //deep copy

		for (int i = currLevel; i >= 0; i--) {
			tempLevels[i] = 0;
		}



		try {
			chapList = Text.getChaps(bid, tempLevels);
		} catch (APIException e) {
			chapList = new ArrayList<Integer>();
			Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
		}
		//Log.d("sup",chapList.toString());
		int currChap = levels[currLevel];
		Log.d("where","currChap: " + currChap);
		for (int i = 0; i < chapList.size(); i++) {
			if (chapList.get(i).equals(currChap)) {
				//we've found the corresponding pos in chapList

				chapPos = i;
				//Toast.makeText(this, "CHAPPOS = " + chapPos, Toast.LENGTH_SHORT).show();
				break;
			}
		}
		//Toast.makeText(this, "CURRCHAP = " + currChap, Toast.LENGTH_SHORT).show();




		getActionBar().setDisplayHomeAsUpEnabled(true);
		//Log.d("lv",Arrays.toString(levels));
		List<Text> textsList;
		try {
			textsList = Text.get(bid,levels);
		} catch (APIException e) {
			textsList = new ArrayList<Text>();
			Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
		}
		//int sum = 0;
		//for (int i = 0; i < yo.size(); i++) {
		//List<Text> tempLinks = Link.getLinkedTexts(yo.get(i), -1, 0);
		//sum += tempLinks.size();
		//}
		//Log.d("links", "sum = " + sum);
		//List<Text> tempChapLinks = Link.getLinkedChapTexts(yo.get(0), 1, -1, 0);
		//Log.d("links", "chapLinks = " + tempChapLinks.size());
		// 1= en, 2 = he, 3 = has both.
		int lang;
		ArrayList<Integer> linkPosList = new ArrayList<Integer>();
		ArrayList<Integer> linkOpenList = new ArrayList<Integer>();
		if (icicle != null) {
			lang = icicle.getInt("lang");
			linkPosList = icicle.getIntegerArrayList("linkPosList");
			linkOpenList = icicle.getIntegerArrayList("linkOpenList");
		}
		else lang = Text.getMaxLang(textsList,-1); //intent.getIntExtra("langs",0); 
		adapter = new VerseAdapter(this, R.layout.item_multiverse, textsList,lang,verseClick,prevClick,nextClick,linkTitleClick,linkPosList,linkOpenList,book); 
		setListAdapter(adapter);

		title = getCurrTitleString(levels, adapter.book, true);
		setTextTitle(title);


		//if you came here from a link, scroll to position that was linked to
		int linkedVerse = intent.getIntExtra("linkedVerse", 0);
		if (linkedVerse != 0) {
			List<Integer> tempVerses = new ArrayList<Integer>(); 
			try {
				tempVerses = Text.getChaps(bid, levels);
			} catch (APIException e) {
				// TODO Auto-generated catch block
				Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
			}
			int versePos = tempVerses.indexOf(linkedVerse);
			getListView().setSelection(versePos);
		}


		isBookmarked = bookmarkSettings.getBoolean(getCurrBookmarkString(levels,bookTitle), false);

		if (intent.getBooleanExtra("isSearch", false)) {
			Searching.findWordsInList(adapter.texts, intent.getStringExtra("searchQuery"), false, false);
		}
	}

	
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("lang", adapter.currLang);
		outState.putIntegerArrayList("linkPosList", adapter.linkPosList);
		outState.putIntegerArrayList("linkOpenList", adapter.linkOpenList);
		outState.putInt("currQueryInt", currQueryInt);
		outState.putIntegerArrayList("listOfQueryFinds",(ArrayList<Integer>) listOfQueryFinds);
	

		AppMenu.saveState(outState);
		AppMenu.restoredByActivity = 0;
		//Log.d("rest","Before: " + AppMenu.currBook.bid);
	}

	@Override
	protected void onRestoreInstanceState(Bundle in) {
		super.onRestoreInstanceState(in);
		if (in != null) {
			adapter.currLang = in.getInt("lang");
			adapter.linkPosList = in.getIntegerArrayList("linkPosList");
			adapter.linkOpenList = in.getIntegerArrayList("linkOpenList");
			currQueryInt = in.getInt("currQueryInt");
			listOfQueryFinds = in.getIntegerArrayList("listOfQueryFinds");
			AppMenu.restoreState(this,in);
			AppMenu.restoredByActivity = R.string.activity_text;
			
			titleTV = (AutofitTextView) this.findViewById(R.id.textTitleTV);
			titleTV.setTypeface(AppMenu.customFont);
			invalidateOptionsMenu();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		try {
			String yo = AppMenu.lastTitles.get(AppMenu.lastTitles.size()-1);
		} catch (Exception e) {
			AppMenu.init(this);
			goHome();
		}
		//Log.d("levels","Resuming");
		currLevel = adapter.book.wherePage - 1;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.menu = menu;
		getMenuInflater().inflate(R.menu.text, menu);
		//Toast.makeText(this, "CHANGE", Toast.LENGTH_LONG).show();
		if (adapter.availableLang == SettingsActivity.EN_CODE) {
			menu.findItem(R.id.action_en).setVisible(true);
			menu.findItem(R.id.action_he).setVisible(false);
			menu.findItem(R.id.action_enandhe).setVisible(false);
		}else if (adapter.availableLang == SettingsActivity.HE_CODE) {
			menu.findItem(R.id.action_en).setVisible(false);
			menu.findItem(R.id.action_he).setVisible(true);
			menu.findItem(R.id.action_enandhe).setVisible(false);
		} else {
			if (adapter.currLang == 1) {
				menu.findItem(R.id.action_enandhe).setVisible(false);
				menu.findItem(R.id.action_he).setVisible(false);
			} else if (adapter.currLang == 2) {
				menu.findItem(R.id.action_en).setVisible(false);
				menu.findItem(R.id.action_enandhe).setVisible(false);
			} else if (adapter.currLang == 3) {
				menu.findItem(R.id.action_he).setVisible(false);
				menu.findItem(R.id.action_en).setVisible(false);
			}
		}
		menu.findItem(R.id.action_findonpage).setVisible(true);

		menu.findItem(R.id.exitSearch).setVisible(false);
		menu.findItem(R.id.nextSearch).setVisible(false);
		menu.findItem(R.id.prevSearch).setVisible(false);

		menu.findItem(R.id.action_prevChap);
		menu.findItem(R.id.action_nextChap);

		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		sv = (SearchView) menu.findItem(R.id.action_findonpage).getActionView();
		sv.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		sv.setOnQueryTextListener(qTextListener);

		return true;
	}
	
	/*@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//super.onListItemClick(l, v, position, id);
		//l.smoothScrollToPosition(position); //doesn't work
		//l.smoothScrollToPosition(l.getChildAt(position).getTop());
		//l.setSelection(position); //kind of annoying
		//Log.d("lv","CLICK");


		if (adapter.hasVisibleLink(v)) adapter.removeLink(position,v,true);
		else {
			if (!adapter.isLoadingLinks) {
				adapter.insertLinkView1(position,v,false,true);
			}
		}
		//Toast.makeText(this, "scrolll: " + scY, Toast.LENGTH_SHORT).show();
	}*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == FILTER_COMPLETE) {
			adapter.filterMap = (HashMap<String,Boolean>)data.getSerializableExtra("filterMap");
			adapter.isCheckedGrid = (ArrayList<ArrayList<Boolean>>)data.getSerializableExtra("checkedGrid");
			//Log.d("fl",Arrays.toString(filterMap.keySet().toArray()));
		} else if (requestCode == CHANGE_BMS) {
			boolean isAdd = data.getBooleanExtra("isAdd", false);
			boolean isRemove = data.getBooleanExtra("isRemove", false);
			if (isAdd) {
				String tempBm = getCurrBookmarkString(levels,bookTitle);
				Editor bEdit = bookmarkSettings.edit();
				bEdit.putBoolean(tempBm, true);
				bEdit.apply();
				isBookmarked = true;
				invalidateOptionsMenu();
				Toast.makeText(this, getString(R.string.bookmark_added) + " " + title, Toast.LENGTH_SHORT).show();
			} else if (isRemove) {
				//reload because we don't know what you did
				isBookmarked = bookmarkSettings.getBoolean(getCurrBookmarkString(levels,bookTitle), false);
				invalidateOptionsMenu();

				//Toast.makeText(this, "Bookmark removed " + title, Toast.LENGTH_SHORT).show();
			}
		} 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (!adapter.isLoadingLinks) {
			switch(item.getItemId()) {
			case android.R.id.home:
				goBack();
				return true;

				//lang order is: he -> enandhe -> en ->
			case R.id.action_en:
				if (adapter.availableLang == SettingsActivity.EN_CODE) {
					//there is no other lang available...
					Toast.makeText(this, getString(R.string.no_other_lang), Toast.LENGTH_SHORT).show();
					return true;
				}
				adapter.changeLang(SettingsActivity.HE_CODE);
				item.setVisible(false);
				menu.findItem(R.id.action_he).setVisible(true);
				return true;
			case R.id.action_he:
				if (adapter.availableLang == SettingsActivity.HE_CODE) {
					//there is no other lang available...
					Toast.makeText(this, getString(R.string.no_other_lang), Toast.LENGTH_SHORT).show();
					return true;
				}
				adapter.changeLang(SettingsActivity.BI_CODE);
				menu.findItem(R.id.action_enandhe).setVisible(true);
				item.setVisible(false);
				return true;
			case R.id.action_enandhe:
				adapter.changeLang(SettingsActivity.EN_CODE);
				menu.findItem(R.id.action_en).setVisible(true);
				item.setVisible(false);
				return true;
			case R.id.action_view_bms:
				Intent intent2 = new Intent(TextActivity.this, BookmarksActivity.class);
				intent2.putExtra("isTextPage", true);
				intent2.putExtra("isBookmarked", isBookmarked);
				intent2.putExtra("bmTitle",getCurrBookmarkString(levels,bookTitle));
				startActivityForResult(intent2,CHANGE_BMS);
				return true;
			case R.id.action_sefariaLink:
				String packageInfoString = "";
				try {
					PackageInfo pInfo;
					pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					packageInfoString = "&app=" + MyApp.APP_NAME + "&appVersion=" + pInfo.versionName; 
				} catch (NameNotFoundException e) {
					MyApp.sendException(e);
					e.printStackTrace();
				}
				
				String urlTitle = bookTitle.replace(" ", "_");
				int[] tempLevels = levels.clone();
				for(int i = 0;i<adapter.book.wherePage-1;i++){
					tempLevels[i] = 0;
				}
				String lvlUrl = "." + Header.getTextLocationString(tempLevels, adapter.book)[2];//getLevelUrl();
				Log.d("url",lvlUrl);
				String url = "http://www.sefaria.org/" + urlTitle + lvlUrl + "?appID=" + MyApp.randomID + packageInfoString;
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
				return true;
			case R.id.action_prevChap:
				if (chapPos >= 1) {
					//close chap links if open
					View v = this.findViewById(R.id.titleView);
					handleChapterTitleClick(v,false);

					chapPos -= 1;
					levels[currLevel] = chapList.get(chapPos);
					title = getCurrTitleString(levels, adapter.book, true);
					setTextTitle(title);//getCurrTitleString(levels, adapter.book));
					List<Text> newTexts = new ArrayList<Text>(); 
					try {
						newTexts = Text.get(bid,levels);
					} catch (APIException e) {
						Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
					}
					adapter.changeChap(newTexts);
					adapter.changeLang(Text.getMaxLang(newTexts,adapter.currLang));
					isBookmarked = bookmarkSettings.getBoolean(getCurrBookmarkString(levels,bookTitle), false);
					//update for bookmarks
					invalidateOptionsMenu();
					//scroll back up to top of page
					getListView().setSelection(0);
				}
				//Toast.makeText(this, "" + chapPos, Toast.LENGTH_SHORT).show();
				return true;
			case R.id.action_nextChap:
				if (chapPos <= chapList.size()-2) {
					//close chap links if open
					View v = this.findViewById(R.id.titleView);
					handleChapterTitleClick(v,false);

					chapPos += 1;
					levels[currLevel] = chapList.get(chapPos);
					title = getCurrTitleString(levels, adapter.book, true);
					setTextTitle(title);//getCurrTitleString(levels, adapter.book));

					List<Text> newTexts = new ArrayList<Text>();
					try {
						newTexts = Text.get(bid, levels);
					} catch (APIException e) {
						Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
					}
					adapter.changeChap(newTexts);
					adapter.changeLang(Text.getMaxLang(newTexts,adapter.currLang));
					isBookmarked = bookmarkSettings.getBoolean(getCurrBookmarkString(levels,bookTitle), false);
					//update for bookmarks
					invalidateOptionsMenu();
					//scroll back up to top of page
					getListView().setSelection(0);
				}
				//Toast.makeText(this, "" + chapPos, Toast.LENGTH_SHORT).show();
				return true;

			case R.id.action_settings:
			Intent intent = new Intent(TextActivity.this, SettingsActivity.class);
				startActivity(intent);
				return true;
			case R.id.filter_links:
			Intent intent3 = new Intent(TextActivity.this, FilterLinksActivity.class);
				intent3.putExtra("dummyText", adapter.texts.get(0).tid);
				intent3.putExtra("checkedGrid", adapter.isCheckedGrid);
				intent3.putExtra("tidMax",adapter.texts.get(adapter.texts.size()-1).tid);
				startActivityForResult(intent3,FILTER_COMPLETE);
				return true;
			case R.id.action_help:
			Intent intent4 = new Intent(TextActivity.this,HelpFragmentActivity.class);
				startActivity(intent4);
				return true;
			case R.id.action_btn_home:
				goHome();
				return true;
			case R.id.action_findonpage:
				for (int i = 0; i < menu.size(); i++) {
					menu.getItem(i).setVisible(false);
				}
				menu.findItem(R.id.exitSearch).setVisible(true);
				menu.findItem(R.id.nextSearch).setVisible(true);
				menu.findItem(R.id.prevSearch).setVisible(true);			

				break;
			case R.id.exitSearch:

				exitSearch();
				break;
			case R.id.nextSearch:
				if (listOfQueryFinds != null)
					gotoSearchItem(true,false);
				break;
			case R.id.prevSearch:
				if (listOfQueryFinds != null) 
					gotoSearchItem(false,false);
				break;

			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		goBack();

		return;
	}

	private void goHome() {
		String[] menuItems = AppMenu.home();
		Intent intent = new Intent(TextActivity.this, MenuLevelActivity.class);
		intent.putExtra("menuItems", menuItems);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void goBack() {
		//TODO this doesn't work...
		if (menu.findItem(R.id.exitSearch).isVisible()) {
			exitSearch();
		} else {

			if (!isLink) {
				String[] menuItems = AppMenu.back();
				Intent intent = new Intent();
				intent.putExtra("menuItems", menuItems);
				setResult(MenuLevelActivity.RESULT_OK,intent);
			}
			finish();
			if (!isLink) overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
	}

	private void exitSearch() {

		//hide keyboard
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(sv.getWindowToken(), 0);
		menu.findItem(R.id.action_findonpage).collapseActionView();
		List<Text> newVerses = new ArrayList<Text>();
		try {
			newVerses = Text.get(adapter.book, levels);
		} catch (APIException e) {
			Toast.makeText(this,R.string.apiexception, Toast.LENGTH_SHORT).show();
		}
		adapter.texts = newVerses;
		adapter.clear();
		for (Text text:newVerses) {
			adapter.insert(text, adapter.getCount());
		}

		adapter.notifyDataSetChanged();

		invalidateOptionsMenu();
	}

	private String getLevelUrl() {
		String lvlUrl = "";
		for (int i = levels.length-1; i >= currLevel; i--) {
			lvlUrl = lvlUrl + '.' + levels[i];
		}
		return lvlUrl;
	}


	private String getCurrTitleString(int[] levels, Book book, boolean sendScreen) {
		int[] tempLevels = levels.clone();
		for(int i = 0;i<book.wherePage-1;i++){
			tempLevels[i] = 0;
		}
		String [] textLocation  = Header.getTextLocationString(tempLevels, book);
		
		String enTitle = book.title + " " + textLocation[0];
		if(sendScreen)
			MyApp.sendScreen(enTitle);
		SharedPreferences settings = getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		int menuLang = settings.getInt("menuLang", 0);
		if (menuLang == SettingsActivity.EN_CODE)
			return enTitle;
		else //if (adapter.currLang == SettingsActivity.HE_CODE)
			return book.heTitle + " " + textLocation[1];
	}

	//returns strings formatted so that they uniquely identify bookmark + a visually friendly version of the same bookmark
	private String getCurrBookmarkString(int[] levels, String title) {
		ArrayList<String> tempList = new ArrayList<String>();
		for (int i = levels.length-1; i >= 0; i--) {
			if (levels[i] != 0) {
				tempList.add(""+levels[i]);
			}

		}
		//notice the  __ used as a separator between visually friendly and bookmark friendly
		return this.title + "__" + title + " " + Util.joinArrayList(tempList,":");
	}

	private void handleChapterTitleClick(View v,boolean fromClickHandler) {
		if (adapter.hasVisibleLink(v)) {
			//remove chap links
			TextActivity.this.getListView().setVisibility(View.VISIBLE);

			TextActivity.this.findViewById(R.id.titleClose).setVisibility(View.GONE);
			adapter.removeLink(adapter.links.size()-1,v,false);
		}
		else if (fromClickHandler) {
			if (!adapter.isLoadingLinks) {
				//insert chap links
				TextActivity.this.getListView().setVisibility(View.GONE);
				TextActivity.this.findViewById(R.id.textRoot).setBackgroundColor(adapter.bgLinkColor);	 
				TextActivity.this.findViewById(R.id.titleClose).setVisibility(View.VISIBLE);
				adapter.insertLinkView1(adapter.links.size()-1,v,false,true);
			}
		}
	}

	private void setTextTitle(String title) {
		titleTV.setText(Html.fromHtml("<u>" + title + "</u>"));
	}

	private void gotoSearchItem(boolean isForward,boolean isInit) {
		if (listOfQueryFinds.size() > 0) {
			if (isInit) {
				currQueryInt = 0;
			} else if (isForward) {
				currQueryInt++;
				if (currQueryInt >= listOfQueryFinds.size()) {
					currQueryInt = 0;
				}

			} else { //isBackward
				currQueryInt--;
				if (currQueryInt < 0) {
					currQueryInt = listOfQueryFinds.size()-1;
				}
			}
			int pos = listOfQueryFinds.get(currQueryInt);
			getListView().setSelection(pos);
		}

	}
	
	public OnTouchListener verseClick = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (v.getParent() != (ViewParent)lv) {
				//this seems to happen when mono lingual sometimes.
				return true;
			}
			int position = lv.getPositionForView(v);
			adapter.handleVerseClick(v,event,position);

			return true;
		}
	};
	
	public OnClickListener prevClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int pos = getListView().getPositionForView(v);
			if (pos == -1) pos = adapter.links.size()-1;
			Integer linkOffset = adapter.linkPosList.get(pos);

			View parView = (ViewGroup)v.getParent().getParent();
			View parParView = (ViewGroup) parView.getParent();
			adapter.updateLink(parView, pos, linkOffset-1,parParView,true);

		}
	};

	public OnClickListener nextClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int pos = getListView().getPositionForView(v);
			if (pos == -1) pos = adapter.links.size()-1;
			Integer linkOffset = adapter.linkPosList.get(pos);

			View parView = (ViewGroup)v.getParent().getParent();
			View parParView = (ViewGroup)parView.getParent();
			adapter.updateLink(parView, pos, linkOffset+1,parParView,true);
		}
	};


	public OnClickListener chapterTitleClick = new OnClickListener() {

		@Override
		public void onClick(View v) {

			handleChapterTitleClick(v,true);
		}
	};

	public OnClickListener preventScrollViewProbHandler = new OnClickListener() {

		@Override
		public void onClick(View v) {
			//Log.d("lv","yooooo");
			v.getParent().requestDisallowInterceptTouchEvent(true);

		}
	};

	OnQueryTextListener qTextListener = new OnQueryTextListener() {

		@Override
		public boolean onQueryTextSubmit(String query) {
			try {
				adapter.texts = Text.get(bid,levels);
			} catch (APIException e) {
				Log.e("api", "APIException");
				adapter.texts = new ArrayList<Text>();
				Toast.makeText(MyApp.currActivityContext,R.string.apiexception, Toast.LENGTH_SHORT).show();
			}
			MyApp.sendEvent("FindOnPage",query);
			listOfQueryFinds = Searching.findWordsInList(adapter.texts, query, false, false);

			Toast.makeText(TextActivity.this,"" + listOfQueryFinds.size() + " " + getString(R.string.texts_contain) + " \"" + query + "\"", Toast.LENGTH_SHORT).show();
			adapter.notifyDataSetChanged();
			//hide keyboard
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(sv.getWindowToken(), 0);
			gotoSearchItem(true,true);
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText) {
			//no one cares...
			return false;
		}
	};

	/*public OnTouchListener yo = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			Toast.makeText(TextActivity.this, "yo", Toast.LENGTH_SHORT).show();
			return false;
		};
	};*/

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		/*switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
			//Toast.makeText(TextActivity.this, "touched", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.dispatchTouchEvent(ev);*/



		adapter.zoomZoom(event);
		this.mDetector.onTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}




	//go to link, actually, now it activates on clicking anywhere on the link
	public OnClickListener linkTitleClick = new OnClickListener() {

		@Override
		public void onClick(View v) {


			int pos = getListView().getPositionForView(v);
			Text linkedText;
			if (pos == -1) { //chap link
				pos = adapter.links.size()-1;
				Text currText = adapter.texts.get(0);
				Integer linkOffset = adapter.linkPosList.get(pos);
				linkedText = Link.getLinkedChapTexts(currText, 1, linkOffset).get(0);
			} else {
				Text currText = adapter.texts.get(pos);
				Integer linkOffset = adapter.linkPosList.get(pos);
				linkedText = Link.getLinkedTexts(currText,1,linkOffset).get(0);
			}


			Intent intent = new Intent(TextActivity.this,TextActivity.class);

			intent.putExtra("bid", linkedText.bid);
			Book linkedBook = new Book(linkedText.bid);	
			//Log.d("links","levels = " + Arrays.toString(linkedText.levels));
			int[] tempLevels = Arrays.copyOfRange(linkedText.levels, 0,linkedBook.textDepth);

			//I think this should work...
			if (linkedBook.textDepth > 1) {
				for(int i = 0; i <= linkedBook.wherePage-2 ; i++)
					tempLevels[i] = 0;
			}
			intent.putExtra("levels", tempLevels);


			intent.putExtra("title", linkedBook.title);	

			//the specific verse that the link links to. could be zero if there is none
			if (linkedBook.textDepth > 1)
				intent.putExtra("linkedVerse", linkedText.levels[0]);


			intent.putExtra("isLink", true);
			startActivity(intent);
		}
	};

	class TextGestureDetector extends GestureDetector.SimpleOnGestureListener {
		private static final String DEBUG_TAG = "Gestures"; 


		@Override
		public boolean onDown(MotionEvent event) { 
			//Log.d(DEBUG_TAG,"onDown: " + event.toString()); 
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2, 
				float velocityX, float velocityY) {
			//Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,float distanceX, float distanceY) {
			//Log.d("touch","scroolllllll");
			return super.onScroll(e1, e2, distanceX, distanceY);
		}



	}
}
