package pt.mashashi.ringsms.talk;

import java.security.GeneralSecurityException;
import java.util.HashMap;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.Crypto;
import pt.mashashi.ringsms.NotificationSuffixes;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.logged.LoggedActivity;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;

public class MsgNotifyBroadcast extends BroadcastReceiver {
	
	public static final String INCOMING_MESSAGE = "message"; 
	public static final String INCOMING_PHONE = "phone";
	public static final String INCOMING_STATUS = "status";
	
	// A mapping between the phone numbers and the pending notifications
	public static final HashMap<String, Integer> notifications = new HashMap<String, Integer>();
	public static final HashMap<String, ContentObserver> listenerContact = new HashMap<String, ContentObserver>();
	
	@Override
	public void onReceive(final Context ctx, Intent iten) {
		
		NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(iten.getAction().equals(BroadcastEvents.MESSAGE_RECEIVED)){
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			String phoneBuffer = null;
			try {
				String password = sp.getString(GeneralPreferences.ACCESS_APP_DATA_PASSWORD, null);
				phoneBuffer = Crypto.decrypt(iten.getExtras().getString(INCOMING_PHONE), password, "AES");
			} catch (GeneralSecurityException e) {
				// Ups! This isn't supposed to happen.
				return ;
			}
			final String phone = phoneBuffer;
			
			String identifier = getIdentifier(ctx, phone);
			
			// On notification click
			Intent intent = new Intent(ctx, LoggedActivity.class);
			intent.putExtra(LoggedActivity.THREAD_PHONE_NUMBER, phone);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			final PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
			
			Notification newMessage = new Notification();
			newMessage.icon = android.R.drawable.stat_notify_chat;
			newMessage.tickerText = String.format(ctx.getString(R.string.new_message_from), identifier);
			newMessage.when = System.currentTimeMillis();
			newMessage.setLatestEventInfo(ctx, ctx.getString(R.string.new_message), newMessage.tickerText, pIntent);
			newMessage.flags |= Notification.FLAG_AUTO_CANCEL; // Remove notification on click
			
			notifications.put(phone, (phone+NotificationSuffixes.RECEIVED_NOTIFICATION).hashCode());
			notificationManager.notify(notifications.get(phone), newMessage);
			
			
			// Setup contact information listener
			ContentObserver contentObserver = new ContentObserver(null) {
				@Override
				public void onChange(boolean selfChange) {
					super.onChange(selfChange);
					
					String identifier = getIdentifier(ctx, phone);
					
					Notification newMessage = new Notification();
					newMessage.icon = android.R.drawable.stat_notify_chat;
					newMessage.tickerText = String.format(ctx.getString(R.string.new_message_from), identifier);
					newMessage.when = System.currentTimeMillis();
					newMessage.setLatestEventInfo(ctx, ctx.getString(R.string.new_message), newMessage.tickerText, pIntent);
					newMessage.flags |= Notification.FLAG_AUTO_CANCEL; // Remove notification on click
					
					notifications.put(phone, (phone+NotificationSuffixes.RECEIVED_NOTIFICATION).hashCode());
					
					NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(notifications.get(phone), newMessage);
				}
			};
			listenerContact.put(phone, contentObserver);
			ctx.getContentResolver().registerContentObserver(ContactUtilsSingleton.getUriFromContactPhone(phone), true, contentObserver);
			
			
			Intent newMsgNotInt = new Intent(BroadcastEvents.NEW_MESSAGE_NOTIFICATION_PLACED);
			newMsgNotInt.putExtra(MsgRefreshBroadcast.REFRESH_NUMBER, phone);
			ctx.sendBroadcast(newMsgNotInt);
			
		} else if(iten.getAction().equals(BroadcastEvents.MESSAGE_READ)){
			String number = iten.getExtras().getString(MsgRefreshBroadcast.REFRESH_NUMBER);
			
			Integer phone = notifications.remove(number);
			ContentObserver contentObserver = listenerContact.remove(number);
			if(phone!=null && contentObserver!=null){
				ctx.getContentResolver().unregisterContentObserver(contentObserver);
				notificationManager.cancel(phone);
			}
			
		}
		
	}
	
	public String getIdentifier(Context ctx, String phone){
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
		Cursor c = ctx.getContentResolver().query(uri, new String[] { ContactsContract.Contacts.DISPLAY_NAME}, null, null, ContactUtilsSingleton.ORDER_POLICY);
		String identifier=phone;
		try {
	        if (c.moveToFirst()){ // Check if a contact was found
		        int index = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		        identifier = c.getString(index);
	        }
	    } finally {
	        if (c != null){ c.close(); }
	    }
		return identifier;
	}
}