package com.torahsummary.betamidrash;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class SearchFilterAdapter extends ArrayAdapter {

	public Context context;
	public String[] items;
	public ArrayList<String> engItems;
	public boolean[] checkList;
	private OnClickListener itemClick;

	public ArrayList<String> currFilter;

	public SearchFilterAdapter(Context context,int resource, String[] objs, OnClickListener itemClick) {
		super(context,resource,objs);
		this.context = context;
		this.items = objs;
		this.checkList = new boolean[objs.length];
		this.itemClick = itemClick;
		this.currFilter = new ArrayList<String>();
		this.engItems = new ArrayList<String>();
		this.setAllChecked(false);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.item_filtergroup, null);
			v.setOnClickListener(itemClick);
		}
		TextView tv = (TextView) v.findViewById(R.id.groupTV);
		tv.setText(items[position]);

		CheckBox cb = (CheckBox) v.findViewById(R.id.groupCB);
		cb.setChecked(checkList[position]);
		return v;
	}

	public void setAllChecked(boolean isCheck) {
		for (int i = 0; i < checkList.length; i++) {
			checkList[i] = isCheck;
			if (engItems.size() != 0) {
				String eng = engItems.get(i);
				if (isCheck) { //currently checked
					currFilter.add(eng);
				} else {
					currFilter.remove(eng);
				}
			}
		}
	}
}
