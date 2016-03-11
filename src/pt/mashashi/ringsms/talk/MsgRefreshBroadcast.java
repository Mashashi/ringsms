package pt.mashashi.ringsms.talk;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.Crypto;
import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.MessageDataSource;
import pt.mashashi.ringsms.database.MsgStatusEnum;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MsgRefreshBroadcast extends BroadcastReceiver{
	
	private static List<OnNewRefreshMessageListener> listenersRefresh = new ArrayList<OnNewRefreshMessageListener>();
	public static boolean register(OnNewRefreshMessageListener listener){
		boolean added = false;
		synchronized(listenersRefresh){
			added = listenersRefresh.add(listener);
		}
		return added;
	}
	public static boolean unregister(OnNewRefreshMessageListener listener){
		boolean removed = false;
		synchronized(listenersRefresh){
			removed = listenersRefresh.remove(listener);
		}
		return removed;
	}
	private static void notifyAllNewMessage(String phone, MsgDirectionEnum direction){
		synchronized (listenersRefresh) {
			for(OnNewRefreshMessageListener refreshListener: listenersRefresh){
				refreshListener.onNewMessage(phone, direction);
			}
		}
	} 
	private static void notifyAllNotificationPlaced(String phone){
		synchronized (listenersRefresh) {
			for(OnNewRefreshMessageListener refreshListener: listenersRefresh){
				refreshListener.onNewMessageNotificationPlaced(phone);
			}
		}
	} 
	public MsgRefreshBroadcast(){}
	
	
	
	
	
	
	
	public final static String REFRESH_NUMBER = "number";
	public final static String REFRESH_DIRECTION = "direction";
	
	@Override
	public void onReceive(Context ctx, Intent intent) {
		
		
		if(intent.getAction().equals(BroadcastEvents.MESSAGE_RECEIVED)){
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
			String password = sp.getString(GeneralPreferences.ACCESS_APP_DATA_PASSWORD, null);
			
			String phone = intent.getExtras().getString(MsgNotifyBroadcast.INCOMING_PHONE);
			String msg=null;
			int status = 0;
			try {
				phone = Crypto.decrypt(phone,password,"AES");
				msg =  intent.getStringExtra(MsgNotifyBroadcast.INCOMING_MESSAGE);
				msg = Crypto.decrypt(msg,password,"AES");
				String statusBuffer =  intent.getStringExtra(MsgNotifyBroadcast.INCOMING_STATUS);
				status = Integer.parseInt(Crypto.decrypt(statusBuffer,password,"AES"));
			} catch (GeneralSecurityException e) {
				// Ups. This isn't supposed to happen.
				return;
			}
			Date now = Calendar.getInstance().getTime();
			
			int jitter = sp.getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
			int checkSum = sp.getInt(GeneralPreferences.CHECK_SUM, GeneralPreferences.PREFERENCE_DEFAULT_CHECK_SUM_LENGTH);
			ThreadDataSource.getInstance(ctx).insertThread(phone, jitter, checkSum, null);
			MessageDataSource.getInstance(ctx).insertMessage(phone, msg, MsgDirectionEnum.INCOMING, now, MsgStatusEnum.values()[status]);
			
			notifyAllNewMessage(phone, MsgDirectionEnum.INCOMING); // Notify threads interface and logged interface
		}else if(intent.getAction().equals(BroadcastEvents.MESSAGE_REFRESH)){
			String phone = intent.getExtras().getString(REFRESH_NUMBER);
			int directionIndex = intent.getExtras().getInt(REFRESH_DIRECTION);
			notifyAllNewMessage(phone, MsgDirectionEnum.values()[directionIndex]);
		}else if(intent.getAction().equals(BroadcastEvents.NEW_MESSAGE_NOTIFICATION_PLACED)){
			String phone = intent.getExtras().getString(REFRESH_NUMBER);
			notifyAllNotificationPlaced(phone);
		}
		
	}
	
	public static void sendRefreshNotice(Context ctx, String phoneNumber, MsgDirectionEnum direction){
		Intent intentEndMessage = new Intent(BroadcastEvents.MESSAGE_REFRESH);
		intentEndMessage.putExtra(MsgRefreshBroadcast.REFRESH_NUMBER, phoneNumber);
		if(direction!=null){
			intentEndMessage.putExtra(MsgRefreshBroadcast.REFRESH_DIRECTION, direction.ordinal());
		}
		ctx.sendBroadcast(intentEndMessage);
	}
	
}
