package com.torahsummary.betamidrash;

import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ettie on 9/6/2015.
 */
public class GetSefariaJSONData extends GetRawData {

    private String LOG_TAG = GetSefariaJSONData.class.getSimpleName();
    private List<Text> mText;
    private Uri mDestinationUri;

    public GetSefariaJSONData(String sefer, String perek) {
        super(null);
        CreateAndUpdateURI(sefer, perek);
        mText = new ArrayList<Text>();
    }

    public void execute() {
        super.setmRawUrl(mDestinationUri.toString());
        DownloadJSONData downloadJSONData = new DownloadJSONData();
        Log.v(LOG_TAG, "Built URI = " + mDestinationUri.toString());
        downloadJSONData.execute(mDestinationUri.toString());
    }

    public boolean CreateAndUpdateURI(String sefer, String perek) {
        final String SEFARIA_BASE_URL = "http://www.sefaria.org/api/texts/";
        final String SEFER_PARAM = "";
        final String PEREK_PARAM = ".";
        /*final String COMMENTARY = "commentary";*/

       /* mDestinationUri = Uri.parse(SEFARIA_BASE_URL).buildUpon()
                .appendQueryParameter(SEFER_PARAM, sefer)
                .appendQueryParameter(PEREK_PARAM, perek)
                .build();*/

        //mDestinationUri = SEFARIA_BASE_URL + sefer + PEREK_PARAM + perek;
        mDestinationUri = Uri.parse(SEFARIA_BASE_URL).buildUpon()
                .appendPath(sefer + "." + perek /*+ "?commentary=0"*/)
               /* .appendQueryParameter("commentary", "0")*/
                .build();

        return  mDestinationUri != null;
    }

    public void processResult() {
        if (getmDownloadStatus() != DownloadStatus.OK) {
            Log.e(LOG_TAG, "error downloading raw file");
            return;
        }

        final String SEFARIA_TEXT = "text";
        final String SEFARIA_HE = "he";
        String[] pasukEnglish = null;
        String[] pasukHebrew = null;
        String pEnglish = null;
        String pHebrew = null;
        try {
            JSONObject jsonData = new JSONObject(getmData());
            JSONArray textArray = jsonData.getJSONArray(SEFARIA_TEXT);
            pasukEnglish = new String[textArray.length()];
            for (int i = 0; i < textArray.length(); i++) {
                //JSONObject jsonText = textArray.getJSONObject(i);
                //String pasuk = jsonText.toString();
                //pasukEnglish[i] = jsonText.toString();
                pasukEnglish[i] = textArray.getString(i);
                pEnglish = textArray.toString();
                Log.v(LOG_TAG, "eng: " + pEnglish);
                Log.v(LOG_TAG, pasukEnglish[i]);
                //GetSefariaJSONData.tvOutput.setText(pasukEnglish[i]);
                //System.out.println(pasukEnglish[i]);
            }
            //String[] pasukEnglish = textArray.toString();
            //JSONObject jsonHe = jsonData.getJSONObject(SEFARIA_HE);
            //String pasukHebrew = jsonHe.toString();
            JSONArray heArray = jsonData.getJSONArray(SEFARIA_HE);
            pasukHebrew = new String[heArray.length()];
            for (int i = 0; i < heArray.length(); i++) {
                //JSONObject jsonText = textArray.getJSONObject(i);
                //String pasuk = jsonText.toString();
                //pasukEnglish[i] = jsonText.toString();
                pasukHebrew[i] = heArray.getString(i);
                pHebrew = heArray.toString();
                Log.v(LOG_TAG, "he: " + pHebrew);
                Log.v(LOG_TAG, pasukHebrew[i]);
                //System.out.println(pasukHebrew[i]);
                /*Log.v(LOG_TAG, pasukHebrew);*/
            }
           /* pasukHebrew = jsonData.getString(SEFARIA_HE);*/

         //   Text text = new Text(pasukEnglish, pasukHebrew);
            Text text = new Text(pEnglish, pHebrew);
            this.mText.add(text);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "error processing json data");
        }

    }

    public class DownloadJSONData extends DownloadRawData {
        protected void onPostExecute(String webData) {
            super.onPostExecute(webData);
            processResult();

        }

        protected String doInBackground(String... params) {
            return super.doInBackground(params);
        }
    }
}
