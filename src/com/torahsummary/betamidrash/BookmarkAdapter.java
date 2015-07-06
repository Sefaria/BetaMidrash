package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.List;

import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class BookmarkAdapter extends ArrayAdapter<String> {
	public static final String BOOKMARK_SEPERATOR = "__";
	
	public List<String> items;
	public List<String> textPosList; //a parallel list to items. it has the actual strings which uniquely identify each text location (see TextActivity.getCurrBookmarkString())
	private Context context;
	private boolean inEditMode;
	private OnClickListener deleteClick;
	private OnClickListener bookmarkClick;
	
	public BookmarkAdapter(Context context, int resource, List<String> objects, OnClickListener deleteClick, OnClickListener bookmarkClick) {
		super(context,resource,objects);
		this.items = objects;
		this.context = context;
		this.deleteClick = deleteClick;
		this.bookmarkClick = bookmarkClick;
		this.textPosList = new ArrayList<String>();
		extractTextPosList();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		String currItem;
		try {
			currItem = items.get(position).substring(0,items.get(position).indexOf(BOOKMARK_SEPERATOR));
		} catch (IndexOutOfBoundsException e) {
			convertView.setVisibility(View.GONE);
			this.notifyDataSetChanged();
			return convertView;
		}
		View view;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) 
					context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.item_bookmark, null);
			TextView tv = (TextView) view.findViewById(R.id.bmText);
			ImageButton ib = (ImageButton) view.findViewById(R.id.bmDelete);
			tv.setOnClickListener(bookmarkClick);
			ib.setOnClickListener(deleteClick);
			
		} else {
			view = convertView;
		}
		TextView tv = (TextView) view.findViewById(R.id.bmText);
		tv.setText(currItem);
		tv.setTypeface(AppMenu.customFont);
		ImageButton ib = (ImageButton) view.findViewById(R.id.bmDelete);
		if (inEditMode) {
			ib.setVisibility(View.VISIBLE);
		} else {
			ib.setVisibility(View.GONE);
		}
		return view;
	}
	
	public void extractTextPosList() {
		for (int i = 0; i < items.size(); i++) {
			try {
				String tempTextPos = items.get(i).substring(items.get(i).indexOf(BOOKMARK_SEPERATOR)+BOOKMARK_SEPERATOR.length());
				textPosList.add(tempTextPos);
			} catch (Exception e) {
				MyApp.sendException(e);
				textPosList.add(items.get(i));
			}
		}
	}
	
	public void toggleEditMode() {
		if (inEditMode) inEditMode = false;
		else inEditMode = true;
		List<String> tempItems = new ArrayList<String>();
		for (String s:items) {
			tempItems.add(s);
		}
		this.clear();
		for (String string:tempItems) {
			this.insert(string, this.getCount());
		}
		
		this.notifyDataSetChanged();
	}
}
