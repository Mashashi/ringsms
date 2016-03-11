package pt.mashashi.ringsms.interfazze;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.codemap.NewLineTokenizer;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.InterfazzeUsedNouncesDataSource;
import pt.mashashi.ringsms.database.RingSMSDBHelper;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.talk.SignalEnum;
import pt.mashashi.ringsms.talk.Tone;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadDAO;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

public class ListenForMessageRequestBroacast extends BroadcastReceiver{
	
	public final static String SUBJECT = "com.fsck.k9.intent.extra.SUBJECT";
	
	// Following the RFC a subject in a message should not be longer than 78 characters.
	
	public final static int MAX_SUBJECT_SIZE = 80;
	@SuppressLint("SimpleDateFormat")
	// DANGER! THIS IS AN DEV API METHOD CHANGE IT CAN LEAD TO INCOMPATIBILITIES IN PROGRAMS DEVELOPED TO RINGSMS
	private final static SimpleDateFormat TIME_STAMP = new SimpleDateFormat("yyyyMMddHHmm");
	private final static int MAX_DELAY_MESSAGE_REQUEST_MIN = 30; // TODO 30
	private final static int DELETE_NOUNCES_AFTER_DAYS = 50; //TODO 10
	
	public final static String PHONE_NUMBER_ACTIVITY_REQUEST_1 = "phoneInterfazze";
	public final static String MESSAGE_ACTIVITY_REQUEST_1 = "messageInterfazze";
	public final static String CODE_MAPPINGS_ACTIVITY_REQUEST_1 = "codeMAppingsInterfazze";
	
	@SuppressWarnings("serial")
	private static class InvalidMessageException extends Exception{
		public InvalidMessageException() {
			super();
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(intent.getAction().equals(BroadcastEvents.NEW_EMAIL)){
			
			String received = intent.getStringExtra(SUBJECT);
			
			if(received.length()>MAX_SUBJECT_SIZE) return;
			if(!received.matches("^\\d+\\|.*\\|\\d{12},(0|1|\\.)*,.*$")) return;
			
			InterfazzeUsedNouncesDataSource.getInstance(context).deleteAllExpired(DELETE_NOUNCES_AFTER_DAYS);
			
			String toSign = received.substring(0, received.lastIndexOf(",")+1);
			
			int nounce = Integer.parseInt(received.substring(0, received.indexOf("|")));
			
			String phone = received.substring(received.indexOf("|")+1, received.lastIndexOf("|"));
			if(!ContactUtilsSingleton.isPhoneNumber(phone)) return;
			
			String message = received.substring(received.indexOf(",")+1, received.lastIndexOf(","));
			String signature = received.substring(received.lastIndexOf(",")+1, received.length());
			
			{// Check message freshness
				String timestamp = received.substring(received.lastIndexOf("|")+1, received.indexOf(","));
				Calendar now = Calendar.getInstance();
				now.add(Calendar.MINUTE, -MAX_DELAY_MESSAGE_REQUEST_MIN);
				Date deadLinePast = now.getTime();
				try{
					Date sent = TIME_STAMP.parse(timestamp);
					if(sent.compareTo(deadLinePast)<0){
						// This message isn't fresh
						return;
					}
				} catch (ParseException e) {
					// Wrong format date string
					return;
				}
			}
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			String interfazze_password = sp.getString(GeneralPreferences.INTERFAZZE_PASSWORD, "");
			
			if(interfazze_password.length()==0)
				return;
			
			try {
				String sig = RotinesUtilsSingleton.doHmac(toSign, interfazze_password);
				if(!signature.equals(sig)){
					throw new GeneralSecurityException("Wrong signature");
				}
				
				SQLiteDatabase writer = RingSMSDBHelper.getInstance(context).getWritableDatabase();
				writer.beginTransaction();
				boolean exists =InterfazzeUsedNouncesDataSource.getInstance(context).existsNounce(nounce);
				if(exists){
					throw new GeneralSecurityException("The nounce is invalid");
				}
				boolean inserted = InterfazzeUsedNouncesDataSource.getInstance(context).insertNounce(nounce);
				if(!inserted){
					throw new GeneralSecurityException("Could not insert nounce");
				}
				writer.setTransactionSuccessful();
				writer.endTransaction();
				
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Send message: "+message+" to "+phone);
				
				
				// Get the used alphabet
				String separator = "";
				long idCodeMappings = 0;
				BiMap<String, String> alphabet = HashBiMap.create();
				{
					writer.beginTransaction();
					ThreadDAO thread = ThreadDataSource.getInstance(context).getThread(phone);
					if(thread!=null && thread.getCodeMappingsId()!=0){
						idCodeMappings = thread.getCodeMappingsId();
						alphabet.putAll(CodeMappingsDataSource.getInstance(context).getCodeMapping(idCodeMappings).getCodesMapping(context, true));
						separator = NewLineTokenizer.SEPARATOR_STR;
					}else{
						Map<String, Tone> alphabetBuffer = SignalEnum.getAlphabet(context);
						alphabet.putAll(Maps.transformEntries(alphabetBuffer,  new Maps.EntryTransformer<String, Tone, String>(){
							@Override public String transformEntry(String k, Tone v) {
								return v.toString();
							}
						}));
						alphabet=alphabet.inverse();
					}
					
					writer.setTransactionSuccessful();
					writer.endTransaction();
				}
				
				// Parse the codes
				StringBuilder finalMessage = new StringBuilder("");
				{
					String[] codesToSend = message.split("\\.");
					for(String code: codesToSend){
						String text = alphabet.get(code);
						if(text!=null){
							finalMessage.append(text);
							finalMessage.append(separator);
						}else{
							throw new InvalidMessageException();
						}
					}
				}
				
				
				Intent intentNew = new Intent(context, ThreadsActivity.class);
				intentNew.putExtra(PHONE_NUMBER_ACTIVITY_REQUEST_1, phone);
				intentNew.putExtra(MESSAGE_ACTIVITY_REQUEST_1, finalMessage.toString());
				intentNew.putExtra(CODE_MAPPINGS_ACTIVITY_REQUEST_1, idCodeMappings);
				intentNew.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intentNew.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intentNew);
				
			} catch (GeneralSecurityException e) {
				MyLog.d(ThreadsActivity.DEBUG_TAG, "");
			} catch (UnsupportedEncodingException e) {
				MyLog.d(ThreadsActivity.DEBUG_TAG, "");
			} catch(InvalidMessageException e)  {
				MyLog.d(ThreadsActivity.DEBUG_TAG, "");
			}
			
		}
		
	}
	
}
