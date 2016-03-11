package pt.mashashi.ringsms.threads;

import java.util.Date;

import android.graphics.Bitmap;

public class ThreadDAO {

	private String name;		// This is not to be stored in the data base
	private Bitmap photo;		// This is not to be stored in the data base
	
	private String number;
	private int jitter;
	private int checkSum;
	private Date lastAccess;
	private String scratch;
	private long codeMappingsId;
	
	private String lastMsg;			// This is stored in the database but belongs to another table
	private Date timeStampLastMsg;	// This is stored in the database but belongs to another table
	private int threadMessageCount;
	
	/**
	 * 
	 * @param name
	 * @param number
	 * @param photo
	 * @param jitter In seconds, the end message code is 3 times this value
	 * @param lastMsg
	 * @param timeStampLastMsg
	 */
	public ThreadDAO(String name, String number, Bitmap photo, 
			int jitter, int checkSum, Date lastAccess,
			String lastMsg, Date timeStampLastMsg, int threadMessageCount, String scratch, long codeMappingsId){
		this.name = name;
		this.photo = photo;
		
		this.number = number;
		this.jitter = jitter;
		this.checkSum = checkSum;
		this.lastAccess = lastAccess;
		this.scratch = scratch;
		this.codeMappingsId=codeMappingsId;
		
		this.lastMsg = lastMsg;
		this.timeStampLastMsg = timeStampLastMsg;
		this.threadMessageCount = threadMessageCount;
	}

	public String getName() {
		return name;
	}
	public Bitmap getPhoto() {
		return photo;
	}
	
	public String getNumber() {
		return number;
	}
	public int getJitter() {
		return jitter;
	}
	public int getCheckSum() {
		return checkSum;
	}
	public Date getLastAccess() {
		return lastAccess;
	}
	public String getScratch() {
		return scratch;
	}
	public long getCodeMappingsId() {
		return codeMappingsId;
	}
	
	public String getLastMsg() {
		return lastMsg;
	}
	public Date getTimeStampLastMsg() {
		return timeStampLastMsg;
	}
	public int getThreadMessageCount(){
		return threadMessageCount;
	}
	
}
