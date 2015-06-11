package com.torahsummary.betamidrash;

import java.util.Calendar;
import java.util.Date;

import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends Activity {
	
	private static final int PRE_NUM_EASTER_TAPS = 3;
	private static final int NUM_EASTER_TAPS = 7;
	
	private int numTaps = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApp.currActivityContext = this;
		setContentView(R.layout.activity_about);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		MyApp.sendScreen("About");
		
		
		SharedPreferences settings = getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		int DBversionNum = settings.getInt("versionNum", -1);

		TextView aboutP1 = (TextView) findViewById(R.id.about_text_p1);
		TextView aboutP3 = (TextView) findViewById(R.id.about_text_p3);
		TextView aboutP5 = (TextView) findViewById(R.id.about_text_p5);
		TextView DBversionTV = (TextView) findViewById(R.id.about_DBVersionTV);
		TextView appVersionTV = (TextView) findViewById(R.id.about_appVersionTV);
		TextView copyrightTV = (TextView) findViewById(R.id.about_copy);
		
		aboutP1.setText(Html.fromHtml(this.getString(R.string.about_text_p1)));
		aboutP1.setMovementMethod(LinkMovementMethod.getInstance());
		
		aboutP3.setText(Html.fromHtml(this.getString(R.string.about_text_p3)));
		aboutP3.setMovementMethod(LinkMovementMethod.getInstance());
		
		aboutP5.setText(Html.fromHtml(this.getString(R.string.about_text_p5)));
		aboutP5.setMovementMethod(LinkMovementMethod.getInstance());
	
		aboutP5.setOnClickListener(easterEggClick);
		
		DBversionTV.append("" + Util.convertDBnum(DBversionNum));
	
		try {
			appVersionTV.append("" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			if(settings.getString("csvURL", Downloader.CSV_REAL_URL).equals( Downloader.CSV_DEBUG_URL))
				appVersionTV.append("D");
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			MyApp.sendException(e, "aboutPage_VersionName");
			
		}
		int currYear = Calendar.getInstance().get(Calendar.YEAR);
		if(currYear > 2015)
			copyrightTV.append(" - " + currYear);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
				goBack();
				return true;
		}
		return super.onOptionsItemSelected(item);
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
	}
	
	@Override
	public void onBackPressed() {
		goBack();
	}
	
	private void goBack() {
		finish();
	}
	
	private OnClickListener easterEggClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (numTaps > NUM_EASTER_TAPS - PRE_NUM_EASTER_TAPS && numTaps < NUM_EASTER_TAPS) {
				Toast.makeText(AboutActivity.this, (NUM_EASTER_TAPS-numTaps) + " taps to unlock secret mode...", Toast.LENGTH_SHORT).show();
			}
			
			if (numTaps == NUM_EASTER_TAPS) {
				Toast.makeText(AboutActivity.this, "What are you doing tapping around?! Shteig torah you am ha'aretz!!",Toast.LENGTH_LONG);
			}
			numTaps++;
			
		}
	};
}
