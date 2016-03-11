package pt.mashashi.ringsms.talk;

import pt.mashashi.ringsms.BroadcastEvents;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallCancelBroadcast extends BroadcastReceiver{

	private static boolean isCancelled = false;
	public static void reset(){
		synchronized (threadWraper) {
			isCancelled=false;
			threadWraper.threadWaiting=null;
		}
	}
	
	private final static ThreadWraper threadWraper = new ThreadWraper();
	/**
	 * 
	 * @param thread Cannot be null
	 */
	public static void interruptIfCancelled(Thread thread){
		synchronized (threadWraper) {
			threadWraper.threadWaiting = thread;
			if(isCancelled){
				threadWraper.threadWaiting.interrupt();
			}
		}
	}
	public static class ThreadWraper {
		public ThreadWraper(){}
		public Thread threadWaiting;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(BroadcastEvents.MESSAGE_SENT_CANCEL)){
			synchronized (threadWraper) {
				isCancelled=true;
				if(threadWraper.threadWaiting!=null){
					threadWraper.threadWaiting.interrupt();
				}
			}
		}	
	}

}
