package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.torahsummary.betamidrash.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;



public class BookmarksActivity extends ListActivity {
	private BookmarkAdapter adapter;
	private SharedPreferences bookmarkSettings;
	private boolean inEditMode;
	private boolean isRemove;
	private String bmTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApp.currActivityContext = this;
		setContentView(R.layout.activity_bookmarks);
		ActionBar actionBar = getActionBar();
		MyApp.sendScreen("Bookmarks");
		//actionBar.setBackgroundDrawable(new ColorDrawable(0xFF357035));
		
		inEditMode = false;
		bookmarkSettings = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);
		Map<String,?> bmMap = bookmarkSettings.getAll();
		List<String> bmList = bmMap2List(bmMap);
		getActionBar().setDisplayHomeAsUpEnabled(true); //turn on menu back button

		adapter = new BookmarkAdapter(this, R.layout.item_bookmark, bmList,deleteClick,bookmarkClick);
		setListAdapter(adapter);
		
		Intent intent = getIntent();
		boolean isTextPage = intent.getBooleanExtra("isTextPage", false);
		boolean isBookmarked = intent.getBooleanExtra("isBookmarked", false);
		bmTitle = intent.getStringExtra("bmTitle");
		if (!bmList.isEmpty() || isTextPage) {
			View nobmsTV = findViewById(R.id.nobms);
			nobmsTV.setVisibility(View.GONE);
		}
		if (!isTextPage) {
			View addBMbtn = findViewById(R.id.addbmbtn);
			addBMbtn.setVisibility(View.GONE);
			
			View remBMbtn = findViewById(R.id.rembmbtn);
			remBMbtn.setVisibility(View.GONE);
		} else {
			if (isBookmarked) {
				View addBMbtn = findViewById(R.id.addbmbtn);
				addBMbtn.setVisibility(View.GONE);
			} else {
				View remBMbtn = findViewById(R.id.rembmbtn);
				remBMbtn.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmarks, menu);
		
		MenuItem editBtn = menu.findItem(R.id.action_edit);
		MenuItem stopBtn = menu.findItem(R.id.action_stop_edit);
		
		if (inEditMode) {
			editBtn.setVisible(false);
			stopBtn.setVisible(true);
		} else {
			editBtn.setVisible(true);
			stopBtn.setVisible(false);
		}
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppMenu.saveState(outState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle in) {
		super.onRestoreInstanceState(in);
		AppMenu.restoreState(this, in);
		AppMenu.restoredByActivity = R.string.activity_help;
	}
	
	private List<String> bmMap2List(Map<String,?> bmMap) {
		List<String> bmList = new ArrayList<String>();
		for (Map.Entry<String, ?> entry : bmMap.entrySet())
		{
			 boolean tempValue = Boolean.class.cast(entry.getValue());
		    if (tempValue) {
		    	bmList.add(entry.getKey());
		    }
		}
		return bmList;
	}
	
	@Override
	public void onBackPressed() {
		goBack();
		return;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home://really the back button
			goBack();
			return true;
		case R.id.action_edit:
			inEditMode = true;
			adapter.toggleEditMode();
			invalidateOptionsMenu();
			return true;
		case R.id.action_stop_edit:
			inEditMode = false;
			adapter.toggleEditMode();
			invalidateOptionsMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	//runs when you finish looking at a bookmark
	//make sure you reload bookmarks
	@Override
	protected void onResume() {
		super.onResume();
		inEditMode = false;
		Log.d("yo","RUNNING");
		bookmarkSettings = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);
		Map<String,?> bmMap = bookmarkSettings.getAll();
		adapter.items = bmMap2List(bmMap);
		adapter.extractTextPosList();
		List<String> tempItems = new ArrayList<String>();
		for (String s:adapter.items) {
			tempItems.add(s);
		}
		adapter.clear();
		for (String string:tempItems) {
			adapter.insert(string, adapter.getCount());
		}
		
		adapter.notifyDataSetChanged();
	}
	private void goBack() {
		Intent intent = new Intent();
		//isRemove gets set true whenever you delete any bm.
		intent.putExtra("isRemove",isRemove);
		setResult(TextActivity.CHANGE_BMS,intent);
		finish();
	}
	
	//this is a click event listener
	public void addBookmark(View button) {
		Intent intent = new Intent();
		intent.putExtra("isAdd",true);
		setResult(TextActivity.CHANGE_BMS,intent);
		finish();
	}
	
	//this is a click event listener
	public void removeBookmark(View button) {
		Editor bEdit = bookmarkSettings.edit();
		bEdit.putBoolean(bmTitle, false);
		bEdit.apply();
		
		
		
		Intent intent = new Intent();
		intent.putExtra("isRemove", true);
		setResult(TextActivity.CHANGE_BMS,intent);
		finish();
	}
	
	 public OnClickListener deleteClick = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int pos = getListView().getPositionForView(v);
				String currItem = adapter.items.get(pos);
				adapter.remove(adapter.getItem(pos)); //remove it from the gui
				adapter.items.remove(pos); //actually remove the data
				adapter.textPosList.remove(pos);
				adapter.notifyDataSetChanged();
				Editor bEdit = bookmarkSettings.edit();
				bEdit.putBoolean(currItem, false);
				bEdit.apply();
				isRemove = true;
			}
	 };
	 
	 public OnClickListener bookmarkClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (inEditMode) return;
			int pos = getListView().getPositionForView(v);
			String currItem = adapter.textPosList.get(pos);
			
			int divPos = currItem.lastIndexOf(' ');
			String bookName = currItem.substring(0,divPos);
			String chapStr = currItem.substring(divPos+1);
			
			String[] chapStrArray = chapStr.split(":");
			int[] chapArray = new int[6]; //the total possible number of levels
			for (int i = 0; i < chapStrArray.length; i++) {
				chapArray[chapStrArray.length-i] = Integer.parseInt(chapStrArray[i]);
			}
			
			Book bmBook = new Book(bookName);
			if (bmBook.bid == 0) {
				Toast.makeText(BookmarksActivity.this, getString(R.string.book_not_available), Toast.LENGTH_SHORT).show();
				return;
			}
			
			
			Intent intent = new Intent(BookmarksActivity.this,TextActivity.class);
			
			intent.putExtra("bid", bmBook.bid);
			
			int[] tempLevels = Arrays.copyOfRange(chapArray, 0,bmBook.textDepth);
			intent.putExtra("levels", tempLevels);
			
			intent.putExtra("title", bookName);
			
			
			

			intent.putExtra("isLink", true);
			startActivity(intent);
		}
	};
}
