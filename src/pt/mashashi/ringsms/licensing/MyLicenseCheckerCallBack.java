package pt.mashashi.ringsms.licensing;

import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.threads.ThreadsActivity;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;

public class MyLicenseCheckerCallBack implements LicenseCheckerCallback {
	
	private Object objNotify;
	private Integer reason;
	
	public MyLicenseCheckerCallBack(Object objNotify){
		this.objNotify = objNotify;
		reason = null;
	}
	
	// Policy.NOT_LICENSED
	// In theory the policy can return Policy.NOT_LICENSED here as well.
	@Override
	public void allow(int reason) {
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Allow");
		alertResult(Policy.LICENSED);
	}
	
	// Policy.LICENSED 
	// In theory the policy can return Policy.LICENSED here as well. Perhaps the call to the LVL took too long, for example.
	@Override
	public void dontAllow(int reason) {
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Dont Allow");
		switch(reason){
			case Policy.NOT_LICENSED:{	
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Not Licensed");
				alertResult(Policy.NOT_LICENSED);
				break;
			}
			case Policy.RETRY:{
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Retry");
				alertResult(Policy.RETRY);
				break;
			}
		}
	}
	
	@SuppressWarnings("unused") private static final int ERROR_NOT_MARKET_MANAGED = 0x3;
	@SuppressWarnings("unused") private static final int ERROR_SERVER_FAILURE = 0x4;
	@SuppressWarnings("unused") private static final int ERROR_OVER_QUOTA = 0x5;
	@SuppressWarnings("unused") private static final int ERROR_CONTACTING_SERVER = 0x101;
	@SuppressWarnings("unused") private static final int ERROR_INVALID_PACKAGE_NAME = 0x102;
	@SuppressWarnings("unused") private static final int ERROR_NON_MATCHING_UID = 0x103;
	@Override
	public void applicationError(int errorCode) {
		MyLog.d(ThreadsActivity.DEBUG_TAG, "App error "+errorCode);
	}
	
	private void alertResult(int reason){
		synchronized(objNotify){
			this.reason = reason;
			objNotify.notify();
		}
	}
	
	public Integer getReason(){
		return reason;
	}
	/*private void verificate(int reason){
		switch(reason){
		case Policy.NOT_LICENSED:{	
			MyLog.d(ThreadsActivity.DEBUG_TAG, "Not Licensed");
			break;
		}
		case Policy.RETRY:{
			MyLog.d(ThreadsActivity.DEBUG_TAG, "Retry");
			break;
		}
		case Policy.LICENSED:{
			MyLog.d(ThreadsActivity.DEBUG_TAG, "Licensed");
			break;
		}
		}
	}*/
}