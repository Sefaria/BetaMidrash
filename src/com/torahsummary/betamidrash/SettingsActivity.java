package com.torahsummary.betamidrash;

import java.io.File;
import com.torahsummary.betamidrash.R;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {


	public static final int BG_WHITE = 0xfff8fcf8;
	public static final int BG_BLACK = 0xff000000;
	public static final int BG_LINK_WHITE = 0xffdddddd;
	public static final int BG_LINK_BLACK = 0xff222222;
	public static final int EN_CODE = 1;
	public static final int HE_CODE = 2;
	public static final int BI_CODE = 3;
	public static final int BI_LAYOUT_HOR = 0;
	public static final int BI_LAYOUT_VER = 1;
	public static final int DEF_LANG = BI_CODE;
	public static final int DEF_NUM_LINKS = 5;
	public static final float DEF_FONT_SIZE = 20f;
	public static final float EN_HE_RATIO = 17.0f/DEF_FONT_SIZE;
	public static final int DEF_BG_COLOR = BG_WHITE;
	public static final boolean DEF_NIKUD_BOOL = true;
	public static final int DEF_BILINGUAL_LAYOUT = 0; //zero is hor
	public static final float DEF_LINE_SPACING = 1.3f;
	public static final int DEF_BT_LANG = EN_CODE;
	public static final int SBL = 24;
	public static final int UPDATE_DELAY_TIME = 14*24*60*60*1000;// DAYS*HOURS*MINS*SEC*MILLISEC  - in milliseconds;
	private static final int USING_DEBU_FONT_CODE = 123123579;







	public static Activity contextYo; //this is just here to be used in Thread



	//background color (black/white)
	//num links shown at once (int)
	//default language (en/he/enandhe)
	//font size (int)
	private SharedPreferences settings; 
	private EditText numLinksET;
	private EditText fontSizeET;
	private RadioGroup bgColorRG;
	private RadioGroup langRG;
	private RadioGroup btlangRG;
	private RadioGroup nikudRG;
	private RadioGroup bilayoutRG;

	private boolean unsavedChanges;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApp.currActivityContext = this;
		unsavedChanges = false;
		MyApp.sendScreen("Settings");
		//actionBar.setBackgroundDrawable(new ColorDrawable(0xFF357035));

		setContentView(R.layout.activity_settings);
		//addPreferencesFromResource(R.xml.user_settings);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		settings = getSharedPreferences("appSettings", Context.MODE_PRIVATE);
		//numLinksET = (EditText) findViewById(R.id.numLinksET);
		fontSizeET = (EditText) findViewById(R.id.fontSizeET);
		bgColorRG = (RadioGroup) findViewById(R.id.bgColorRG);
		langRG = (RadioGroup) findViewById(R.id.langRG);
		btlangRG = (RadioGroup) findViewById(R.id.btlangRG);
		nikudRG = (RadioGroup) findViewById(R.id.nikudRG);
		bilayoutRG = (RadioGroup) findViewById(R.id.bilayoutRG);
		bgColorRG.requestFocus();
		//register download complete handler

		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		if (SDK_INT >= 17) { //17 is 4.2 which doesn't handle vowels well
			TextView nikudWarning = (TextView) findViewById(R.id.nikudWarning);
			nikudWarning.setVisibility(View.GONE);
		}

		//set version num
		int versionNum = settings.getInt("versionNum", -1);
		TextView versTV = (TextView) findViewById(R.id.currLibVersTV);
		versTV.append(""+Util.convertDBnum(versionNum)) ;
		if(settings.getString("csvURL", Downloader.CSV_REAL_URL).equals( Downloader.CSV_DEBUG_URL))
			versTV.append("_D");



		updateForm();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			saveSettings();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		//Downloader.unregisterDownloader(this);
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		/*private static Activity contextYo; //this is just here to be used in Thread

		//these two vars are stupid, but work. they are persistent vars which I need for checking version num. before and after this, they serve no purpose
		private static int updatedVersionNum;
		private static int currentVersionNum;
		private static boolean userInitiated;*/
		outState.putInt("updatedVersionNum",UpdateService.updatedVersionNum);
		outState.putInt("currentVersionNum", UpdateService.currentVersionNum);
		outState.putBoolean("userInitiated", UpdateService.userInitiated);
		outState.putBoolean("inUpdateStage3", UpdateService.inUpdateStage3);
		outState.putBoolean("isShowingDialog",DialogManager.isShowingDialog);
		outState.putInt("currentDialog", DialogManager.currentDialog);

		AppMenu.saveState(outState);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if(DialogManager.isShowingDialog) {
			if (DialogManager.currentDialog != -1) {
				int currDia = DialogManager.currentDialog;
				DialogManager.dismissCurrentDialog();
				Log.d("up","remaking dialog");
				DialogManager.showDialog(currDia);

			}
		} 
	}

	@Override
	protected void onRestoreInstanceState(Bundle in) {
		super.onRestoreInstanceState(in);

		AppMenu.restoreState(this, in);
		AppMenu.restoredByActivity = R.string.activity_search;

		UpdateService.updatedVersionNum = in.getInt("updatedVersionNum");
		UpdateService.currentVersionNum = in.getInt("currentVersionNum");
		UpdateService.userInitiated = in.getBoolean("userInitiated");
		UpdateService.inUpdateStage3 = in.getBoolean("inUpdateStage3");
		contextYo = this;
		DialogManager.isShowingDialog = in.getBoolean("isShowingDialog");
		DialogManager.currentDialog = in.getInt("currentDialog");

		if(DialogManager.isShowingDialog) {
			if (DialogManager.currentDialog != -1) {
				int currDia = DialogManager.currentDialog;
				DialogManager.dismissCurrentDialog();
				//Log.d("dia","remaking dialog");
				DialogManager.showDialog(currDia);

			}
		} 
		Log.d("up","restoring");
		File testFile = new File(UpdateService.DATABASE_ZIP_DOWNLOAD_LOC);
		/*if (testFile.exists()) {

			UpdateService.updateStage3();
		} else {
			UpdateService.restart();
		}*/
	}

	@Override
	public void onBackPressed() {
		saveSettings();
		return;
	}

	public void saveSettings(View button) {
		saveSettings();
	}

	public void saveSettings() {
		//int numLinks = Integer.parseInt(numLinksET.getText().toString());
		float fontSize = Float.parseFloat(fontSizeET.getText().toString());
		int bgColorId = bgColorRG.getCheckedRadioButtonId();
		int langId = langRG.getCheckedRadioButtonId();
		int btlangId = btlangRG.getCheckedRadioButtonId();
		int nikudId = nikudRG.getCheckedRadioButtonId();
		int bilayoutId = bilayoutRG.getCheckedRadioButtonId();

		
		int lang = DEF_LANG;
		if (langId == R.id.engRB) lang = EN_CODE;
		else if (langId == R.id.hebRB) lang = HE_CODE;
		else if (langId == R.id.bilRB) lang = BI_CODE;

		int bgColor = DEF_BG_COLOR;
		if (bgColorId == R.id.bgBlack) bgColor = BG_BLACK;
		else if (bgColorId == R.id.bgWhite) bgColor = BG_WHITE;

		boolean showNikud = DEF_NIKUD_BOOL;
		if (nikudId == R.id.nikudYesRB) showNikud = true;
		else if (nikudId == R.id.nikudNoRB) showNikud = false;

		int bilayout = DEF_BILINGUAL_LAYOUT;
		if (bilayoutId == R.id.bilayoutHorRB) bilayout = BI_LAYOUT_HOR;
		else bilayout = BI_LAYOUT_VER;

		int btlang = DEF_BT_LANG;
		if (btlangId == R.id.btengRB) btlang = EN_CODE;
		else if (btlangId == R.id.bthebRB) btlang = HE_CODE;

		Editor edit = settings.edit();
		if(fontSize == USING_DEBU_FONT_CODE){//you have entered into debug mode
			Toast.makeText(this, "Debug", Toast.LENGTH_SHORT).show();
			edit.putString("csvURL", Downloader.CSV_DEBUG_URL);
		}
		else{
			edit.putFloat("fontSize", fontSize);
			edit.putString("csvURL", Downloader.CSV_REAL_URL);
		}
		//edit.putInt("numLinks", numLinks);
		edit.putInt("bgColor", bgColor);
		edit.putInt("lang", lang);
		edit.putInt("menuLang", btlang);
		edit.putBoolean("showNikud", showNikud);
		edit.putInt("bilayout", bilayout);
		edit.apply();
		
		String settingsString = "fontSize="  + fontSize + ";bgColor=" +  bgColor + ";lang=" + langId + ";menuLang=" + btlang
				+ ";showNikud=" + showNikud + ";bilayout=" + bilayout; 
		MyApp.sendEvent(MyApp.SETTING_CHANGE, settingsString);

		Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();

		Intent intent = new Intent();
		intent.putExtra("menuLang",btlang);
		setResult(MenuLevelActivity.SETTINGS_RESULT_CODE,intent); //not using this now...
		finish();
	}

	//after a change to the form, update input fields
	private void updateForm() {
		unsavedChanges = true;
		//int numLinks = settings.getInt("numLinks", DEF_NUM_LINKS);
		float fontSize = settings.getFloat("fontSize", DEF_FONT_SIZE);
		int bgColor = settings.getInt("bgColor", DEF_BG_COLOR);
		int lang = settings.getInt("lang", DEF_LANG);
		int btlang = settings.getInt("menuLang", DEF_BT_LANG);
		boolean showNikud = settings.getBoolean("showNikud", DEF_NIKUD_BOOL);
		int bilayoutId = settings.getInt("bilayout", DEF_BILINGUAL_LAYOUT);

		//numLinksET.setText("" + numLinks);
		fontSizeET.setText("" + fontSize);

		if (bgColor == BG_BLACK) bgColorRG.check(R.id.bgBlack);
		else if (bgColor == BG_WHITE) bgColorRG.check(R.id.bgWhite);

		if (lang == EN_CODE) langRG.check(R.id.engRB);
		else if (lang == HE_CODE) langRG.check(R.id.hebRB);
		else if (lang == BI_CODE) langRG.check(R.id.bilRB);

		if (btlang == EN_CODE) btlangRG.check(R.id.btengRB);
		else if (btlang == HE_CODE) btlangRG.check(R.id.bthebRB);

		if (showNikud) nikudRG.check(R.id.nikudYesRB);
		else nikudRG.check(R.id.nikudNoRB);

		if (bilayoutId == BI_LAYOUT_HOR) bilayoutRG.check(R.id.bilayoutHorRB);
		else bilayoutRG.check(R.id.bilayoutVerRB);
	}

	public void restoreDefaults(View button) {
		Editor edit = settings.edit();
		edit.putFloat("fontSize", DEF_FONT_SIZE);
		edit.putInt("numLinks", DEF_NUM_LINKS);
		edit.putInt("bgColor", DEF_BG_COLOR);
		edit.putInt("lang", DEF_LANG);
		edit.putInt("menuLang", DEF_BT_LANG);
		edit.putBoolean("showNikud", DEF_NIKUD_BOOL);
		edit.putInt("bilayout", DEF_BILINGUAL_LAYOUT);
		edit.apply();

		updateForm();

		Toast.makeText(this, getString(R.string.defaults_restored), Toast.LENGTH_SHORT).show();
	}





	//this is a click event listener
	public void updateLibrary(View button) {
		UpdateService.lockOrientation(this);
		Intent intent = new Intent(this,UpdateReceiver.class);
		intent.putExtra("isPre",true);
		intent.putExtra("userInit",true);
		sendBroadcast(intent);
		DialogManager.showDialog(DialogManager.CHECKING_FOR_UPDATE);
	}






}
