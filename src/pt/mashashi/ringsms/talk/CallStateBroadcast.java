package pt.mashashi.ringsms.talk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.Crypto;
import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.codemap.NewLineTokenizer;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.RingSMSDBHelper;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import pt.mashashi.ringsms.database.MsgStatusEnum;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadDAO;
import pt.mashashi.ringsms.threads.ThreadsActivity;

import com.android.internal.telephony.ITelephony;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * 
 * Note that: 
 * + There is no way to check if the recipient as answered the call.
 */
public class CallStateBroadcast extends BroadcastReceiver {

	// Fields related to outgoing message
	private static ArrayListMultimap<String, Long> callTryCounter = ArrayListMultimap.create();
	private static HashMap<String,StringBuilder> message = new HashMap<String,StringBuilder>();
	private static HashMap<String,ThreadJitterEnum> jitter = new HashMap<String,ThreadJitterEnum>();
	private static HashMap<String,Long> lastSignalTimeStamp = new HashMap<String,Long>();
	private static HashMap<String,Boolean> swapBits = new HashMap<String,Boolean>();
	private static HashMap<String,Map<String, Tone>> codeMapInUse	= new HashMap<String,Map<String, Tone>>();
	private static HashMap<String, Thread> notMessageNotifier = new HashMap<String, Thread>();
	private static HashMap<String, Boolean> isUrgent = new HashMap<String, Boolean>();

	// Fields related to incoming message with the use of this variables we can detect if the call was not hanged up by the program it self
	public final static AtomicBoolean isPhoneSendingMsg = new AtomicBoolean(false);
	public static final int MAXIMUM_TIME_INTERVAL_SECS = (int) 30;	// This time has to be at least long enough to accommodate the biggest jitter of 10 seconds. It was chosen 30 to accommodate some eventual bit lost.

	public CallStateBroadcast(){}

	//private int signatureSHA1hash	= "C1:1A:17:1A:1A:39:F0:D2:74:09:C2:9A:28:80:D5:A2:AA:8F:F4:1B".hashCode();
	//private int signatureMD5hash 	= "3E:A6:21:C3:31:1D:FF:CB:ED:8F:1E:95:61:BA:F1:4B".hashCode();
	// Making sure the application is not debuggable
	// When the application is in release mode it is not debuggable. The result is "Is debuggable:false"
	/*boolean isDebuggable =(0!=(ctx.getApplicationInfo().flags&=ApplicationInfo.FLAG_DEBUGGABLE));
	MyLog.d(ThreadsActivity.DEBUG_TAG, "Is debuggable:"+isDebuggable);*/

	private final int PUBLIC_KEY_MASHASHI_HASH = -621827995;

	// Lets assume it takes sometime to hang up to the cell 
	// phone that is receiving the message perceives the hang up
	public final static int TIME_FOR_RECEIVER_PERCEIVE_HANGUP=2; 
	public final static int TIMEOUT_MILISEC = (2*ThreadJitterEnum.HUGE.getLength()+TIME_FOR_RECEIVER_PERCEIVE_HANGUP)*1000;

	@Override
	public void onReceive(final Context ctx, Intent intent) {

		final String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		final String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

		// Anti resign package
		// If this package was altered and resigned this should be enough to detect it
		// This is obfuscated on purpose to make it harder for code analyzing tools to break our code

		/*try {
			String encCer = Crypto.encrypt("generateCertificate", GUARD_FUNCTION_NAME_KEY);
			String encPubKey = Crypto.encrypt("getPublicKey", GUARD_FUNCTION_NAME_KEY);
			String encPM = Crypto.encrypt("getPackageManager", GUARD_FUNCTION_NAME_KEY);
			String encPI = Crypto.encrypt("getPackageInfo", GUARD_FUNCTION_NAME_KEY);
		} catch (GeneralSecurityException e1) { e1.printStackTrace(); }*/

		try {

			String gpm=ctx.getString(R.string.gpm);
			String gps=ctx.getString(R.string.gps);
			String gpi=ctx.getString(R.string.gpi);
			String gc=ctx.getString(R.string.gc);
			String gpk=ctx.getString(R.string.gpk);
			String what=ctx.getString(R.string.what);
			String cer=ctx.getString(R.string.cer);

			String alg = ThreadsActivity.checkIntegrity(gps, gpm, gpi, gc, gpk, what, cer);

			String getPM = Crypto.decrypt(gpm, gps, alg); //getPackageManager
			String getPI = Crypto.decrypt(gpi, gps, alg); // getPackageInfo
			Method gpmMethod = ctx.getClass().getMethod(getPM);
			Object gpmObject = gpmMethod.invoke(ctx); // PackageManager packageManager = ctx.getPackageManager();
			Method gpiMethod = gpmObject.getClass().getMethod(getPI, String.class, int.class); // PackageInfo info = packageManager.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);

			Signature[] signs = ((PackageInfo)gpiMethod.invoke(gpmObject, ctx.getPackageName(), 64)).signatures;//PackageManager.GET_SIGNATURES=64
			CertificateFactory cf = CertificateFactory.getInstance(Crypto.decrypt(cer, gps, alg)); //"X.509"

			String getCerFact = Crypto.decrypt(ctx.getString(R.string.gc), ctx.getString(R.string.gps), alg); // generateCertificate
			Method cerFact = cf.getClass().getMethod(getCerFact, InputStream.class);
			X509Certificate cert = (X509Certificate)cerFact.invoke(cf, new ByteArrayInputStream(signs[0].toByteArray())); //cf.generateCertificate(new ByteArrayInputStream(signs[0].toByteArray()));

			String getPubKey = Crypto.decrypt(ctx.getString(R.string.gpk), ctx.getString(R.string.gps), alg); // getPublicKey
			Method pubKeyGetter = X509Certificate.class.getMethod(getPubKey);
			PublicKey key = (PublicKey) pubKeyGetter.invoke(cert); // cert.getPublicKey()

			int modulusHash = ((RSAPublicKey)key).getModulus().hashCode();

			switch(modulusHash){
			    case PUBLIC_KEY_MASHASHI_HASH:{
			    	MyLog.d(ThreadsActivity.DEBUG_TAG, "Signature test passed");
			    	break;
			    }
			    default: return;
			}
		} catch (Exception e) {
			// If an error occur assume it is an hacker
			return;
		}

		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			// This code will execute when the phone has an incoming call
			MyLog.d(ThreadsActivity.DEBUG_TAG, "EXTRA_INCOMING_NUMBER");

			if(notMessageNotifier.get(incomingNumber)==null){
				// First ring
				// This should be cancelled at the end of the first ring in case it rang for less than
				Thread notifyInCaseNotMessage = new Thread(new Runnable() {
					AtomicInteger integer = new AtomicInteger(0);
					@Override
					public void run() {
						Integer id = -1;
						NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
						try {
							Thread.sleep(TIMEOUT_MILISEC);
							// null would result in a IllegalArgumentException
							PendingIntent pIntent = PendingIntent.getActivity(ctx.getApplicationContext(), 0, new Intent(), 0);

							id = ("real message"+integer.addAndGet(1)).hashCode();


							Notification newMessage = new Notification();
							newMessage.icon = android.R.drawable.stat_sys_phone_call;
							newMessage.tickerText = ctx.getString(R.string.real_call_description);
							newMessage.when = System.currentTimeMillis();

							String title = ctx.getString(R.string.real_call_title);
							newMessage.setLatestEventInfo(ctx, title, newMessage.tickerText, pIntent);
							//newMessage.flags |= Notification.FLAG_AUTO_CANCEL;
							//newMessage.flags |= Notification.FLAG_NO_CLEAR;
							newMessage.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
							// Same Id the view only gets updated
							notificationManager.notify(id, newMessage);
							synchronized (this) {
								while(true){
									this.wait();
								}
							}
						} catch (InterruptedException e) {
							// Drop dead, remove notification if it is showing
							notificationManager.cancel(id);
						}
					}
				});
				notifyInCaseNotMessage.start();
				notMessageNotifier.put(incomingNumber,notifyInCaseNotMessage);
				isUrgent.put(incomingNumber, false);
			}

			{
				Long lastSignalTimeStampStart = lastSignalTimeStamp.get(incomingNumber);
				long lastSignalGapTime = -1;
				if(lastSignalTimeStampStart!=null)
					lastSignalGapTime = getDelayInSecsSince(lastSignalTimeStampStart);
				if(lastSignalGapTime>MAXIMUM_TIME_INTERVAL_SECS){
					// Last signals were invalid but this may be isn't
					cleanUp(incomingNumber);
				}

			}
			callTryCounter.put(incomingNumber, System.currentTimeMillis());

			//


		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) { 
			// This code will execute when the call is disconnected when calling or being called
			MyLog.d(ThreadsActivity.DEBUG_TAG, "EXTRA_STATE_IDLE");

			// Check if the call that as terminated was an on going phone call related to a message
			// In that case is assumed that the connection as been broken
			if(isPhoneSendingMsg.get()){
				Intent cancelIntent = new Intent(BroadcastEvents.MESSAGE_SENT_CANCEL);
				ctx.sendBroadcast(cancelIntent);
			}

			List<Long> listTones = callTryCounter.get(incomingNumber);
			if(listTones!=null && !listTones.isEmpty()){ // Check if the call is incoming, i.e the code of EXTRA_STATE_IDLE was executed

				boolean cleanIt = false;
				//List<Integer> listCheck = checkSum.get(incomingNumber);
				ThreadJitterEnum jitterPhone = jitter.get(incomingNumber);
				StringBuilder strBuilder = getStringBuilder(incomingNumber);

				long callTimeSecs = getDelayInSecsSince(listTones.remove(listTones.size()-1));
				listTones.add(callTimeSecs);
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Try call time (s): "+callTimeSecs);

				if(swapBits.get(incomingNumber)==null){

					// First bit is swap indication
					SignalEnum signal = SignalEnum.getSignal(callTimeSecs, ThreadJitterEnum.biggestJitterEnum(), false);
					if(signal.equals(SignalEnum.ZERO)){
						swapBits.put(incomingNumber, false);
					}else if(signal.equals(SignalEnum.ONE)){
						swapBits.put(incomingNumber, true);
					}else{
						// Not valid
						cleanIt = true;
					}

					// Cancel the notification by this point the user hanged up or it is a valid message
					Thread notMessage = notMessageNotifier.get(incomingNumber);
					if(notMessage!=null){
						notMessage.interrupt();
					}
					listTones.remove(0); // Should solve [16, 5, 5, 5, 5, 6, 5, 5, 5, 23] 

				}else{
					boolean invert = swapBits.get(incomingNumber);

					if(jitterPhone==null){
						// Process the possible jitter

						SignalEnum signal = SignalEnum.getSignal(callTimeSecs, ThreadJitterEnum.biggestJitterEnum(), invert);
						SignalEnum signalDefaultJitter = SignalEnum.getSignal(callTimeSecs, ThreadJitterEnum.LONG, invert);
						int indexJitter = calculateIndexJitter(incomingNumber, invert);
						boolean indexValidJitter = indexJitter<ThreadJitterEnum.values().length && indexJitter>=0;

						if(signal.equals(SignalEnum.END) 
								|| (!indexValidJitter && signalDefaultJitter.equals(SignalEnum.END))){

							if(strBuilder.length()!=0){	
								// End of jitter transmission
								jitterPhone = processJitter(incomingNumber, indexValidJitter, indexJitter);
							}else{ 
								// Message is invalid
								cleanIt = true; 
							}

						}else{
							if(jitterPhone==null){
								strBuilder.append(signal.getTag());
								MyLog.d(ThreadsActivity.DEBUG_TAG, "(Receiving) Possible jitter is "+strBuilder);
							}
						}

					}

					if(jitterPhone!=null){
						// Process the message
						SignalEnum signal = SignalEnum.getSignal(callTimeSecs, jitterPhone, invert);

						if(signal.equals(SignalEnum.END)){
							StringBuilder message = getStringBuilder(incomingNumber);

							processMapInUse(ctx, incomingNumber, invert);
							final String separator = codeMapInUse.get(incomingNumber)!=null?NewLineTokenizer.SEPARATOR_STR:"";

							boolean finalizing = true;
							while(process(ctx, incomingNumber, separator, invert, finalizing)!=null){
								if(finalizing){ finalizing=false; }
							}

							// Process checksum
							int checksum=processCheckSum(incomingNumber, invert);

							if(message.length()!=0){
								sendBroadcastMessageReceived(ctx, incomingNumber, checksum);
							}else{
								/* This ringing sequence was not a message */	
							}
							cleanIt=true;
						} else { /*A message can*/
							processMapInUse(ctx,incomingNumber, invert);
							final String separator = codeMapInUse.get(incomingNumber)!=null?NewLineTokenizer.SEPARATOR_STR:"";

							process(ctx, incomingNumber, separator, invert, false);
						}
					}
				}
				if(cleanIt){
					cleanUp(incomingNumber);
				}else{
					lastSignalTimeStamp.remove(incomingNumber);
					lastSignalTimeStamp.put(incomingNumber, System.currentTimeMillis());
				}
			} else {

				// This will happen when a incoming call that as been picked up is hanged up
				// Here we can not access which was the phone the user picked up so the solution is cancell 
				// All notifications relative to a real call
				for(Thread t: notMessageNotifier.values()){
					t.interrupt();
				}

			}

		} else if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){ // This code will be execute when the call is outgoing
			MyLog.d(ThreadsActivity.DEBUG_TAG, "EXTRA_STATE_OFFHOOK");

			final Integer callTime = Tone.queueCallTime.peek();
			if(		callTime!=null								// Check if this call was initiated by the program
					&& callTime!=Tone.RING_HAS_FINISHED			// And that the program has already received the signal
					&& callTime!=Tone.MESSAGE_WAS_CANCELLED
					&& callTime!=Tone.CHANNEL_HAS_BEEN_BROKEN	// ...
					){

				isPhoneSendingMsg.set(true);

				Tone.queueCallTime.poll();
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Hangingup in: "+callTime+"ms");

				new Thread(){
					@Override
					public void run() {

						RingTimeWaiter waiter = new RingTimeWaiter(callTime);
						waiter.start();
						CallCancelBroadcast.interruptIfCancelled(waiter);

						try {
							waiter.join();
						} catch (InterruptedException e) {
							MyLog.d(ThreadsActivity.DEBUG_TAG, "Message cancelled or connection broken");
						}

						// Moving this line of code to some point after the call has been disconnected
						// by the program will introduce a bug
						isPhoneSendingMsg.set(false);
						boolean connectionBroken = hangupCall(ctx);


						if(connectionBroken){
							sendBrokenSignal();
						}else if(waiter.getWasInterrupted()){ 
							sendCancelSignal();
						}else{ 
							sendFinishSignal();
						}
					};
				}.start();

			}

		}
	}


	// TelephonyManager.EXTRA_STATE_IDLE Methods
	private void processMapInUse(Context ctx, String incomingNumber, boolean invert){

		boolean evaluated = codeMapInUse.containsKey(incomingNumber);

		if(!evaluated){

			List<Long> tonesCalcMap = callTryCounter.get(incomingNumber);
			ThreadJitterEnum jitterEnumCalcMap = jitter.get(incomingNumber);
			SignalEnum signal = SignalEnum.getSignal(tonesCalcMap.get(0),jitterEnumCalcMap, invert);

			if(signal.equals(SignalEnum.ZERO)){
				Map<String,Tone> mapInUse = codeMapInUse.get(isPhoneSendingMsg);
				if(mapInUse==null){

					Map<String, String> codeMapRaw = HashBiMap.create();

					RingSMSDBHelper instance = RingSMSDBHelper.getInstance(ctx);
					SQLiteDatabase writer = instance.getWritableDatabase();
					writer.beginTransaction();
					ThreadDAO incomingThread = ThreadDataSource.getInstance(ctx).getThread(incomingNumber);
					if(incomingThread!=null){
						codeMapRaw = CodeMappingsDataSource.getInstance(ctx).listCodeMapping(incomingThread.getCodeMappingsId());
					}else{
						codeMapRaw = HashBiMap.create();
					}
					writer.setTransactionSuccessful();
					writer.endTransaction();

					codeMapInUse.put(incomingNumber, SignalEnum.convertToToneMap(codeMapRaw));
				}
			}else{
				codeMapInUse.put(incomingNumber, null);
			}
			tonesCalcMap.remove(0);
		}




	}
	private int calculateIndexJitter(String incomingPhone, boolean inverted){
		StringBuilder strBuilder = getStringBuilder(incomingPhone);
		int indexJitter=-1;
		{
			//String zeroSymbol = inverted?"1":"0";
			//String oneSymbol = inverted?"0":"1";

			final int jittersLength = ThreadJitterEnum.values().length-1;
			String binStringMaxJitter = Integer.toBinaryString(jittersLength);
			final int maxBinaryStringBits = binStringMaxJitter.length();	
			boolean isValid	= strBuilder.length()!=0;
			isValid = isValid && strBuilder.length()-1/*code map bit still on buffer*/<=maxBinaryStringBits; 
			//isValid = isValid && !strBuilder.toString().matches("^("+zeroSymbol+"|"+oneSymbol+")"+zeroSymbol+zeroSymbol+"$");
			if(isValid){
				indexJitter = Integer.parseInt(strBuilder.toString(), 2);
			}
		}
		return indexJitter;
	}
	private int processCheckSum(String incomingNumber, boolean invert){
		int checksum=-1;
		ThreadJitterEnum jitterPhone = jitter.get(incomingNumber);
		List<Long> checkSumTones = callTryCounter.get(incomingNumber);
		if(checkSumTones.size()<=(GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE+1)){
			StringBuilder checkBuilder = new StringBuilder("");
			final int ORIGINAL_SIZE_CHECK_SUM_TONES_LIST = checkSumTones.size();
			for(int i=0;i<ORIGINAL_SIZE_CHECK_SUM_TONES_LIST-1;i++){
				checkBuilder.append(SignalEnum.getSignal(checkSumTones.get(0), jitterPhone, invert).getTag());
				checkSumTones.remove(0);
			}
			if(checkBuilder.length()!=0)
				checksum=Integer.parseInt(checkBuilder.toString(), 2);
		}
		return checksum;
	}
	private ThreadJitterEnum processJitter(String incomingNumber, boolean indexJitterValid, int indexJitter){
		StringBuilder strBuilder = getStringBuilder(incomingNumber);
		ThreadJitterEnum jitterPhone = jitter.get(incomingNumber);
		List<Long> listTones = callTryCounter.get(incomingNumber);
		if(indexJitterValid){
			ThreadJitterEnum jitterEnum = ThreadJitterEnum.values()[indexJitter];
			MyLog.d(ThreadsActivity.DEBUG_TAG, "(Received) Jitter is "+jitterEnum.toString());
			jitter.put(incomingNumber, jitterEnum);
			listTones.removeAll(listTones);
			strBuilder.delete(0, strBuilder.length());
		}else{
			// This message as the default jitter of JiiterEnum.LONG
			MyLog.d(ThreadsActivity.DEBUG_TAG, "(Received) This message as the default jitter of "+ThreadJitterEnum.LONG);
			jitterPhone = ThreadJitterEnum.LONG;
			jitter.put(incomingNumber, jitterPhone);
			strBuilder.delete(0, strBuilder.length());
		}
		return jitterPhone;
	}

	private void sendBroadcastMessageReceived(Context ctx, String incomingNumber, int checksum){

		Intent intentEndMessage = new Intent(BroadcastEvents.MESSAGE_RECEIVED);
		StringBuilder messageForPhone = message.get(incomingNumber);
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Message from "+incomingNumber+"! Message is: "+messageForPhone);

		final int reminderOf = (int) (Math.pow(GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE, 2)-1);
		final int messageCheckSum = (Math.abs(messageForPhone.toString().hashCode()%reminderOf));

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		String passToEncrypt = sp.getString(GeneralPreferences.INTERFAZZE_PASSWORD, "");
		if(passToEncrypt.length()==0){
			passToEncrypt = UUID.randomUUID().toString().replaceAll("-", "");
		}else{
			passToEncrypt=RotinesUtilsSingleton.transformIn256BitHexKey(passToEncrypt);
		}
		Editor editor = sp.edit();
		editor.putString(GeneralPreferences.ACCESS_APP_DATA_PASSWORD, passToEncrypt);
		editor.commit();

		boolean checkSumDetectedError = checksum!=messageCheckSum&&checksum!=-1;

		/*The end signal is still in the list*/
		int status = MsgStatusEnum.SUCCESS.ordinal();
		if(isUrgent.get(incomingNumber)){
			status = MsgStatusEnum.URGENT_RECEIVED.ordinal();
		}
		if(callTryCounter.get(incomingNumber).size()!=1  || checkSumDetectedError){
			// This message has an error
			// An error occurred while transmitting because symbols were left for decoding after receiving a end
			// or because the checksum is incorrect
			status = MsgStatusEnum.RECEIVED_WITH_ERROR.ordinal();
		}
		try {
			intentEndMessage.putExtra(MsgNotifyBroadcast.INCOMING_STATUS, Crypto.encrypt(status+"", passToEncrypt, "AES"));
			intentEndMessage.putExtra(MsgNotifyBroadcast.INCOMING_MESSAGE, Crypto.encrypt(messageForPhone.toString()+"", passToEncrypt, "AES"));
			intentEndMessage.putExtra(MsgNotifyBroadcast.INCOMING_PHONE, Crypto.encrypt(incomingNumber+"", passToEncrypt, "AES"));
			ctx.sendBroadcast(intentEndMessage);
		} catch (GeneralSecurityException e) {
			// Ups!!! This isn't supposed to happen!
		}
	}

	private String process(Context ctx, String incomingNumber, String separator, final boolean invert, boolean finalizing){
		List<Long> listTones = callTryCounter.get(incomingNumber);
		final ThreadJitterEnum jitterEnum = jitter.get(incomingNumber);

		String recognized = null;

		Map<String, Tone> inUse = codeMapInUse.get(incomingNumber);
		Map<String, Tone> alphabet = inUse==null?SignalEnum.getAlphabet(ctx):inUse;

		int signalsInBuffer = 0;
		if(listTones.size()!=0){
			SignalEnum lastSignal = SignalEnum.getSignal(listTones.get(listTones.size()-1), jitterEnum, invert);
			signalsInBuffer = listTones.size()-(lastSignal.equals(SignalEnum.END)?1:0)/*end tone*/;
		}

		final int codeSize = inUse!=null?SignalEnum.CODE_SIZE:SignalEnum.CHARACTER_SIZE_RINGS_DEFAULT;
		Long zero = (long) ((invert?2*jitterEnum.getLength():jitterEnum.getLength())-2/*Just to make sure this is zero*/);
		{	

			// finalizing=true
			// If the number of bits in the is equal to codeSize let the normal function handle it as normal
			// If the number of bits in the is smaller than codeSize let the normal function handle it as normal as finalizing is true, maybe it is urgent
			// If the number of bits in the is bigger than codeSize

			// If this is not the first iteration lets assume the data is compressed if it isn't no worries the code will handle it

			boolean decompress = signalsInBuffer>codeSize || (!finalizing && signalsInBuffer>=SignalEnum.MINIMUM_CODE_LENGTH_AFTER_COMPRESSION);

			if(decompress){ // This function is unavailable for message with just one code
				// Interpret 1 as 00 if there is not a word starting by 1
				int pos = 0;
				for(int i=0;i<listTones.size() && pos<listTones.size() && recognized==null;i++){
					boolean isNotEnd = !SignalEnum.getSignal(listTones.get(pos), jitterEnum, invert).equals(SignalEnum.END);
					if(isNotEnd){
						boolean applied = replaceInvalidOnes(zero, listTones, alphabet, pos, jitterEnum, invert);
						pos+=applied?2:1;
					}

					if(pos%codeSize==0){
						recognized = recognizedIt(incomingNumber, separator, invert, listTones, jitterEnum, alphabet);
					}
				}
			}
		}


		if(recognized==null){

			if(finalizing && getStringBuilder(incomingNumber).length()==0){
				// Urgent function
				boolean appendZeros = listTones.size()!=1; 			
				appendZeros=appendZeros&&signalsInBuffer<codeSize; 

				if(appendZeros){
					isUrgent.put(incomingNumber, true);
					do{
						listTones.add(0, zero);
					}while(listTones.size()-1/*end tone*/!=codeSize);
				}

			}
			recognized = recognizedIt(incomingNumber, separator, invert, listTones, jitterEnum, alphabet);
		}

		return recognized;
	}


	private String recognizedIt(String incomingNumber, String separator, 
			final boolean invert, List<Long> listTones,
			final ThreadJitterEnum jitterEnum, Map<String, Tone> alphabet) {

		String recognized = SignalEnum.parseString(listTones, alphabet, jitterEnum, invert);
		if(recognized!=null){
			StringBuilder strBuilder = getStringBuilder(incomingNumber);
			strBuilder.append(recognized);
			strBuilder.append(separator);
		}
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Recognized: "+recognized);
		return recognized;
	}

	public boolean replaceInvalidOnes(Long zero, List<Long> listTones, Map<String, Tone> alphabet, 
			int index, final ThreadJitterEnum jitterEnum, final boolean invert){
		boolean applied= false;
		List<Long> listTonesProcessing = listTones.subList(0, index+1);
		Long last = listTonesProcessing.get(index);
		SignalEnum lastSig = SignalEnum.getSignal(last, jitterEnum, invert);
		if(lastSig.equals(SignalEnum.ONE)){
			final List<SignalEnum> start =Lists.transform(listTonesProcessing, new Function<Long, SignalEnum>() {
				@Override
				public SignalEnum apply(Long arg) { return SignalEnum.getSignal(arg, jitterEnum, invert); }
			});
			Map<String, Tone> matched = Maps.filterValues(alphabet, new Predicate<Tone>() {
				@Override
				public boolean apply(Tone arg) {
					//if(arg.getTone().size()>start.size()){}return false;
					List<SignalEnum> matchesStart = arg.getTone().subList(0, start.size());
					return matchesStart.equals(start);
				}
			});
			if(matched.size()==0){
				listTones.remove(index); // Remove last
				listTones.add(index, zero);
				listTones.add(index,zero);
				applied = true;
			}
		}
		return applied;
	}

	private long getDelayInSecsSince(long startTime){
		double elapseTimeInMilisecs = (double)(System.currentTimeMillis()-startTime);
		long elapsedTimeSecs = Math.round(elapseTimeInMilisecs/((double)1000));
		return elapsedTimeSecs;
	}

	private static StringBuilder getStringBuilder(String phone){
		StringBuilder result = message.get(phone);
		if(result==null){
			result = new StringBuilder("");
			message.put(phone, result);
		}
		return result;
	}


	private static void cleanUp(String incomingNumber){
		message.remove(incomingNumber);
		jitter.remove(incomingNumber);
		lastSignalTimeStamp.remove(incomingNumber);
		List<Long> listTones = callTryCounter.get(incomingNumber);
		listTones.removeAll(listTones);
		codeMapInUse.remove(incomingNumber);
		swapBits.remove(incomingNumber);
		notMessageNotifier.remove(incomingNumber);
		isUrgent.remove(incomingNumber);
	}







	// TelephonyManager.EXTRA_STATE_OFFHOOK Methods
	private static void sendBrokenSignal(){
		synchronized (Tone.class) {
			Tone.queueCallTime.add(Tone.CHANNEL_HAS_BEEN_BROKEN);
			Tone.class.notify();
		}
	}
	private static void sendFinishSignal(){
		synchronized (Tone.class) {// Notify for call completion
			Tone.queueCallTime.add(Tone.RING_HAS_FINISHED);
			Tone.class.notify();
		}
	}
	private static void sendCancelSignal(){
		synchronized (Tone.class) {
			Tone.queueCallTime.add(Tone.MESSAGE_WAS_CANCELLED);
			Tone.class.notify();
		}
	}

	private static boolean hangupCall(Context context){

		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean cancel = true;

		try {
			// Java reflection to gain access to TelephonyManager's
			// ITelephony getter
			Class<?> c = Class.forName(tm.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			final ITelephony telephonyService = (ITelephony) m.invoke(tm);

			try {

				cancel = !telephonyService.endCall();

			} catch (RemoteException e) {
				e.printStackTrace();
				Log.e(ThreadsActivity.DEBUG_TAG, "Failing to end call");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(ThreadsActivity.DEBUG_TAG, "Could not connect to telephony subsystem");
		}

		return cancel;
	}



}