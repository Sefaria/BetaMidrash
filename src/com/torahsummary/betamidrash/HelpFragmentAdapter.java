package com.torahsummary.betamidrash;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class HelpFragmentAdapter extends FragmentStatePagerAdapter {
	
	public static int numPages;
	
	private List<Integer> items;
	private List<String> descrs;
	
	public HelpFragmentAdapter(FragmentManager fm, List<Integer> imgList, List<String> descrList) {
		super(fm);
		
		items = imgList;
		descrs = descrList;
	}
	
	@Override
	public Fragment getItem(int pos) {
		return new HelpFragment(items.get(pos),descrs.get(pos));
	}

	@Override
	public int getCount() {
		return numPages;
	}

}
