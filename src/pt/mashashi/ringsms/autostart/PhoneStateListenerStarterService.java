package pt.mashashi.ringsms.autostart;

import java.util.concurrent.atomic.AtomicBoolean;

import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.talk.CallStateBroadcast;
import pt.mashashi.ringsms.talk.ServiceListenerSendMsg;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class PhoneStateListenerStarterService extends Service {
	
	private CallStateBroadcast callStateListener;
	
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	public static boolean getIsRunning(){
		return isRunning.get();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Service message received");
		callStateListener = new CallStateBroadcast();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Phone state listener unregistered");
		unregisterReceiver(callStateListener);
		notifyPointsOfProgram(false);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try{
			
			if(!isRunning.get()){
				registerReceiver(callStateListener, new IntentFilter("android.intent.action.PHONE_STATE"));
				notifyPointsOfProgram(true);
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Phone state listener registered");
			}else{
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Phone state listener not registered, it has been already");
			}
			
		}catch(Exception e){
			MyLog.d(ThreadsActivity.DEBUG_TAG, e.getMessage());
		}
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private void notifyPointsOfProgram(boolean running){
		synchronized(ServiceListenerSendMsg.class){
			synchronized(GeneralPreferences.class){
				isRunning.set(running);
				GeneralPreferences.class.notify();
				ServiceListenerSendMsg.class.notify();
			}
		}
	}
	
} 