package pt.mashashi.ringsms.interfazze;

import java.util.concurrent.atomic.AtomicBoolean;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class InterfazzeStarterService extends Service{
	
	private ListenForMessageRequestBroacast interfazzeListener;
	private final static AtomicBoolean isRunning = new AtomicBoolean(false);
	public static boolean isRunning(){
		return isRunning.get();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		interfazzeListener = new ListenForMessageRequestBroacast();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Stopping interfazze");
		unregisterReceiver(interfazzeListener);
		isRunning.set(false);
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!isRunning.get()){
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			String interfazze_password = sp.getString(GeneralPreferences.INTERFAZZE_PASSWORD, "");
			if(interfazze_password.length()!=0){
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Starting interfazze");
				IntentFilter fltr = new IntentFilter();
				fltr.addAction(BroadcastEvents.NEW_EMAIL);
				fltr.addDataScheme("email"); 
				registerReceiver(interfazzeListener, fltr);
			}
			isRunning.set(true);
		}
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
