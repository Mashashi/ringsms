package pt.mashashi.ringsms.talk;


import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.NotificationSuffixes;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Tone {
	
	public static Queue<Integer> queueCallTime = new LinkedList<Integer>();
	private static void agendHangupCall(int time){
		queueCallTime.add(time);
	}
	
	private List<SignalEnum> tone;
	public List<SignalEnum> getTone() { return tone; }
	
	
	
	
	
	public static final int RING_HAS_FINISHED = -1;
	public static final int MESSAGE_WAS_CANCELLED = -2;
	public static final int CHANNEL_HAS_BEEN_BROKEN = -3;
	
	
	
	
	public Tone(List<SignalEnum> tone){
		this.tone = tone;
	}
	
	public Tone(Tone tone){
		this.tone = new LinkedList<SignalEnum>(tone.getTone());
	}
	
	
	public void sendTone(boolean invert, String destinationNumber, int jitter, Context context, int numberOfRings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
		ThreadJitterEnum jitterEnum = ThreadJitterEnum.values()[jitter];
		for(SignalEnum sig: tone){
			if(invert){
				if(sig.equals(SignalEnum.ONE)){
					sig=SignalEnum.ZERO;
				}else if(sig.equals(SignalEnum.ZERO)){
					sig=SignalEnum.ONE;
				}
			}
			ringFor(sig.getFactor()*jitterEnum.getLengthInMilisecs(), destinationNumber, context, numberOfRings, givenRings);
		}
	}
	public static void sendEndTone(String destinationNumber, int jitter, Context context, int numberOfRings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
		ThreadJitterEnum jitterEnum = ThreadJitterEnum.values()[jitter];
		ringFor(jitterEnum.getLengthInMilisecs()*SignalEnum.END.getFactor(), destinationNumber, context,  numberOfRings, givenRings);
	}
	
	
	
	
	
	private static final int PAUSE_PERIOD_TO_AVOID_BLOCKING = 2000;
	private static void ringFor(int time, String destinationNumber, Context context, double numberOfRings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
		
		agendHangupCall(time);
		try {
			// With out this delay the phone will just perform the first or and second call then the process would hang
			// because the call wouldn't be made and we would just get hanging on waitForCallCompletion();
			Thread.sleep(PAUSE_PERIOD_TO_AVOID_BLOCKING);
		} catch (InterruptedException e) {}
		callNumber(destinationNumber, context);
		
		waitForCallCompletion();
		updateProgress(destinationNumber, context, numberOfRings, givenRings, 1);
		
	}
	public static void updateProgress(String destinationNumber, Context context, double numberOfRings, AtomicInteger givenRings, int increment){
		{ 	//Update progress
			
			givenRings.addAndGet(increment);
			Intent cancelIntent = new Intent(BroadcastEvents.MESSAGE_SENT_CANCEL);
			PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, 0);
			
			NotificationManager notificationManager = 
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	
			Notification newMessage = new Notification();
			newMessage.icon = android.R.drawable.ic_notification_clear_all;
			newMessage.tickerText = context.getString(R.string.cancel_message_being_send);
			newMessage.when = System.currentTimeMillis();
			long progressValue = Math.round((100*(((double)givenRings.get())/numberOfRings)));
			String progress = String.format(context.getString(R.string.message_being_send), progressValue);
			newMessage.setLatestEventInfo(context, progress, newMessage.tickerText, pIntent);
			newMessage.flags |= Notification.FLAG_AUTO_CANCEL;
			newMessage.flags |= Notification.FLAG_NO_CLEAR;
			
			// Same Id the view only gets updated
			notificationManager.notify((destinationNumber+NotificationSuffixes.SENDING_NOTIFICATION).hashCode(), newMessage);
		}
	}
	private static void callNumber(String destinationNumber, Context context){
		try {
	        Intent callIntent = new Intent(Intent.ACTION_CALL);
	        callIntent.setData(Uri.parse("tel:"+destinationNumber));
	        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        context.startActivity(callIntent);
	    } catch (ActivityNotFoundException e) {
	        Log.e(ThreadsActivity.DEBUG_TAG, "Call failed", e);
	    }
	}
	
	/**
	 * 
	 * @return True if the message being sent was cancelled, false otherwise
	 * @throws BrokenChannelException 
	 * @throws MessageCancelledException 
	 */
	private static void waitForCallCompletion() throws BrokenChannelException, MessageCancelledException{
		synchronized (Tone.class) {
			boolean isNotResponseCode = true;
			while(isNotResponseCode){
				try {
					Tone.class.wait();
				} catch (InterruptedException e) {}
				
				isNotResponseCode = (queueCallTime.peek()!=RING_HAS_FINISHED && 
									queueCallTime.peek()!=MESSAGE_WAS_CANCELLED && 
									queueCallTime.peek()!=CHANNEL_HAS_BEEN_BROKEN);
			}
		}
		Integer value = queueCallTime.poll();
		
		switch(value){
			case CHANNEL_HAS_BEEN_BROKEN:
			{
				throw new BrokenChannelException();
			}
			case MESSAGE_WAS_CANCELLED:
			{
				throw new MessageCancelledException();
			}
			default: break;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for(SignalEnum signal :tone){
			str.append(signal.getTag());
		}
		return str.toString();
	}
}
