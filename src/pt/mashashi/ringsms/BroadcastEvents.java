package pt.mashashi.ringsms;

public class BroadcastEvents {
	
	private static final String APP_IDENTIFIER = "pt.mashashi.ringsms";
	
	// Used by CallStateListener
	// in pt.mashashi.ringsms.talk to broadcast a received message
	public static final String MESSAGE_RECEIVED = APP_IDENTIFIER+".MSG_RECEIVED";
	// Used by ThreadsActivity
	// to receive broadcast signal from a the ChatActivity or one of its subclasses
	// to inform that the view that should be refreshed
	public static final String MESSAGE_REFRESH = APP_IDENTIFIER+".MSG_REFRESH";		
	
	public static final String MESSAGE_READ = APP_IDENTIFIER+".MSG_READ";
	
	// Used to cancel a call
	public static final String MESSAGE_SENT_CANCEL =  APP_IDENTIFIER+".MESSAGE_SENT_CANCEL";
	
	// Notification as been placed for incoming message
	public static final String NEW_MESSAGE_NOTIFICATION_PLACED =  APP_IDENTIFIER+".NEW_MSG_NOTIFICATION_PLACED";
	
	// New imported code map
	public static final String NEW_IMPORTED_CODE_MAP =  APP_IDENTIFIER+".NEW_IMPORTED_CODE_MAP";
	
	// New message from the K9 email client
	public static final String NEW_EMAIL = "com.fsck.k9.intent.action.EMAIL_RECEIVED";
	
}
