package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.torahsummary.betamidrash.R;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class FilterLinksAdapter extends BaseExpandableListAdapter {
	
	Context context;
	List<String> headers;
	HashMap<String,List<String>> childrenMap;
	ArrayList<ArrayList<Boolean>> isCheckedGrid;
	ArrayList<Boolean> isGroupOpen;
	
	public FilterLinksAdapter(Context context, List<String> headers, HashMap<String,List<String>> childrenMap) {
		this.context = context;
		this.headers = headers;
		this.childrenMap = childrenMap;
		
		isCheckedGrid = new ArrayList<ArrayList<Boolean>>();
		isGroupOpen = new ArrayList<Boolean>();
		for (int i = 0; i < headers.size(); i++) {
			ArrayList<Boolean> tempArray = new ArrayList<Boolean>();
			for (int j = 0; j < getChildrenCount(i); j++) {
				tempArray.add(true);
			}
			isCheckedGrid.add(tempArray);
			isGroupOpen.add(false);
			
		}
	}
	
	@Override
	public int getGroupCount() {
		return this.headers.size();
	}

	@Override
    public int getChildrenCount(int groupPosition) {
        return this.childrenMap.get(this.headers.get(groupPosition)).size();
    }

	@Override
	public Object getGroup(int groupPosition) {
		return this.headers.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return childrenMap.get(headers.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.item_filtergroup, null);
        }
 
        CheckBox lblListHeader = (CheckBox) convertView.findViewById(R.id.groupCB);
        TextView lblListHeaderTV = (TextView) convertView.findViewById(R.id.groupTV);
        ArrayList<Boolean> tempArray = isCheckedGrid.get(groupPosition);
        boolean allChecked = areAllCheckedInGroup(groupPosition);
        lblListHeader.setChecked(allChecked);
        lblListHeaderTV.setTypeface(null, Typeface.BOLD);
        lblListHeaderTV.setText(headerTitle);
        
        
 
        return convertView;
    }

	@Override
	public View getChildView(int groupPosition, int childPosition,boolean isLastChild, View convertView, ViewGroup parent) {
	        final String childText = (String) getChild(groupPosition, childPosition);
	 
	        if (convertView == null) {
	            LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(R.layout.item_filtersub, null);
	        }
	 
	        CheckBox txtListChild = (CheckBox) convertView.findViewById(R.id.subCB);
	        TextView txtListChildTV = (TextView) convertView.findViewById(R.id.subTV);
	        txtListChild.setChecked(isCheckedGrid.get(groupPosition).get(childPosition));
	        txtListChildTV.setText(childText);
	        return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	public boolean areAllCheckedInGroup(int groupPosition) {
		ArrayList<Boolean> tempArray = isCheckedGrid.get(groupPosition);
		for (int i = 0; i < tempArray.size(); i++) {
			if (!tempArray.get(i)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean areAllNotCheckedInGroup(int groupPosition) {
		ArrayList<Boolean> tempArray = isCheckedGrid.get(groupPosition);
		for (int i = 0; i < tempArray.size(); i++) {
			if (tempArray.get(i)) {
				return false;
			}
		}
		return true;
	}
	
	public void setAllChecked(boolean isChecked) {
		for (int i = 0; i < isCheckedGrid.size(); i++) {
			ArrayList<Boolean> tempArray = isCheckedGrid.get(i);
			for (int j = 0; j < tempArray.size(); j++) {
				tempArray.set(j,isChecked);
			}
		}
	}
	
	public HashMap<String,Boolean> getFilterMap() {
		HashMap<String,Boolean> filter = new HashMap<String,Boolean>();
		for (int i = 0; i < isCheckedGrid.size(); i++) {
			ArrayList<Boolean> tempArray = isCheckedGrid.get(i);
			for (int j = 0; j < tempArray.size(); j++) {
				if (tempArray.get(j)) {
					String bookTitle = childrenMap.get(headers.get(i)).get(j);
					bookTitle = bookTitle.substring(0,bookTitle.lastIndexOf(' '));
					filter.put(bookTitle, true);
				}
			}
		}
		return filter;
	}
	
	public void initCheckedGrid(ArrayList<ArrayList<Boolean>> oldCheckedGrid) {
		if (oldCheckedGrid == null) return;
		
		isCheckedGrid = oldCheckedGrid;
	}

}
