package pt.mashashi.ringsms.database;

public enum MsgStatusEnum {
	// In sending or receiving
	SUCCESS,	 
	
	// Receiving
	RECEIVED_WITH_ERROR,
	URGENT_RECEIVED,
	
	// Sending
	SEND_CANCELLED,
	CHANNEL_BROKEN,
	URGENT_SENT;
}
