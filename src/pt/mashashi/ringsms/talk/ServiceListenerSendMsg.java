package pt.mashashi.ringsms.talk;

import pt.mashashi.ringsms.autostart.PhoneStateListenerStarterService;

public class ServiceListenerSendMsg extends Thread {
	
	private static final long MAX_TIME = 200;
	private volatile boolean  finished;
	private long passedTime = System.currentTimeMillis();
	
	public ServiceListenerSendMsg() { finished = false; }
	
	@Override
	public void run() {
		synchronized(ServiceListenerSendMsg.class){
			try {
				{
					ServiceListenerSendMsg.class.wait(MAX_TIME);
					passedTime = System.currentTimeMillis()-passedTime;
				}while(!PhoneStateListenerStarterService.getIsRunning() && passedTime<MAX_TIME);
			} catch (InterruptedException e) { /* This is not supposed to happen */ }
		}
		synchronized(this){
			finished=true;
			notify();
		}
	}
	public void onUnblockGoOn(){
		long startWait = System.currentTimeMillis();
		long timePassed = 0;
		synchronized(this){
			while(!finished && timePassed<MAX_TIME){
				try {
					wait(MAX_TIME);
				} catch (InterruptedException e) {}
				timePassed = System.currentTimeMillis()-startWait;
			}
		}
	}
	
}