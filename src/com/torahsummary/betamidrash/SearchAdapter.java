package com.torahsummary.betamidrash;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchAdapter extends ArrayAdapter<Text> {
	
	public List<Text> items;
	public Context context;
	public int searchLang;
	
	public SearchAdapter(Context context, int resource, List<Text> objects) {
		super(context,resource,objects);
		this.items = objects;
		this.context = context;
	}
	
	@Override
	public View getView(int position, View v, ViewGroup parent) {
		Text text = items.get(position);
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.search_monoverse, null);
		}
		
		//v.setBackgroundColor(SettingsActivity.BG_WHITE);
		
		TextView tv = (TextView) v.findViewById(R.id.tv);
		tv.setTypeface(AppMenu.customFont);
		if (searchLang == SettingsActivity.EN_CODE) {
			tv.setText(Html.fromHtml(text.enText));
			tv.setTextSize((int)Math.round(SettingsActivity.DEF_FONT_SIZE * SettingsActivity.EN_HE_RATIO));
		} else if (searchLang == SettingsActivity.HE_CODE) {
			tv.setText(Html.fromHtml(text.heText));
			tv.setTextSize(SettingsActivity.DEF_FONT_SIZE);
		}
		//linkTextEnTV.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
		tv.setLineSpacing(0, SettingsActivity.DEF_LINE_SPACING);
		//tv.setTextColor(SettingsActivity.BG_BLACK);
		
		Book tempBook = new Book(text.bid);
		
		AutofitTextView titleTV = (AutofitTextView) v.findViewById(R.id.titleView);
		String [] textLocation = Header.getTextLocationString(text.levels, tempBook);
		String bookTitle = "";
		SharedPreferences settings = MyApp.getContext().getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		int menuLang = settings.getInt("menuLang", 0);
		if (menuLang == SettingsActivity.EN_CODE)
			bookTitle = tempBook.title + " " + textLocation[0];
		else //if (menuLang == SettingsActivity.HE_CODE)
			bookTitle = tempBook.heTitle + " " + textLocation[1];
		titleTV.setText(Html.fromHtml("<u>" + bookTitle + "</u>"));
		titleTV.setTypeface(AppMenu.customFont);
		//titleTV.setTextColor(SettingsActivity.BG_BLACK);
		//titleTV.setTextSize(SettingsActivity.DEF_FONT_SIZE);
		
		
		return v;
	}
	
	public void addResults(ArrayList<Text> searchResults) {
		items.addAll(searchResults);
		for (Text text : searchResults) {
			insert(text,getCount());
		}
		notifyDataSetChanged();
	}
}
