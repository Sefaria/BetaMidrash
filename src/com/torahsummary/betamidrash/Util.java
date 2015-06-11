package com.torahsummary.betamidrash;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;




//contains useful random functions
public class Util {
	static final private char[] heChars = {
		'\u05d0','\u05d1','\u05d2','\u05d3','\u05d4','\u05d5','\u05d6','\u05d7','\u05d8','\u05d9',
		//'\u05da',
		'\u05db','\u05dc',
		//'\u05dd',
		'\u05de',
		//'\u05df',
		'\u05e0','\u05e1','\u05e2',
		//'\u05e3',
		'\u05e4',
		//'\u05e5',
		'\u05e6','\u05e7','\u05e8','\u05e9',
	'\u05ea'};


	public static boolean isSystemLangHe(){
		return Locale.getDefault().getLanguage().equals("iw");
	}

	static String readFile(Context context, String path) throws IOException 
	{
		Resources resources = context.getResources();
		InputStream iS = resources.getAssets().open(path);
		//create a buffer that has the same size as the InputStream  
		byte[] buffer = new byte[iS.available()];  
		//read the text file as a stream, into the buffer  
		iS.read(buffer);  
		//create a output stream to write the buffer into  
		ByteArrayOutputStream oS = new ByteArrayOutputStream();  
		//write this buffer to the output stream  
		oS.write(buffer);  
		//Close the Input and Output streams  
		oS.close();  
		iS.close();  

		//return the output stream as a String  
		return oS.toString();  
	}

	static JSONObject getJSON(Context context, String path) throws JSONException,IOException { 
		String jsonText = readFile(context,path);
		JSONObject object = (JSONObject) new JSONTokener(jsonText).nextValue();
		return object;	
	}

	static String joinArrayList(ArrayList<?> r, String delimiter) {
		if(r == null || r.size() == 0 ){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		int i, len = r.size() - 1;
		for (i = 0; i < len; i++){
			sb.append(r.get(i).toString() + delimiter);
		}
		return sb.toString() + r.get(i).toString();
	}
	
	//given two arrays, join them (need to be non-primitive)
	static <T> T[] concatenateArrays (T[] a, T[] b) {
	    int aLen = a.length;
	    int bLen = b.length;

	    @SuppressWarnings("unchecked")
	    T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
	    System.arraycopy(a, 0, c, 0, aLen);
	    System.arraycopy(b, 0, c, aLen, bLen);

	    return c;
	}

	static String[] str2strArray(String str) {
		if(str == null)
			return new String [] {};
		final Pattern r = Pattern.compile("(\\[|\\]|\")+"); //matches all [ , ] & "
		str = r.matcher(str).replaceAll("");
		String[] strArray = str.split(",");
		return strArray;
	}

	public static boolean deleteNonRecursiveDir(String dirname){
        File dir = new File(dirname);
        if (dir.exists() && dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null) {
	                for (int i = 0; i < children.length; i++) {
	                    new File(dir, children[i]).delete();
	                }
                }
            }
        return dir.delete();
    }
	
	static String getRemovedNikudString(String nikStr) {
		//final Pattern r = Pattern.compile("[\u0591-\u05C7]");
		final Pattern r = Pattern.compile("[\u0591-\u05BD\u05BF\u05C7]");
		final Pattern r2 = Pattern.compile("\u05be"); 
		return r.matcher( r2.matcher(nikStr).replaceAll(" ")).replaceAll("");
		//return r.matcher(nikStr).replaceAll("");
	}

	static int[] concatIntArrays(int[] a, int[] b) {
		int aLen = a.length;
		int bLen = b.length;
		int[] c= new int[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	static String int2heb(int num) {
		int origNum = num;
		String heb = "";
		int place = 0;
		while (num >= 1) {
			int digit = num%10;
			//Log.d("gem","DIGIT: " + digit); 
			num /= 10;
			int baseHebChar = 0; //this is the position of a char in hebChars
			char hebChar;
			if (digit == 0) {
				//Log.d("gem","zero");
				hebChar = '\0'; //no char when exactly multiple of ten
			}
			else {
				if (place == 0) { 
					baseHebChar = 0; //alef
					hebChar = heChars[(baseHebChar + digit-1)];
					heb = hebChar + heb;
				} else if (place == 1) {
					baseHebChar = 9; //yud
					hebChar = heChars[(baseHebChar + digit-1)];
					heb = hebChar + heb;
				} else if (place >= 2) {
					baseHebChar = 18; //kuf
					if (digit == 9) { //can't be greater than tuf
						char hChar1 = heChars[(baseHebChar + digit-9)];
						char hChar2 = heChars[(baseHebChar + 3)]; //tuf, need two of these
						heb = "" + hChar2 + hChar2 + hChar1 + heb;
					} else if (digit > 4) {
						char hChar1 = heChars[(baseHebChar + digit-5)];
						char hChar2 = heChars[(baseHebChar + 3)]; //tuf
						heb = "" + hChar2 + hChar1 + heb;
					} else {
						char hChar1 = heChars[(baseHebChar + digit-1)];
						heb = "" + hChar1 + heb;
					}
				}
			}
			place++;	
		}
		//now search for 15 & 16 to replace
		final String ka = "\u05D9\u05D4"; //carefull...don't join these strings
		final String ku = "\u05D9\u05D5";
		final Pattern kaPatt = Pattern.compile("(" + ka + ")+");
		final Pattern kuPatt = Pattern.compile("(" + ku + ")+");
		heb = kaPatt.matcher(heb).replaceAll("\u05D8\u05D5");
		heb = kuPatt.matcher(heb).replaceAll("\u05D8\u05D6");

		//Log.d("gem",origNum + " = " + heb);
		return heb;
	}


	public static void moveFile(String inputPath, String inputFile, String outputPath, String outputFile) {

		InputStream in = null;
		OutputStream out = null;
		try {

			//create output directory if it doesn't exist
			File dir = new File (outputPath); 
			if (!dir.exists())
			{
				dir.mkdirs();
			}


			in = new FileInputStream(inputPath + inputFile);        
			out = new FileOutputStream(outputPath + outputFile);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;

			// write the output file
			out.flush();
			out.close();
			out = null;

			// delete the original file
			new File(inputPath + inputFile).delete();  
		} catch (Exception e) {
			MyApp.sendException(e, "Moving file");
		}


	}


	
	public static String convertDBnum(int DBnum){
		if(DBnum <= 0)
			return String.valueOf(DBnum);
		String passDot;
		if(DBnum%100 <10){
			passDot = "0" + DBnum%100;
		}
		else
			passDot = "" + DBnum%100;
		return String.valueOf(DBnum/100) + "." + passDot;
	}
	
	public static float pixelsToSp(Context context, float px) {
	    float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
	    return px/scaledDensity;
	}

}


