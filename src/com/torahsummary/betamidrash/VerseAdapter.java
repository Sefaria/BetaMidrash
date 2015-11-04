package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VerseAdapter extends ArrayAdapter<Text> {
	public static final int LINK_LOAD_COMPLETE = 42;
	public static final int LINK_LOAD_FAILED = 613;
	public static final int NO_LINKS_MATCH_FILTER = 5775;
	public static final int MIN_FONT_SIZE = 10;
	public static final int MAX_FONT_SIZE = 50;
	
	public static final int TOUCH_TIMER_SLOP = 200; //ms

	Context context;
	public List<Text> texts;
	public boolean isLoadingLinks = false;
	public HashMap<String,Boolean> filterMap;
	public ArrayList<ArrayList<Boolean>> isCheckedGrid;
	public ArrayList<ArrayList<Text>> links; //the last item in this list is designated for chapter links
	public ArrayList<Integer> linkPosList; //a parallel list to texts, this contains position of links currently visible for a given text. if not visible, pos = -1
	private ArrayList<Integer> linkLangList;
	public ArrayList<Integer> linkOpenList; //true if currently open, false o/w
	public int currLang;
	private int menuLang;
	public int availableLang; //the most general lang combo available for this adapter
	private float fontSize;
	private int numLinks;
	private int bgColor;
	private int textColor;
	private int bilayout;
	public int bgLinkColor;
	private int savedLang;
	public Book book;
	public String bookTitle;
	public int currLevel;
	private boolean showNikud;
	private SharedPreferences settings;
	private OnSharedPreferenceChangeListener listener;
	private OnClickListener prevClickListener;
	private OnClickListener nextClickListener;
	private OnClickListener titleClickListener;
	private OnTouchListener verseClickListener;

	//event vars
	private static final int NONE = 0;
	private static final int ZOOM = 1;
	private float oldDist;
	private int mode;

	private float startTouchPosX;
	private float startTouchPosY;
	private long startTouchTime;
	private int swipeSlop;
	private boolean isSwipingHor = false;
	private boolean isSwipingVer = false;
	private boolean isTouchDown = false;
	private View eventView;
	private Timer touchTimer; //to get the verses to change background color at the right time
	private View touchedVerse;
	
	public VerseAdapter(Context context, int resource, List<Text> objects, int currLang, OnTouchListener verseClickListener, OnClickListener prevClickListener, OnClickListener nextClickListener, OnClickListener titleClickListener, ArrayList<Integer> linkPosList,ArrayList<Integer> linkOpenList, Book book) {
		super(context, resource, objects);
		this.context = context;
		
		swipeSlop = ViewConfiguration.get(MyApp.currActivityContext).getScaledTouchSlop();
		touchTimer = new Timer();
		
		this.texts = objects;
		//initialize links
		this.linkPosList = new ArrayList<Integer>();
		this.linkLangList = new ArrayList<Integer>();
		this.linkOpenList = new ArrayList<Integer>();
		this.links = new ArrayList<ArrayList<Text>>();
		if (linkPosList.size() == 0) {
			while (this.linkPosList.size() < this.texts.size() + 1) { //+1 for chapterlinks
				this.linkPosList.add(0);
				this.linkOpenList.add(0);
			}
		} else {
			this.linkPosList = linkPosList;
			this.linkOpenList = linkOpenList;
		}
		//as tempting as it is to initialize all three link lists at the same time, linkPosList is not always empty (bc of rotation stuffs)
		while (this.linkLangList.size() < this.texts.size() + 1) {
			this.linkLangList.add(0);
			this.links.add(new ArrayList<Text>());
		}

		this.verseClickListener = verseClickListener;
		this.prevClickListener = prevClickListener;
		this.nextClickListener = nextClickListener;
		this.titleClickListener = titleClickListener;

		this.availableLang = currLang;
		//Toast.makeText(context, "AVALLANG: " + this.availableLang, Toast.LENGTH_LONG).show();
		this.currLang = currLang;
		settings = context.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		this.menuLang = settings.getInt("menuLang",0);
		// Instance field for listener
		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				updateVerseSettings();
				notifyDataSetChanged();
			}
		};

		this.book = book;
		bookTitle = book.title;
		currLevel = book.wherePage-1;

		settings.registerOnSharedPreferenceChangeListener(listener);
		updateVerseSettings();
	}

	public void updateVerseSettings() {

		this.numLinks = settings.getInt("numLinks", SettingsActivity.DEF_NUM_LINKS);
		this.fontSize = settings.getFloat("fontSize", SettingsActivity.DEF_FONT_SIZE);
		this.bgColor = settings.getInt("bgColor", SettingsActivity.DEF_BG_COLOR);
		this.savedLang = settings.getInt("lang", SettingsActivity.DEF_LANG);
		this.showNikud = settings.getBoolean("showNikud", SettingsActivity.DEF_NIKUD_BOOL);
		this.bilayout = settings.getInt("bilayout", SettingsActivity.DEF_BILINGUAL_LAYOUT);

		if (bgColor == SettingsActivity.BG_BLACK) {
			textColor = SettingsActivity.BG_WHITE;
			bgLinkColor = SettingsActivity.BG_LINK_BLACK;
		}
		else if (bgColor == SettingsActivity.BG_WHITE) {
			textColor = SettingsActivity.BG_BLACK;
			bgLinkColor = SettingsActivity.BG_LINK_WHITE;
		}



		//if bilingual, use settings, else use the lang available
		if (availableLang == SettingsActivity.BI_CODE) this.currLang = savedLang;

		//set all views as unupdated, until they get updated by getView()
		//all Ints initialize to zero, if they're updated, set them to one
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Text verse = texts.get(position);
		View view = convertView;
		//check if convertView is a different lang than your current lang
		if (view == null || (view.findViewById(R.id.mono) == null && (currLang == 1 || currLang == 2)) 
				|| (view.findViewById(R.id.he) == null && (currLang == 3))
				|| (view.findViewById(R.id.verticalVerse) == null && (bilayout == SettingsActivity.BI_LAYOUT_VER))
				|| (view.findViewById(R.id.horizontalVerse) == null && (bilayout == SettingsActivity.BI_LAYOUT_HOR))) {

			LayoutInflater inflater = (LayoutInflater) 
					context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			if (currLang == 3) {
				if (bilayout == SettingsActivity.BI_LAYOUT_HOR) {
					view = inflater.inflate(R.layout.item_multiverse, null);
				} else if (bilayout == SettingsActivity.BI_LAYOUT_VER) {
					view = inflater.inflate(R.layout.item_multivertverse, null);
				}
			}
			else view = inflater.inflate(R.layout.item_monoverse, null);

			view.setOnTouchListener(verseClickListener);
		}
		view.setBackgroundColor(bgColor);

		if (currLang == SettingsActivity.BI_CODE) {
			TextView etv = (TextView) view.findViewById(R.id.en);
			TextView htv = (TextView) view.findViewById(R.id.he);
			//Toast.makeText(context, "" + Math.round(SettingsActivity.fontSize * SettingsActivity.EN_HE_RATIO), Toast.LENGTH_SHORT).show();
			etv.setTextSize((int)Math.round(fontSize * SettingsActivity.EN_HE_RATIO));
			htv.setTextSize(fontSize);
			etv.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
			htv.setLineSpacing(0, 1.3f);
			etv.setTextColor(textColor);
			htv.setTextColor(textColor);

			htv.setTypeface(AppMenu.customFont);
			//etv.setTypeface(Typeface.SERIF);
			etv.setTypeface(AppMenu.customFont);

			String enText = verse.enText;
			if (enText == "") enText = "(No text in English)";
			etv.setText(Html.fromHtml(getVerseNum(verse, false) + enText));

			//nikud check
			String hebText;
			if (showNikud) hebText = verse.heText;
			else hebText = Util.getRemovedNikudString(verse.heText);

			if (hebText == "") hebText = "\u05D0\u05D9\u05DF \u05E7\u05D8\u05E2 \u05D1\u05E2\u05D1\u05E8\u05D9\u05EA";
			if (verse.level1 == -1) Log.d("-1","sadface");
			htv.setText(Html.fromHtml(getVerseNum(verse,true) + hebText));
		} else {

			TextView mtv = (TextView) view.findViewById(R.id.mono);
			mtv.setTextColor(textColor);
			mtv.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
			mtv.setTypeface(AppMenu.customFont);
			if (currLang == 2) {
				mtv.setTextSize(fontSize);

				//nikud check
				String hebText;
				if (showNikud) hebText = verse.heText;
				else hebText = Util.getRemovedNikudString(verse.heText);

				if (hebText == "") hebText = "\u05D0\u05D9\u05DF \u05E7\u05D8\u05E2 \u05D1\u05E2\u05D1\u05E8\u05D9\u05EA";
				mtv.setText(Html.fromHtml(getVerseNum(verse,true) + hebText));
			} else if (currLang == 1) {
				mtv.setTextSize((int)Math.round(fontSize * SettingsActivity.EN_HE_RATIO));

				String enText = verse.enText;
				if (enText == "") enText = "(No text in English)";
				mtv.setText(Html.fromHtml(getVerseNum(verse, false) + enText));
			}
		}
		//Log.d("lv","BID = " + links.get(position).bid);
		//Log.d("lv","isVis = " + hasVisibleLink(view));

		//deal with links


		if (linkOpenList.get(position) == 0 && hasVisibleLink(view)) {
			//remove link that shouldn't be there
			removeLink(position,view,false);
			Log.d("lv","removed");
		} else if (linkOpenList.get(position) != 0 && !hasVisibleLink(view)) {
			//add link that should be there

			insertLinkView1(position, view,true,true);
			Log.d("lv","added");
		} else if (linkOpenList.get(position) != 0 && hasVisibleLink(view)){
			//update link that is already there
			//View lv = view.findViewById(R.id.link);
			//updateLink(lv, position,-1,view);
			removeLink(position,view,false);
			insertLinkView1(position,view,true,true);
			Log.d("lv","update");
		}


		//Log.d("yoo",verse.level1 + "");
		//ntv.setText("" + verse.level1);
		return view;
	}

	public void changeLang(int lang) {
		this.currLang = lang;
		//Toast.makeText(context, "LANG", Toast.LENGTH_SHORT).show();
		this.notifyDataSetChanged();

	}

	public void changeChap(List<Text> newVerses) {
		//reinitialize links
		this.linkPosList = new ArrayList<Integer>();
		this.linkLangList = new ArrayList<Integer>();
		this.linkOpenList = new ArrayList<Integer>();
		this.links = new ArrayList<ArrayList<Text>>();
		while (this.linkPosList.size() < newVerses.size() + 1) {
			this.linkPosList.add(0);
			this.linkLangList.add(0);
			this.linkOpenList.add(0);
			this.links.add(new ArrayList<Text>());
		}

		this.texts = newVerses;
		this.clear();
		for (Text text:newVerses) {
			this.insert(text, this.getCount());
		}
		this.availableLang = Text.getMaxLang(newVerses, -1);

		this.notifyDataSetChanged();
	}

	public void setFontSize(float newFontSize) {
		fontSize = newFontSize;
	}

	public float getFontSize() {
		return fontSize;
	}

	/*
	LINKS STUFF--------------------------------------------------------------------------------------------------------
	 */

	//reinsert is true when switching languages when updating link
	//because of thead, we need extra bool to tell us if we want to from updateLinkView after we finish loading
	public void insertLinkView1(int clickedPos, View inputView, boolean reinsert,boolean runUpdateAfterPart2) {
		isLoadingLinks = true;
		LayoutInflater layoutInflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		try {
			Text clickedLink = links.get(clickedPos).get(linkPosList.get(clickedPos));
			int offsetPos = getFilteredOffset(links.get(clickedPos), linkPosList.get(clickedPos));
			if (offsetPos == -1) {
				Toast.makeText(context, context.getString(R.string.no_match_filter), Toast.LENGTH_SHORT).show();
				isLoadingLinks = false;

				//convoluted way of accessing list view and x mark...
				((View)inputView.getParent().getParent()).findViewById(android.R.id.list).setVisibility(View.VISIBLE);
				((View)inputView.getParent().getParent()).findViewById(R.id.titleClose).setVisibility(View.GONE);
			} else {
				insertLinkView2(clickedPos, inputView, reinsert, runUpdateAfterPart2,clickedLink,layoutInflater);

			}
		} catch (Exception e) {
			//MyApp.sendException(e, "add temp linkloader");
			//add linkloader view temporarily
			int insertPos;
			if (currLang == 3 && clickedPos != links.size()-1) insertPos = 2;
			else if (currLang !=3 && clickedPos != links.size()-1) insertPos = 1;
			else {
				inputView = inputView.findViewById(R.id.titleScroll);
				insertPos = 0;
			}
			View loaderView = layoutInflater.inflate(R.layout.item_linkloader, null);
			((ViewGroup) inputView).addView(loaderView, insertPos, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			loadLinks(clickedPos,inputView,reinsert,runUpdateAfterPart2,layoutInflater);
		}




		return;
		//int count = ((ViewGroup)inputView.findViewById(R.id.link_monoverse)).getChildCount();
		//Log.d("lv","child count = " + count);
	}

	private void insertLinkView2(int clickedPos, View inputView, boolean reinsert,boolean runUpdateAfterPart2,Text clickedLink, LayoutInflater layoutInflater) {

		View v;

		int linkLang = linkLangList.get(clickedPos);
		if (!reinsert) linkLang = Text.getMaxLang(clickedLink); //(new Book(clickedLink.bid)).languages;



		//Log.d("lv","Link Lang: " + linkLang);
		if (linkLang == 3) {
			if (bilayout == SettingsActivity.BI_LAYOUT_HOR) v = layoutInflater.inflate(R.layout.link_multiverse,null);
			else /*if (bilayout == R.id.bilayoutVerRB)*/ v = layoutInflater.inflate(R.layout.link_multivertverse, null);
		}
		else v = layoutInflater.inflate(R.layout.link_monoverse, null);

		Button prevBtn = (Button) v.findViewById(R.id.prevLinkBtn);
		Button nextBtn = (Button) v.findViewById(R.id.nextLinkBtn);
		TextView titleTV = (TextView) v.findViewById(R.id.linkTitleTV);
		prevBtn.setOnClickListener(prevClickListener);
		nextBtn.setOnClickListener(nextClickListener);
		titleTV.setOnClickListener(titleClickListener);
		//v.setOnClickListener(titleClickListener);

		//opening for first time
		if (linkOpenList.get(clickedPos) == 0) {
			//Log.d("run","run");
			linkLangList.set(clickedPos, linkLang);

		}
		linkOpenList.set(clickedPos, 1);




		int insertPos;
		//insertPos = 1 for chapter links
		if (currLang == 3 && clickedPos != links.size()-1) insertPos = 2;
		else if (currLang != 3 && clickedPos != links.size()-1) insertPos = 1;
		else {
			inputView = inputView.findViewById(R.id.titleScroll);
			insertPos = 0;
		}
		((ViewGroup) inputView).addView(v, insertPos, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		if (!reinsert) {
			try {
				ListView lv = ((ListView)inputView.getParent());
				int h1 = lv.getHeight();
				int h2 = inputView.getHeight();
				lv.smoothScrollToPositionFromTop(clickedPos, h1/2 - h2, 300);
			} catch (Exception e) {
				MyApp.sendException(e,"clicked chap link");
				//you must have clicked a chapter link...
			}
		}


		//I believe !reinsert = newUpdate always
		if (runUpdateAfterPart2)
			updateLink(v,clickedPos,linkPosList.get(clickedPos),inputView,!reinsert);

		isLoadingLinks = false;

	}

	public void removeLink(int clickedPos, View view, boolean totalRemoval) {

		if (clickedPos != links.size()-1) {
			((ViewGroup) view).removeView(view.findViewById(R.id.link));
		} else {
			//title chaps
			Log.d("lv","removed chap Link!");
			View scrollChildLayout = view.findViewById(R.id.link);
			((ViewGroup)scrollChildLayout.getParent()).removeView(scrollChildLayout);
		}

		if (totalRemoval) {
			//linkPosList.set(clickedPos,-1);
			//linkLangList.set(clickedPos, 0);levels
			linkOpenList.set(clickedPos, 0);
		}
	}

	private int getFilteredOffset(List<Text> links,int startPos) {
		if (filterMap == null) return startPos;

		for (int i = startPos; i < links.size(); i++) {
			if (filterMap.containsKey(Book.getTitle(links.get(i).bid))) {
				return i;
			}
		}

		return -1;
	}

	//boolean newUpdate is true when you are updating to a new link
	public void updateLink(View linkView, int position, int newLinkOffset, View parView, boolean newUpdate) {

		Text currText;
		if (position != links.size()-1 && position != -1) {
			currText = texts.get(position);
		} else { //this should only run for chapter links
			position = links.size()-1;
			currText = texts.get(0);
		}

		Integer linkOffset;
		if (newLinkOffset != -1) {
			linkOffset = newLinkOffset;
		}
		else linkOffset = linkPosList.get(position);

		linkOffset = getFilteredOffset(links.get(position), linkOffset);
		if (linkOffset == -1) return; //no more links :(


		Text linkText;
		try {
			linkText = links.get(position).get(linkOffset);
		} catch (IndexOutOfBoundsException e) {

			ArrayList<Text> linkList = (ArrayList<Text>)Link.getLinkedTexts(currText, -1, 0);
			links.set(position, linkList);
			try {
				linkText = linkList.get(linkOffset);

			} catch (IndexOutOfBoundsException e2) {
				//you tried to go too far (either negative or past numLinks)
				return;	
			}
		}
		//Log.d("yo","Real off: " + linkOffset);
		linkPosList.set(position, linkOffset);
		int linkLang = Text.getMaxLang(linkText);

		if (linkLang != linkLangList.get(position)) {
			//you've switched languages
			Log.d("lv"," You switched from " + linkLangList.get(position) + " to " + linkLang);
			linkLangList.set(position, linkLang);
			removeLink(position,parView,false);
			insertLinkView1(position, parView,true,false);
			linkView = parView.findViewById(R.id.link);
		}
		linkView.setBackgroundColor(bgLinkColor);

		Book tempBook = new Book(linkText.bid);
		String []  textLocPair = Header.getTextLocationString(linkText.levels, tempBook);
		String linkTextLocationEn = textLocPair[0];
		String linkTextLocationHe = textLocPair[1];
		String enNumTotal = textLocPair[2];
		String heNumTotal = textLocPair[3];


		//title
		AutofitTextView linkTitleTV = (AutofitTextView) linkView.findViewById(R.id.linkTitleTV);
		String bookTitle = "";

		if (menuLang == SettingsActivity.EN_CODE || menuLang == SettingsActivity.BI_CODE) {
			bookTitle = tempBook.title; //+ " " + linkTextLocationEn;
			linkTitleTV.setText(Html.fromHtml("<u>" + bookTitle + " " +  enNumTotal + "</u>"));

		} else /*if (menuLang == SettingsActivity.HE_CODE)*/ {
			bookTitle = tempBook.heTitle; //+ " " + linkTextLocationHe;
			linkTitleTV.setText(Html.fromHtml("<u>" + bookTitle + " " +  heNumTotal + "</u>"));

		}
		linkTitleTV.setTextColor(textColor);
		linkTitleTV.setTypeface(AppMenu.customFont);

		// text
		if (linkLang == 3) {
			TextView linkTextEnTV = (TextView) linkView.findViewById(R.id.en);
			TextView linkTextHeTV = (TextView) linkView.findViewById(R.id.he);
			linkTextEnTV.setTextSize((int)Math.round(fontSize * SettingsActivity.EN_HE_RATIO));
			linkTextHeTV.setTextSize(fontSize);
			linkTextEnTV.setTextColor(textColor);
			linkTextHeTV.setTextColor(textColor);
			linkTextEnTV.setTypeface(AppMenu.customFont);
			linkTextHeTV.setTypeface(AppMenu.customFont);
			linkTextEnTV.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
			linkTextHeTV.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
			linkTextEnTV.setText(Html.fromHtml("<b>(" + linkTextLocationEn + ") </b>" + linkText.enText));

			//nikud
			String hebText;
			if (showNikud) hebText = linkText.heText;
			else hebText = Util.getRemovedNikudString(linkText.heText);

			linkTextHeTV.setText(Html.fromHtml("<b>(" + linkTextLocationHe + ") </b>" + hebText));
		} else {
			TextView linkTextTV = (TextView) linkView.findViewById(R.id.linkTextTV);
			linkTextTV.setTextSize(fontSize);
			linkTextTV.setTextColor(textColor);
			linkTextTV.setTypeface(AppMenu.customFont);
			linkTextTV.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
			if (linkLang == 1) {
				linkTextTV.setTextSize((int)Math.round(fontSize * SettingsActivity.EN_HE_RATIO));
				linkTextTV.setText(Html.fromHtml("<b>(" + linkTextLocationEn + ") </b>" + linkText.enText));
			} else if (linkLang == 2) {


				//nikud
				String hebText;
				if (showNikud) hebText = linkText.heText;
				else hebText = Util.getRemovedNikudString(linkText.heText);

				linkTextTV.setText(Html.fromHtml("<b>(" + linkTextLocationHe + ") </b> " + hebText));
			}
		}

		//buttons
		Button nextTV = (Button) linkView.findViewById(R.id.nextLinkBtn);
		Button prevTV = (Button) linkView.findViewById(R.id.prevLinkBtn);
		if (fontSize < SettingsActivity.DEF_FONT_SIZE) {
			nextTV.setTextSize(fontSize);
			prevTV.setTextSize(fontSize);
		}




		/*
		//use button instead future nOah!
		double titleMaxWidth = nextTV.getLeft() - prevTV.getRight(); 
		double numChars = titleMaxWidth/10.0;
		Log.d("numChars","" + nextTV.getLeft());
		Log.d("numChars", "" + numChars);
		double titleSize = fontSize;
		double fontChanger = numChars*(numChars/fontSize);//number of chars that fit*(font size when checking that/fontSize)
		if (bookTitle.length() > fontChanger) titleSize = fontSize*(fontChanger/bookTitle.length());
		linkTitleTV.setTextSize((int)titleSize);
		 */



	}

	//checks if given view has a visible link
	public boolean hasVisibleLink(View view) {
		View linkView = view.findViewById(R.id.link);
		if (linkView != null) {
			return true;
		} else {
			//Toast.makeText(context, "FALSE", Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	private String getVerseNum(Text verse, boolean isHebrew) {
		if (!verse.displayNum) return "";
		String verseLoc = "";
		if (isHebrew) {
			for (int i = 1; i < book.wherePage-1; i++) {
				verseLoc += book.heSectionNamesL2B[i] + " " + Util.int2heb(verse.levels[i]);
				verseLoc += ", ";
			}
			verseLoc += Util.int2heb(verse.level1);
			return "<b>" +  verseLoc + ": </b> ";
		} else {
			for (int i = 1; i < book.wherePage-1; i++) {
				verseLoc += book.sectionNamesL2B[i] + " " + verse.levels[i];
				verseLoc += ", ";
			}
			verseLoc += verse.level1;
			return "<b>" + verseLoc + ": </b> ";
		}

	}
 
	public void handleVerseClick(View v, MotionEvent e, int pos) {
		
		eventView = v;
		switch (e.getAction() & MotionEvent.ACTION_MASK)  {

		case MotionEvent.ACTION_DOWN:
			
			//START DISGUSTING CODE
			//this delays the highlighting by a small amount of time so it doesn't interfere with zooming / scrolling
			touchTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (!isSwipingHor && !isSwipingVer && isTouchDown && mode != ZOOM) {
						//you must be touching, i guess...
						MyApp.currActivityContext.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								highlightVerse(true, touchedVerse);
							}
						});
					}
					
				}
			},TOUCH_TIMER_SLOP);
			//END
			
			touchedVerse = v;
			v.invalidate();
			v.getParent().requestDisallowInterceptTouchEvent(true);
			startTouchPosX = e.getX();
			startTouchPosY = e.getY();
			startTouchTime = e.getEventTime();
			Log.d("touch","start");
			isSwipingHor = false;
			isSwipingVer = false;
			isTouchDown = true;
			
			break;
		case MotionEvent.ACTION_MOVE:
			float diffX1 = e.getX()-startTouchPosX;
			float diffY1 = e.getY()-startTouchPosY;
			if (Math.abs(diffY1) > Math.abs(diffX1)) { //vert scroll
				//Log.d("event","Y: " + diffY1 + " Slop: " + swipeSlop);
				if (Math.abs(diffY1) > (float)swipeSlop) {
					isSwipingVer = true;
					isSwipingHor = false;
					
					highlightVerse(false, v);
					v.invalidate();
					v.getParent().requestDisallowInterceptTouchEvent(false);
				}

			} else {
				if (Math.abs(diffX1) > (float)swipeSlop) {
					isSwipingHor = true;
					isSwipingVer = false;
					//Log.d("event","SWIPE HORIZONTAL");
				}

			}
			
			if (isSwipingHor) {
				//v.setTranslationX(e.getX() + v.getTranslationX() - startTouchPosX);
			}
			
			
			break;
		case MotionEvent.ACTION_UP:
			
			if (isSwipingHor) {
				float deltaAbsX = Math.abs(e.getX() + v.getTranslationX() - startTouchPosX);
				float fractionCovered = 1 - (deltaAbsX / v.getWidth());
				long duration = (int) ((1 - fractionCovered) * 250);
				
				
				
				TranslateAnimation anim = new TranslateAnimation( v.getTranslationX(),0, 0, 0);
				anim.setDuration(duration);
				
				anim.setAnimationListener(new TranslateAnimation.AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) { }

					@Override
					public void onAnimationRepeat(Animation animation) { }

					@Override
					public void onAnimationEnd(Animation animation) 
					{
					    eventView.setTranslationX(0); //test
					}
					});
				
				v.startAnimation(anim);
			}
			
			isSwipingHor = false;
			isSwipingVer = false;
			isTouchDown = false;
			
			//blink grey than normal just in case you tap quickly
			highlightVerse(true, v);
			touchTimer = new Timer();
			touchTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					MyApp.currActivityContext.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							highlightVerse(false, touchedVerse);
						}
					});
					
				}
			},100);
			v.invalidate();
			v.getParent().requestDisallowInterceptTouchEvent(false);
			
			//handle touch click
			if (!isSwipingHor && ! isSwipingVer && !isLoadingLinks) {
				if (hasVisibleLink(v)) {
					removeLink(pos, v, true);
				} else {
					insertLinkView1(pos,v,false,true);
				}
			}

			break;
		}
	}
	
	
	private void highlightVerse(boolean shouldHighlight, View verse) {
		int color;
		if (shouldHighlight) color = MyApp.currActivityContext.getResources().getColor(R.color.click_down);
		else color = bgColor;
		
		if (currLang == SettingsActivity.BI_CODE) {
			verse.findViewById(R.id.he).setBackgroundColor(color);
			verse.findViewById(R.id.en).setBackgroundColor(color);
		} else { //he or en
			verse.findViewById(R.id.mono).setBackgroundColor(color);
		}
		verse.setBackgroundColor(color);
	}
	
	/*
	 *  Zoomzoom------------------------------------------------------------------------------------------------------------------
	 */
	/* ZOOM ZOOM */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	public void zoomZoom(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			//Log.d("zoom", "oldDist=" + oldDist);
			if (oldDist > 10f) {
				mode = ZOOM;
				//Log.d("zoom", "mode=ZOOM" );
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			//when done zoomZooming, update settings
			/*Editor edit = settings.edit();
			edit.putFloat("fontSize", Math.round(fontSize*100)/100); //round to make it prettier
			edit.apply();*/
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == ZOOM) {
				float newDist = spacing(event);
				// If you want to tweak font scaling, this is the place to go.
				if (newDist > 10f) {
					float scale = newDist / oldDist;
					//Log.d("zoom","Scale: " + scale);
					if (scale > 1) {
						scale = 1.015f;
					} else if (scale < 1) {
						scale = 0.985f;
					}

					float currentSize = fontSize * scale;
					if ((currentSize < MAX_FONT_SIZE && currentSize > MIN_FONT_SIZE)
							||(currentSize >= MAX_FONT_SIZE && scale < 1)
							|| (currentSize <= MIN_FONT_SIZE && scale > 1)) {
						fontSize = currentSize;
						notifyDataSetChanged();
					}


				}
			}
			break;
		}
	}

	/*
	 * PARALLEL STUFF-------------------------------------------------------------------------------------------------------------
	 * 
	 */

	private void loadLinks(int clickedPos, View inputView, boolean reinsert,boolean runUpdateAfterPart2, LayoutInflater layoutInflater) {

		Runnable r = new LinkLoadThread(clickedPos, inputView, reinsert, runUpdateAfterPart2,layoutInflater);
		new Thread(r).start();
	}

	//used for passing parameters to linkLoadHandler after insertLinkView1() finishes
	private class LinkMessage {
		int clickedPos;
		View inputView;
		boolean reinsert;
		boolean runUpdateAfterPart2;
		Text clickedLink;
		LayoutInflater layoutInflater;

		public LinkMessage(int clickedPos, View inputView, boolean reinsert,boolean runUpdateAfterPart2,Text clickedLink, LayoutInflater layoutInflater) {
			this.clickedPos = clickedPos;
			this.inputView = inputView;
			this.reinsert = reinsert;
			this.runUpdateAfterPart2 = runUpdateAfterPart2;
			this.clickedLink = clickedLink;
			this.layoutInflater = layoutInflater;
		}

		public LinkMessage(View inputView) {
			this.inputView = inputView;
		}
	}

	private Handler linkLoadHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			LinkMessage lmsg = (LinkMessage)(msg.obj);
			//remove loader
			((ViewGroup) lmsg.inputView).removeView(lmsg.inputView.findViewById(R.id.linkLoader));

			switch (msg.what) {
			case LINK_LOAD_COMPLETE:

				//let's move on to stage 2				
				insertLinkView2(lmsg.clickedPos, lmsg.inputView, lmsg.reinsert, lmsg.runUpdateAfterPart2, lmsg.clickedLink,lmsg.layoutInflater);
				//Toast.makeText(context, "link load complete", Toast.LENGTH_SHORT).show();

				break;
			case LINK_LOAD_FAILED:
				isLoadingLinks = false;
				String message = context.getString(R.string.no_links);
				if(API.useAPI())
					message = "Link feature requires library (download in Settings)";
				Toast.makeText(context, message,Toast.LENGTH_SHORT).show();
				//convoluted way of accessing list view and x mark...
				((View)lmsg.inputView.getParent().getParent()).findViewById(android.R.id.list).setVisibility(View.VISIBLE);
				((View)lmsg.inputView.getParent().getParent()).findViewById(R.id.titleClose).setVisibility(View.GONE);
				break;
			case NO_LINKS_MATCH_FILTER:
				isLoadingLinks = false;
				Toast.makeText(context, context.getString(R.string.no_match_filter), Toast.LENGTH_SHORT).show();
				((View)lmsg.inputView.getParent()).setVisibility(View.VISIBLE);
				Log.d("fl","yooooo");
				//convoluted way of accessing list view and x mark...
				((View)lmsg.inputView.getParent().getParent()).findViewById(android.R.id.list).setVisibility(View.VISIBLE);
				((View)lmsg.inputView.getParent().getParent()).findViewById(R.id.titleClose).setVisibility(View.GONE);
				break;
			}
		}
	};

	private class LinkLoadThread implements Runnable {
		int clickedPos;
		View inputView;
		boolean reinsert;
		boolean runUpdateAfterPart2;
		LayoutInflater layoutInflater;

		public LinkLoadThread(int clickedPos, View inputView, boolean reinsert,boolean runUpdateAfterPart2,LayoutInflater layoutInflater) {
			this.clickedPos = clickedPos;
			this.inputView = inputView;
			this.reinsert = reinsert;
			this.runUpdateAfterPart2 = runUpdateAfterPart2;
			this.layoutInflater = layoutInflater;
		}

		public void run() {
			//try {
			int offset = linkPosList.get(clickedPos);
			ArrayList<Text> linkList;
			if (clickedPos != links.size()-1) {
				Text clicked = texts.get(clickedPos);
				linkList = (ArrayList<Text>)Link.getLinkedTexts(clicked, -1, 0);
			} else {
				linkList = (ArrayList<Text>)Link.getLinkedChapTexts(texts.get(0), -1, 0);
			}
			links.set(clickedPos, linkList);
			int firstLink = getFilteredOffset(linkList, offset);
			if (linkList.size() > 0 && firstLink != -1) {
				Text clickedLink = linkList.get(firstLink);
				//congrats, you move on to part2
				//linkLoadHandler.sendEmptyMessage(LINK_LOAD_COMPLETE);
				Message msg = linkLoadHandler.obtainMessage();
				//msg.clickedPos = clickedPos;
				msg.obj = new LinkMessage(clickedPos, inputView, reinsert, runUpdateAfterPart2, clickedLink,layoutInflater);
				msg.what = LINK_LOAD_COMPLETE;
				linkLoadHandler.sendMessage(msg);
			} else if (linkList.size() == 0) {
				Message msg = linkLoadHandler.obtainMessage();
				msg.obj = new LinkMessage(inputView);
				msg.what = LINK_LOAD_FAILED;
				linkLoadHandler.sendMessage(msg);
				return;
			} else {
				Message msg = linkLoadHandler.obtainMessage();
				msg.obj = new LinkMessage(inputView);
				msg.what = NO_LINKS_MATCH_FILTER;
				linkLoadHandler.sendMessage(msg);
				return;
			}

			/*} catch (Exception e) {
				MyApp.sendException(e);
				throw new Error(e);
			}*/
		}




	}


}
