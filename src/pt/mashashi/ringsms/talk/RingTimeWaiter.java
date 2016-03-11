package pt.mashashi.ringsms.talk;
class RingTimeWaiter extends Thread{
							
	private final int sleepFor;
	private final int ONE_SECOND = 1000;
	private int sleeped;
	
	private boolean wasInterrupted; // isInterrupted only works when the thread is alive
	
	public RingTimeWaiter(int sleepFor){
		this.sleepFor = sleepFor;
		this.sleeped = 0;
		this.wasInterrupted = false;
	}
	
	@Override
	public void run() {
		boolean interrupted = isInterrupted();
		try {
			while(!interrupted && sleeped<sleepFor){
				synchronized (this) { this.wait(ONE_SECOND); }
				interrupted = isInterrupted();
				sleeped+=ONE_SECOND;
			}
		} catch (InterruptedException e) { 
			/*User as cancelled the sent message*/
			interrupted = true;
		}
		if(interrupted){
			wasInterrupted=true;
		}
	}
	public boolean getWasInterrupted(){
		return wasInterrupted;
	}
}