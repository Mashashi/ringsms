package pt.mashashi.ringsms;

import android.util.Log;

public class MyLog {
	
	public final static boolean debug = true;
	
	public static int d(String tag, String what){
		if(debug){
			return Log.d(tag, what);
		}
		return -1;
	}
	
}
