package com.torahsummary.betamidrash;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpFragment extends Fragment {
    
	private int imgId;
	private String descr;
	
	public HelpFragment(int imgId, String descr) {
		this.imgId = imgId;
		this.descr = descr;
	}
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_helppager, container, false);
		
		MyApp.sendScreen("Help");
        
        ImageView iv = (ImageView) rootView.findViewById(R.id.image);
		iv.setImageResource(imgId);
        
		TextView tv = (TextView) rootView.findViewById(R.id.descr);
		tv.setText(descr);
		
        return rootView;
    }
}
