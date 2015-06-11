package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView.OnEditorActionListener;

public class SearchActivity extends ListActivity {

	public static final int DONE_SEARCHING = 56;
	public static final int LOAD_MORE_SEARCH = 4343;
	public static final int DB_ERROR = 343;

	SearchAdapter adapter;
	SearchFilterAdapter sfAdapter;
	AutoCompleteTextView actv;
	LinearLayout loader;
	ArrayList<Text> searchResults;
	Thread currThread;

	boolean filterShowing;
	boolean firstTimeScrolledToBottom;

	String currQuery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApp.currActivityContext = this;
		setContentView(R.layout.activity_search);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		// getActionBar().hide();

		// need to restore state here so that AppMenu.getNamesOfChildren() will
		// work
		if (savedInstanceState != null)
			AppMenu.restoreState(this, savedInstanceState);

		Searching.init();
		firstTimeScrolledToBottom = true;
		filterShowing = false;
		// ArrayList<Text> yo = Searching.searchDBheTexts("\u05d0\u05d1\u05d0");
		ArrayList<Text> tempList = new ArrayList<Text>();
		adapter = new SearchAdapter(this, R.layout.search_monoverse, tempList);
		setListAdapter(adapter);

		getListView().setOnTouchListener(listListener);
		getListView().setOnScrollListener(listScroll);

		loader = (LinearLayout) this.findViewById(R.id.loader);
		loader.setVisibility(View.GONE);

		// filter search
		findViewById(R.id.filterSearchLayout).setVisibility(View.GONE);
		ListView filtView = (ListView) findViewById(R.id.filterSearchList);

		ArrayList<String> engItems = new ArrayList<String>();

		String[] cats = AppMenu.getNamesOfChildren(AppMenu.menuRoot, false,
				false, engItems);
		engItems.add("Commentary");
		cats = Arrays.copyOf(cats, cats.length + 1);
		if (AppMenu.displayLang == SettingsActivity.EN_CODE)
			cats[cats.length - 1] = "Commentary";
		else if (AppMenu.displayLang == SettingsActivity.HE_CODE)
			cats[cats.length - 1] = "\u05DE\u05E4\u05E8\u05E9\u05D9\u05DD";
		sfAdapter = new SearchFilterAdapter(this, R.layout.item_filtergroup,
				cats, sfClick);
		sfAdapter.engItems = engItems;

		filtView.setAdapter(sfAdapter);

		ArrayList<String> allBookNames = new ArrayList<String>(
				AppMenu.enBookNames);
		allBookNames.addAll(AppMenu.heBookNames);

		actv = (AutoCompleteTextView) this.findViewById(R.id.actv);
		ArrayAdapter<String> autoComAdapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, allBookNames);
		actv.setAdapter(autoComAdapter);
		actv.setOnItemClickListener(autoComItemClick);
		actv.setOnFocusChangeListener(autoComFocus);
		actv.setOnEditorActionListener(autoComEnterClick);
		findViewById(R.id.clearButton).setOnClickListener(clearClick);

		// auto search query that was sent from menu
		String query = getIntent().getStringExtra("query");
		actv.setText(query);
		if (query.length() > 0)
			search(query);

		View root = findViewById(R.id.root);
		ViewTreeObserver viewTreeObserver = root.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
			viewTreeObserver
					.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

						@Override
						public void onGlobalLayout() {
							View root = SearchActivity.this
									.findViewById(R.id.root);
							root.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
							actv.setDropDownWidth(root.getWidth());

						}
					});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// set dropdown width to width of phone.
		findViewById(R.id.root).measure(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		int num = findViewById(R.id.root).getMeasuredWidth();
		actv.setDropDownWidth(num);
		getListView().requestFocus();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		goBack();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelableArrayList("searchResults", searchResults);
		outState.putBoolean("firstTimeScrolledToBottom",
				firstTimeScrolledToBottom);
		outState.putString("currQuery", currQuery);
		outState.putBoolean("filterShowing", filterShowing);
		// Searching
		outState.putBoolean("doneSearching", Searching.doneSearching);
		outState.putInt("currChunkIndex", Searching.getCurrChunkIndex());
		// SearchAdapter
		outState.putInt("searchLang", adapter.searchLang);
		// SFAdapter
		outState.putBooleanArray("checkList", sfAdapter.checkList);

		if (currThread != null && currThread.isAlive())
			Searching.interrupted = true;

		AppMenu.saveState(outState);
		AppMenu.restoredByActivity = 0;
	}

	@Override
	protected void onRestoreInstanceState(Bundle in) {
		super.onRestoreInstanceState(in);

		searchResults = in.getParcelableArrayList("searchResults");
		firstTimeScrolledToBottom = in.getBoolean("firstTimeScrolledToBottom");
		currQuery = in.getString("currQuery");
		filterShowing = in.getBoolean("filterShowing");
		// Searching
		Searching.doneSearching = in.getBoolean("doneSearching");
		Searching.setCurrChunkIndex(in.getInt("currChunkIndex"));
		// SearchAdapter
		adapter.searchLang = in.getInt("searchLang");
		adapter.context = this;
		// SfAdapter
		sfAdapter.checkList = in.getBooleanArray("checkList");
		sfAdapter.context = this;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			goBack();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void goBack() {
		if (filterShowing) {
			showFilterLayout(false);
		} else {
			if (currThread != null && currThread.isAlive())
				Searching.interrupted = true;
			finish();
		}
	}

	private void showFilterLayout(boolean show) {
		if (show) {
			filterShowing = true;
			findViewById(R.id.searchLayout).setVisibility(View.GONE);
			findViewById(R.id.filterSearchLayout).setVisibility(View.VISIBLE);
		} else {
			filterShowing = false;
			findViewById(R.id.searchLayout).setVisibility(View.VISIBLE);
			findViewById(R.id.filterSearchLayout).setVisibility(View.GONE);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		int pos = getListView().getPositionForView(v);
		Text searchText = adapter.items.get(position);

		Intent intent = new Intent(SearchActivity.this, TextActivity.class);

		intent.putExtra("bid", searchText.bid);
		Book linkedBook = new Book(searchText.bid);
		// Log.d("links","levels = " + Arrays.toString(linkedText.levels));
		int[] tempLevels = Arrays.copyOfRange(searchText.levels, 0,
				linkedBook.textDepth);

		// I think this should work...
		if (linkedBook.textDepth > 1) {
			for (int i = 0; i <= linkedBook.wherePage - 2; i++)
				tempLevels[i] = 0;
		}
		intent.putExtra("levels", tempLevels);

		intent.putExtra("title", linkedBook.title);

		// the specific verse that the link links to. could be zero if there is
		// none
		if (linkedBook.textDepth > 1)
			intent.putExtra("linkedVerse", searchText.levels[0]);

		intent.putExtra("isLink", true);
		intent.putExtra("isSearch", true);
		intent.putExtra("searchQuery", currQuery);
		startActivity(intent);
	}

	// this is a click event listener
	public void selectAll(View button) {
		sfAdapter.setAllChecked(true);
		sfAdapter.notifyDataSetChanged();
	}

	// this is a click event listener
	public void selectNone(View button) {
		sfAdapter.setAllChecked(false);
		sfAdapter.notifyDataSetChanged();
	}

	// this is a click event listener
	public void doneClick(View button) {
		showFilterLayout(false);
	}

	// this is a click event listener
	public void filterClick(View button) {
		showFilterLayout(true);
	}

	// this is a click event listener
	public void searchClick(View button) {
		String query = String.valueOf(actv.getText());
		if (query.length() > 0) {
			search(query);
		}
	}

	private void search(String query) {
		try {
			MyApp.sendEvent("Search", query);
			currQuery = query;
			adapter.items = new ArrayList<Text>();
			adapter.clear();
			adapter.notifyDataSetChanged();

			View searchHelpTV = findViewById(R.id.searchHelp);// hide
																// searchingHelp
			searchHelpTV.setVisibility(View.GONE);

			Searching.init();
			firstTimeScrolledToBottom = true;

			loader.setVisibility(View.VISIBLE);
			getListView().setVisibility(View.GONE);
			if (currThread != null && currThread.isAlive())
				Searching.interrupted = true;
			currThread = new Thread(new SearchRunnable());
			currThread.start();
			// hide keyboard
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(actv.getWindowToken(), 0);
		} catch (SQLiteException e) {
			DialogManager.dismissCurrentDialog();
			DialogManager.showDialog(DialogManager.LIBRARY_EMPTY);
		}
	}

	OnClickListener sfClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int pos = ((ListView) findViewById(R.id.filterSearchList))
					.getPositionForView(v);
			CheckBox cb = (CheckBox) v.findViewById(R.id.groupCB);
			boolean isChecked = cb.isChecked();
			cb.setChecked(!isChecked);

			sfAdapter.checkList[pos] = !isChecked;

			String eng = sfAdapter.engItems.get(pos);
			if (!isChecked) { // currently checked
				sfAdapter.currFilter.add(eng);
			} else {
				sfAdapter.currFilter.remove(eng);
			}
			// Log.d("filt", Arrays.toString(sfAdapter.currFilter.toArray()));
		}
	};

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
		public void onItemClick(android.widget.AdapterView<?> parent, View v,
				int pos, long id) {
			String bookName = (String) ((TextView) v).getText();
			String[] children = AppMenu.jumpToBook(bookName);

			if (!AppMenu.inJsonMenu && AppMenu.currBook.bid == 0) {
				Toast.makeText(SearchActivity.this,
						getString(R.string.book_not_available),
						Toast.LENGTH_SHORT).show();
				AppMenu.back();
				return;
			}

			Intent intent = new Intent(SearchActivity.this,
					MenuLevelActivity.class);
			intent.putExtra("menuItems", children);
			intent.putExtra("inChapPage", !AppMenu.inJsonMenu);
			startActivityForResult(intent, MenuLevelActivity.BACK_CODE);
			overridePendingTransition(R.anim.slide_in_right,
					R.anim.slide_out_left);
		};
	};

	OnEditorActionListener autoComEnterClick = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {

				String textEntered = String.valueOf(((TextView) v).getText());
				if (textEntered.length() > 0) {
					search(textEntered);
				}
				return true;
			}
			return false;
		}
	};

	OnClickListener clearClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			actv.setText("");

		}
	};

	OnTouchListener listListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			getListView().requestFocus();
			// hide keyboard
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(actv.getWindowToken(), 0);
			return false;
		}
	};

	OnScrollListener listScroll = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView arg0, int arg1) {
			if (arg0.getLastVisiblePosition() == arg0.getAdapter().getCount() - 1) {
				// Toast.makeText(SearchActivity.this,"END",Toast.LENGTH_SHORT).show();
				handler.sendEmptyMessage(LOAD_MORE_SEARCH);
			}
		}

		@Override
		public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {

		}
	};

	public Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case DONE_SEARCHING:

				loader.setVisibility(View.GONE);
				getListView().setVisibility(View.VISIBLE);

				adapter.addResults(searchResults);

				if (searchResults.size() == 0) {
					;// Toast.makeText(SearchActivity.this,getString(R.string.found)
						// + " " + searchResults.size() +
						// getString(R.string.occurrences) + " \"" + currQuery +
						// "\"",Toast.LENGTH_SHORT).show();
					Toast.makeText(
							SearchActivity.this,
							getString(R.string.finished_searching) + ": "
									+ currQuery, Toast.LENGTH_SHORT).show();
				}
				// Toast.makeText(SearchActivity.this,"" + searchResults.size()
				// + " texts contain \"" + currQuery + "\"",
				// Toast.LENGTH_SHORT).show();
				break;

			case LOAD_MORE_SEARCH:
				if (!Searching.doneSearching && !currThread.isAlive()) {
					// Log.d("searcher","currChund " + Searching.currChunk +
					// " / " + Searching.CHUNK_COUNT);
					loader.setVisibility(View.VISIBLE);
					currThread = new Thread(new SearchRunnable());
					currThread.start();
				} else if (SearchActivity.this.firstTimeScrolledToBottom) {
					// Toast.makeText(SearchActivity.this,"Found all occurences of \""
					// + currQuery + "\"",Toast.LENGTH_SHORT).show();
					SearchActivity.this.firstTimeScrolledToBottom = false;
				}
				break;
			case DB_ERROR:

				loader.setVisibility(View.GONE);
				DialogManager.showDialog(DialogManager.LIBRARY_EMPTY);
				break;
			}
		};
	};

	private class SearchRunnable implements Runnable {

		public SearchRunnable() {
		}

		@Override
		public void run() {

			try {
				Object[] currFilterArrayObj = sfAdapter.currFilter.toArray();
				String[] currFilterArray = Arrays.copyOf(currFilterArrayObj,
						currFilterArrayObj.length, String[].class);
				if (currFilterArray.length == sfAdapter.items.length) {
					currFilterArray = new String[0]; // no reason to filter, you
														// selected everything
				}
				if (currQuery.replaceFirst("[A-Za-z]", "ABC").hashCode() != currQuery
						.hashCode()) { // do english search
					adapter.searchLang = SettingsActivity.EN_CODE;
					searchResults = Searching.searchEnTexts(currQuery,
							(String[]) currFilterArray);
				} else { // hebrew search
					adapter.searchLang = SettingsActivity.HE_CODE;
					searchResults = Searching.searchDBheTexts(currQuery,
							(String[]) currFilterArray);

				}
				handler.sendEmptyMessage(DONE_SEARCHING);
			} catch (InterruptedException e) {
				Searching.interrupted = false;
				return;
			} catch (SQLiteException e) {
				handler.sendEmptyMessage(DB_ERROR);
				// handler.sendEmptyMessage(DB_ERROR);
			}

		}
	}
}
