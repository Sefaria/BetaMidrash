package com.torahsummary.betamidrash;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HelpFragmentActivity extends FragmentActivity {


	private ViewPager vPager;
	private PagerAdapter pAdapter;
	
	private Button prevBtn;
	private Button nextBtn;
	private TextView progressTV;
	
	private int currPage;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_help);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		prevBtn = (Button) findViewById(R.id.prev);
		nextBtn = (Button) findViewById(R.id.next);
		progressTV = (TextView) findViewById(R.id.progressTV);
		progressTV.setText("1/"+HelpFragmentAdapter.numPages);
		
		
		List<Integer> helpItems = Arrays.asList(R.drawable.help_ab1,R.drawable.help_ab2,R.drawable.help_ab3
				,R.drawable.help_links1,R.drawable.help_links5, R.drawable.help_links4, R.drawable.help_filt1
				,R.drawable.help_find1, R.drawable.help_find2, R.drawable.help_find3, R.drawable.help_search1, R.drawable.help_search2);
		
		List<String> helpDescrs = Arrays.asList("Menu Bar 1","Menu Bar 2","Menu Bar 3","View Sources 1","View Sources 2","View Sources 3", "View Sources 4","Find On Page 1","Find On Page 2","Find On Page 3", "Search 1", "Search 2");
		HelpFragmentAdapter.numPages = helpItems.size();
		progressTVUpdate(1); //init
		vPager = (ViewPager) findViewById(R.id.pager);
		vPager.setOnPageChangeListener(pageChangeListener);
		vPager.setPageTransformer(true, new ZoomOutPageTransformer());
		pAdapter = new HelpFragmentAdapter(getSupportFragmentManager(),helpItems,helpDescrs);

		vPager.setAdapter(pAdapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			goBack(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		goBack(false);
	}

	private void goBack(boolean fullBack) {
		if (vPager.getCurrentItem() == 0 || fullBack) {
			super.onBackPressed();
		} else {
			// Otherwise, select the previous step.
			vPager.setCurrentItem(vPager.getCurrentItem() - 1);
		}
	}

	//event listener
	public void prevClick(View button) {
		if (currPage == 0) {
			return;
		} else {
			//buttonUpdate(true);
			vPager.setCurrentItem(currPage-1);
			
		}
	}

	//event listener
	public void nextClick(View button) {
		if (currPage == HelpFragmentAdapter.numPages-1) {
			goBack(true); //you've reached the end. finish it
		} else {
			//buttonUpdate(false);
			vPager.setCurrentItem(currPage+1);

		}
	}
	
	private void buttonUpdate(boolean isPrev) {
		if (isPrev) {
			if (currPage == HelpFragmentAdapter.numPages-1) {
				nextBtn.setText("Next");
			}
			currPage = vPager.getCurrentItem();
		} else {
			currPage = vPager.getCurrentItem();
			if (currPage == HelpFragmentAdapter.numPages-1) {
				nextBtn.setText(this.getString(R.string.finish));
			}
		}
		Log.d("help","New Page" + currPage);
	}
	
	private void progressTVUpdate(int newNum) {
		progressTV.setText(newNum + "/" + HelpFragmentAdapter.numPages);
	}
	
	private OnPageChangeListener pageChangeListener = new OnPageChangeListener() {
		//when new page is selected
		@Override
		public void onPageSelected(int pos) {
			boolean isPrev = pos < currPage;
			Log.d("help","isPrev = " + isPrev);
			buttonUpdate(isPrev);
			progressTVUpdate(currPage+1);
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}
		
		
		@Override
		public void onPageScrollStateChanged(int state) {
			
			
		}
	};
	
	public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
	    private static final float MIN_SCALE = 0.85f;
	    private static final float MIN_ALPHA = 0.5f;

	    public void transformPage(View view, float position) {
	        int pageWidth = view.getWidth();
	        int pageHeight = view.getHeight();

	        if (position < -1) { // [-Infinity,-1)
	            // This page is way off-screen to the left.
	            view.setAlpha(0);

	        } else if (position <= 1) { // [-1,1]
	            // Modify the default slide transition to shrink the page as well
	            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
	            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
	            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
	            if (position < 0) {
	                view.setTranslationX(horzMargin - vertMargin / 2);
	            } else {
	                view.setTranslationX(-horzMargin + vertMargin / 2);
	            }

	            // Scale the page down (between MIN_SCALE and 1)
	            view.setScaleX(scaleFactor);
	            view.setScaleY(scaleFactor);

	            // Fade the page relative to its size.
	            view.setAlpha(MIN_ALPHA +
	                    (scaleFactor - MIN_SCALE) /
	                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

	        } else { // (1,+Infinity]
	            // This page is way off-screen to the right.
	            view.setAlpha(0);
	        }
	    }
	}

}


