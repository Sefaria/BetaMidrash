package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.Toast;

public class FilterLinksActivity extends Activity {

	FilterLinksAdapter adapter;
	ExpandableListView expList;

	private HashMap<String,List<String>> lmap;
	private Text dummyText;
	private int tidMax;

	public static final int LINKS_LOADED = 248;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case LINKS_LOADED:
				initEnd();
				break;
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApp.currActivityContext = this;
		setContentView(R.layout.activity_filterlinks);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		int tid = intent.getIntExtra("dummyText", -1);
		tidMax = intent.getIntExtra("tidMax", -1);
		
		if(API.useAPI()){
			//TODO make this work
			Toast.makeText(this, "Not an option without full Library", Toast.LENGTH_SHORT).show();
		}else{
			dummyText = new Text(tid);
			initStart();
			findViewById(R.id.content).setVisibility(View.GONE);
		}


	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Toast.makeText(this, "CHANGE", Toast.LENGTH_LONG).show();
		switch(item.getItemId()) {
		case android.R.id.home:
			goBack();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		goBack();

		return;
	}

	private void initStart() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				lmap = getLinksMap(dummyText, tidMax);
				handler.sendEmptyMessage(LINKS_LOADED);
				//Log.d("fl",Arrays.toString(lmap.keySet().toArray()));
			}
		}).start();

	}

	private void initEnd() {
		adapter = new FilterLinksAdapter(this,new ArrayList<String>(lmap.keySet()), lmap);
		expList = (ExpandableListView) findViewById(R.id.expList);
		expList.setAdapter(adapter);
		expList.setOnGroupClickListener(groupClick);
		expList.setOnChildClickListener(childClick);

		ArrayList<ArrayList<Boolean>> oldCheckedGrid = (ArrayList<ArrayList<Boolean>>) getIntent().getSerializableExtra("checkedGrid");
		adapter.initCheckedGrid(oldCheckedGrid);


		if (lmap.isEmpty()) {
			expList.setVisibility(View.GONE);
		} else {
			findViewById(R.id.noLinksTV).setVisibility(View.GONE);
		}

		findViewById(R.id.loader).setVisibility(View.GONE);
		findViewById(R.id.content).setVisibility(View.VISIBLE);
	}

	private void goBack() {
		Intent intent = new Intent();
		intent.putExtra("filterMap", adapter.getFilterMap());
		intent.putExtra("checkedGrid", adapter.isCheckedGrid);
		setResult(TextActivity.FILTER_COMPLETE,intent);
		finish();
	}

	private HashMap<String,List<String>> getLinksMap(Text text, int tidMax1) {
		Text dummyText  = Text.makeDummyChapText0(text);
		HashMap<String,List<String>> map = new HashMap<String,List<String>>();		
		List<Pair<String,Integer>> linkPairs = Link.getCountsTitlesFromLinks_small(dummyText, text.tid, tidMax1);// Link.getCountsTitles(dummyText);
		HashMap<String,Integer> headerLinkCountMap = new HashMap<String,Integer>();


		for (int i = 0; i < linkPairs.size(); i++) {
			Pair<String,Integer> tempPair = linkPairs.get(i);
			String tempKey = AppMenu.bidMap.get(tempPair.first);
			Integer tempHeaderCount = headerLinkCountMap.get(tempKey);
			if (tempHeaderCount == null) {
				tempHeaderCount = 0;
			}
			headerLinkCountMap.put(tempKey, tempHeaderCount+tempPair.second);

			List<String> tempBooks = map.get(tempKey);
			if (tempBooks == null) {//we need to initialize this category
				tempBooks = new ArrayList<String>();
			}
			//tempBooks.add(tempBooks.size(),getLinkString(tempPair));
			tempBooks.add(tempBooks.size(),getLinkString(tempPair));
			map.put(tempKey,tempBooks);
		}

		//now refresh all keys to reflect total numbering
		Object[] oldKeys = map.keySet().toArray(); 
		for (int i = 0; i < oldKeys.length; i++) {
			String key = (String)oldKeys[i];
			map.put(getLinkString(key,headerLinkCountMap.get(key)),map.remove(key));
		}
		return map;
	}

	private String getLinkString(Pair<String,Integer> pair) {
		return pair.first + " (" + pair.second + ")";
	}

	private String getLinkString(String str, int num) {
		return str + " (" + num + ")";
	}

	//this is a click event listener
	public void selectAll(View button) {
		adapter.setAllChecked(true);
		adapter.notifyDataSetChanged();
	}

	//this is a click event listener
	public void deselectAll(View button) {
		adapter.setAllChecked(false);
		adapter.notifyDataSetChanged();
	}
	
	public void doneClick(View button) {
		goBack();
	}

	public OnGroupClickListener groupClick = new OnGroupClickListener() {

		@Override
		public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
			boolean isOpen = adapter.isGroupOpen.get(groupPosition);
			adapter.isGroupOpen.set(groupPosition, !isOpen);

			CheckBox cb = ((CheckBox) v.findViewById(R.id.groupCB));
			boolean isChecked = cb.isChecked();


			boolean allChecked = adapter.areAllCheckedInGroup(groupPosition);
			boolean allNotChecked = adapter.areAllNotCheckedInGroup(groupPosition);

			if (isChecked && allChecked) {
				cb.setChecked(!isChecked);

				int numChildren = adapter.getChildrenCount(groupPosition);
				ArrayList<Boolean> tempArray = adapter.isCheckedGrid.get(groupPosition);
				for (int i = 0; i < numChildren; i++) {
					tempArray.set(i,!isChecked);
				}
			} else if (!isChecked && allNotChecked) {
				cb.setChecked(!isChecked);

				int numChildren = adapter.getChildrenCount(groupPosition);
				ArrayList<Boolean> tempArray = adapter.isCheckedGrid.get(groupPosition);
				for (int i = 0; i < numChildren; i++) {
					tempArray.set(i, !isChecked);
				}
				adapter.isGroupOpen.set(groupPosition, isOpen);
				adapter.notifyDataSetChanged();
				return true;
			}

			return false;
		}
	};

	public OnChildClickListener childClick = new OnChildClickListener() {

		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
			CheckBox cb = (CheckBox) v.findViewById(R.id.subCB);
			boolean isChecked = !cb.isChecked();
			cb.setChecked(isChecked);
			adapter.isCheckedGrid.get(groupPosition).set(childPosition, isChecked);
			Log.d("fl","sub click");
			return false;
		}
	};
}
