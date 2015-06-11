package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MenuItemAdapter extends ArrayAdapter<String> {

	public static final int MENU_FONT_SIZE = 20;

	Context context;
	public ArrayList<String> items;

	public List<Header> chapItems; //for when in text menus
	//public List<Integer> chapNums;
	boolean isChapPage;
	public boolean addBmMode = false;
	//

	public MenuItemAdapter(Context context, int resource, ArrayList<String> strItems, boolean isChapPage) {
		super(context,resource,strItems);
		this.context = context;
		this.items = strItems;
		this.chapItems = AppMenu.currChaps;
		//this.chapNums = AppMenu.currChapInts;
		this.isChapPage = isChapPage;
		//if (isChapPage) Collections.sort(this.chapItems,MenuItemComparator); //to deal with misordering on certain "devices"
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String item = items.get(position);

		float fontSize = 0f;
		if (isChapPage){
			if(AppMenu.displayLang == 2) {//he
				item = chapItems.get(position).heHeader;
				fontSize = MENU_FONT_SIZE * SettingsActivity.EN_HE_RATIO;
			} else {
				item = chapItems.get(position).enHeader;
				fontSize = MENU_FONT_SIZE;
			}
		}
		View view = convertView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)
					context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.item_menu,null);



		}
		//		CheckBox cb = (CheckBox) view.findViewById(R.id.bmCheckbox);
		//		if (addBmMode && cb.getVisibility() != View.VISIBLE) {
		//			cb.setVisibility(View.VISIBLE);
		//			cb.setOnCheckedChangeListener(cbListener);
		//		} else if (!addBmMode && cb.getVisibility() != View.GONE) {
		//			cb.setVisibility(View.GONE);
		//		}
		TextView tv = (TextView) view.findViewById(R.id.menuText);
		tv.setTypeface(AppMenu.customFont);
		tv.setText(item);
		//tv.setTextSize(fontSize);
		return view;
	}

	public void changeLang(int displayLang) {
		AppMenu.displayLang = displayLang;

		if(AppMenu.inJsonMenu){
			String[] tempItems = AppMenu.getNamesOfChildren(true);
			this.clear();
			for (String string:tempItems) {
				this.insert(string, this.getCount());
			}
		}
		this.notifyDataSetChanged();

	}

	public void changeBmMode() {
		if (addBmMode) addBmMode = false;
		else addBmMode = true;
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
	/*

	public static int compareTo(Header item1,Header item2) {
		//int num1 = Integer.parseInt(item1.substring(item1.lastIndexOf(" ")+1));
		//int num2 = Integer.parseInt(item2.substring(item2.lastIndexOf(" ")+1));

		return item1.chapNum - item2.chapNum;
	}

	public static Comparator<Header> MenuItemComparator 
	= new Comparator<Header>() {

		public int compare(Header item1, Header item2) {

			return compareTo(item1,item2);
		}

	};
	 */
}
