package pt.mashashi.ringsms.chat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.NotificationSuffixes;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.autostart.PhoneStateListenerStarterService;
import pt.mashashi.ringsms.codemap.CodeMappingsDAO;
import pt.mashashi.ringsms.codemap.CodeMappingsEditListActivity;
import pt.mashashi.ringsms.codemap.MultiTextWatcher;
import pt.mashashi.ringsms.codemap.MultiTextWatcher.DeleteTokenAction;
import pt.mashashi.ringsms.codemap.NewLineTokenizer;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.RingSMSDBHelper;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import pt.mashashi.ringsms.database.MessageDataSource;
import pt.mashashi.ringsms.database.MsgStatusEnum;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.talk.BrokenChannelException;
import pt.mashashi.ringsms.talk.CallCancelBroadcast;
import pt.mashashi.ringsms.talk.MessageCancelledException;
import pt.mashashi.ringsms.talk.MsgRefreshBroadcast;
import pt.mashashi.ringsms.talk.ServiceListenerSendMsg;
import pt.mashashi.ringsms.talk.Tone;
import pt.mashashi.ringsms.talk.SignalEnum;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.talk.MapTypeEnum;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.BadTokenException;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

public abstract class ChatActivity extends Activity{

	protected long idCodeMappings;

	private static ChatActivity target;

	private DeleteTokenAction deleteTokenAction;
	private TextWatcher multiWatcher;

	// Views
	private ListView messagesView;
	protected EditText mOutEditText; protected EditText hideMOutEditText;
	private Button mSendButton;
	protected MsgsAdapter messageAdapter;









	public static final int REFRESH_CONTACT_HANDLER_WHAT_1 = 0;
	public static final String CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA = "contactName";
	protected Handler refresherContact = getRefresherContactHandler();

	protected static final int PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1 = 0;
	public static final String JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "jitterValue";
	public static final String ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "errorCorrectionValue";
	public static final String ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "idCodeMappings";
	public static final String REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "refreshCodeMappings";
	public static final String IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "isUrgent";

	// Preferences
	protected int jitter;
	protected int checkSumLength;
	protected boolean isUrgent;

	protected class MessagesRefresher extends AsyncTask<String, Void, List<MsgDAO>>{
		private MsgsAdapter msgsAdapter;
		public MessagesRefresher(MsgsAdapter msgsAdapter){
			this.msgsAdapter = msgsAdapter;
		}
		@Override
		protected List<MsgDAO> doInBackground(String... phone) {
			MessageDataSource instance = MessageDataSource.getInstance(ChatActivity.this);
			return instance.listThreadMessages(phone[0]);
		}
		@Override
		protected void onPostExecute(List<MsgDAO> result) {
			msgsAdapter.newData(result);
		}
	}








	public static class NewCodeMapImportedRefreshBroadcast extends BroadcastReceiver{
		public NewCodeMapImportedRefreshBroadcast(){}
		@Override
		public void onReceive(Context context, Intent intent) {
			String associatedPhone = intent.getStringExtra(CodeMappingsEditListActivity.ASSOCIATED_PHONE_ID_NEW_IMPORTED_CODE_MAP);
			long newAssociatedCode = intent.getLongExtra(CodeMappingsEditListActivity.CODE_MAP_ID_NEW_IMPORTED_CODE_MAP, 0);
			synchronized (sendMessage) {
				if(target!=null){
					target.whatToDoOnNewCodeMap(associatedPhone, newAssociatedCode);
				}
			}
		}
	}








	public final static Handler serviceNotRunningWarn = new Handler(){
		public void handleMessage(android.os.Message msg) {
			synchronized(sendMessage){
				Toast.makeText(target, R.string.service_not_running, Toast.LENGTH_LONG).show();;
			}
		};
	};
	//+Handle
	public static final int SEND_MESSAGE = 0;
	//++Handle Data
	public static final String MESSAGE = "message";
	public static final String NUMBER = "number";
	public static final String SERVICE_WAS_RUNNING = "serviceWasRunning";
	public static final String MESSAGE_ID = "messageId";
	public static final String IS_URGENT = "isUrgent";

	public final static Handler sendMessage = new Handler(){
		public void handleMessage(final android.os.Message msg) {

			final boolean wasRunning = msg.getData().getBoolean(SERVICE_WAS_RUNNING);
			final String message = msg.getData().getString(MESSAGE);
			final String number = msg.getData().getString(NUMBER);
			final Long messageId = msg.getData().getLong(MESSAGE_ID);
			final boolean isUrgent = msg.getData().getBoolean(IS_URGENT);

			ServiceListenerSendMsg serviceChecker = new ServiceListenerSendMsg();
			serviceChecker.start();
			serviceChecker.onUnblockGoOn();
			(new Thread(){
				int numberOfRings=0;
				public void run() {
					try{

						int jitter = GeneralPreferences.PREFERENCE_DEFAULT_JITTER;
						int checkSumLength = GeneralPreferences.PREFERENCE_DEFAULT_CHECK_SUM_LENGTH;
						long idCodeMappings = 0;
						NotificationManager notificationManager = null;

						if(PhoneStateListenerStarterService.getIsRunning()){
							synchronized(sendMessage){

								jitter = target.jitter;
								checkSumLength = target.checkSumLength;
								idCodeMappings = target.idCodeMappings;

								Intent cancelIntent = new Intent(BroadcastEvents.MESSAGE_SENT_CANCEL);
								PendingIntent pIntent = PendingIntent.getBroadcast(target, 0, cancelIntent, 0);

								notificationManager = (NotificationManager) target.getSystemService(Context.NOTIFICATION_SERVICE);

								Notification newMessage = new Notification();
								newMessage.icon = android.R.drawable.ic_notification_clear_all;
								newMessage.tickerText = target.getString(R.string.cancel_message_being_send);
								newMessage.when = System.currentTimeMillis();

								String rawStr = target.getString(R.string.message_being_send);
								String titleProgress = String.format(rawStr, "0");
								newMessage.setLatestEventInfo(target, titleProgress, newMessage.tickerText, pIntent);
								newMessage.flags |= Notification.FLAG_AUTO_CANCEL;
								newMessage.flags |= Notification.FLAG_NO_CLEAR;

								// Same Id the view only gets updated
								notificationManager.notify((number+NotificationSuffixes.SENDING_NOTIFICATION).hashCode(), newMessage);


							}

							// We are just using the atomic integer in order to pass the integer by reference
							AtomicInteger givenRings = new AtomicInteger();
							numberOfRings = calculateNumberOfRings(jitter, checkSumLength, idCodeMappings);

							final boolean swapBitTimes = swapBitTimes(jitter, checkSumLength, idCodeMappings);

							try{
								//
								Tone swap = SignalEnum.parseString(swapBitTimes?"1":"0");
								swap.sendTone(false, number, ThreadJitterEnum.biggestJitterEnum().ordinal(), getContext(), numberOfRings, givenRings);

								if(jitter!=ThreadJitterEnum.LONG.ordinal()){
									sendJitter(swapBitTimes, jitter, checkSumLength, idCodeMappings, givenRings);
									Tone.sendEndTone(number, ThreadJitterEnum.biggestJitterEnum().ordinal(), getContext(), numberOfRings, givenRings);
								}
								sendMapInUse(swapBitTimes, jitter, checkSumLength, idCodeMappings, givenRings);
								sendMessage(swapBitTimes, jitter, checkSumLength, idCodeMappings, givenRings);
								if(!isUrgent && checkSumLength==GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE){
									sendCheckSum(swapBitTimes, jitter, checkSumLength, idCodeMappings, givenRings);
								}
								Tone.sendEndTone(number, jitter, getContext(), numberOfRings, givenRings);

							}catch(BrokenChannelException e){
								MessageDataSource.getInstance(getContext()).updateMessageStatus(messageId, MsgStatusEnum.CHANNEL_BROKEN);
							}catch (MessageCancelledException e){
								MessageDataSource.getInstance(getContext()).updateMessageStatus(messageId, MsgStatusEnum.SEND_CANCELLED);
							}

							notificationManager.cancel((number+NotificationSuffixes.SENDING_NOTIFICATION).hashCode());
							CallCancelBroadcast.reset();
							MsgRefreshBroadcast.sendRefreshNotice(getContext(), number, MsgDirectionEnum.OUTGOING);

						}else{
							serviceNotRunningWarn.sendEmptyMessage(0);
						}

						if(!wasRunning){
							// We started the fire we put it down
							Intent stopPhoneListener = new Intent(getContext(), PhoneStateListenerStarterService.class);
							getContext().stopService(stopPhoneListener);
						}
					}catch(RuntimeException e){
						// This is not supposed to happen
						e.printStackTrace();
					}
				}

				public Context getContext(){
					synchronized(sendMessage){
						return target;
					}
				}

				public boolean swapBitTimes(int jitter, int checkSumLength, long idCodeMappings){
					List<Tone> totalTones = new LinkedList<Tone>();
					totalTones.addAll(getJitter(jitter));
					totalTones.addAll(getMapInUse(idCodeMappings));
					totalTones.addAll(getMessage(idCodeMappings));
					totalTones.addAll(getCheckSumTone(checkSumLength));

					// Count zeros and ones
					int zeros = 0;
					int ones = 0;

					for(Tone tone: totalTones){
						for(SignalEnum sig :tone.getTone()){
							if(sig.equals(SignalEnum.ONE)){
								ones++;
							}else if(sig.equals(SignalEnum.ZERO)){
								zeros++;
							}
						}
					}

					return zeros<ones;
				}

				public void sendMapInUse(boolean swapBitTimes, int jitter, int checkSumLength, long idCodeMappings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
					Tone tone = getMapInUse(idCodeMappings).getFirst();
					tone.sendTone(swapBitTimes, number, ThreadJitterEnum.biggestJitterEnum().ordinal(), getContext(), numberOfRings, givenRings);
				}
				public LinkedList<Tone> getMapInUse(long idCodeMappings){
					LinkedList<Tone> result = new LinkedList<Tone>();
					Tone tone = SignalEnum.parseString(MapTypeEnum.getMapInUseByCodeMApId(idCodeMappings).getCode());
					result.add(tone);
					return result;
				}

				public void sendJitter(boolean swapBitTimes, int jitter, int checkSumLength, long idCodeMappings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
					if(jitter==ThreadJitterEnum.LONG.ordinal())
						throw new IllegalArgumentException(); // No need to send long jitter
					Tone tone = getJitter(jitter).getFirst();
					tone.sendTone(swapBitTimes, number, ThreadJitterEnum.biggestJitterEnum().ordinal(), getContext(), numberOfRings, givenRings);
				}
				public LinkedList<Tone> getJitter(int jitter){
					LinkedList<Tone> result = new LinkedList<Tone>();
					if(jitter!=ThreadJitterEnum.LONG.ordinal()){
						String jitterIdBin = Integer.toBinaryString(jitter);
						Tone tone = SignalEnum.parseString(jitterIdBin);
						result.add(tone);
					}
					return result;
				}

				public void sendMessage(boolean swapBitTimes, int jitter, int checkSumLength, long idCodeMappings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
					LinkedList<Tone> toSend = getMessage(idCodeMappings);
					for(Tone sendIt : toSend){						
						sendIt.sendTone(swapBitTimes, number, jitter, getContext(), numberOfRings, givenRings);
					}
				}
				/*public BiMap<String, Tone> getAlphabet(long idCodeMappings){
					BiMap<String, Tone> result = HashBiMap.create();
					if(idCodeMappings==0){
						result.putAll(SignalEnum.getAlphabet(getContext()));
					}
					result.putAll(SignalEnum.convertToToneMap(CodeMappingsDataSource.getInstance(getContext()).listCodeMapping(idCodeMappings).inverse()));
					return result;
				}*/

				public LinkedList<Tone> getMessage(long idCodeMappings){
					LinkedList<Tone> result = new LinkedList<Tone>();
					BiMap<String, Tone> alphabet=null;
					String[] codeStrings = null;

					if(idCodeMappings==0){
						alphabet = ImmutableBiMap.copyOf(SignalEnum.getAlphabet(getContext()));
						LinkedList<String> list = new LinkedList<String>(Arrays.asList(message.split("")));
						list.remove(0);
						codeStrings=list.toArray(new String[list.size()]);
					} else{

						BiMap<String, String> tempCodesMappings = CodeMappingsDataSource.getInstance(getContext()).listCodeMapping(idCodeMappings);
						//tempCodesMappings=tempCodesMappings.inverse();
						alphabet=ImmutableBiMap.copyOf(SignalEnum.convertToToneMap(tempCodesMappings));

						codeStrings = message.split(NewLineTokenizer.SEPARATOR_STR);
					}

					for(String codeString: codeStrings){
						Tone tone = alphabet.get(codeString);
						result.add(new Tone(tone));
					}

					if(isUrgent){
						if(result.size()!=1){
							throw new IllegalArgumentException("Can only send urgent message with one code");
						}else{
							List<SignalEnum> listSignals = result.remove().getTone();
							int toRemove = listSignals.indexOf(SignalEnum.ONE);
							
							// The code transmitted must have at least 3 bits, if not problems on reception will occur due to miss parsed Jitter
							if(toRemove>=0){
								
								if(listSignals.size()<3){ 
									throw new IllegalArgumentException(); 
								}
								
								int elementCount = listSignals.size()-toRemove-1;
								if(elementCount<3){ 
									toRemove=toRemove-elementCount; 
								}
								
								List<SignalEnum> optimizedCode = listSignals.subList(toRemove, listSignals.size());
								result.add(new Tone(optimizedCode));
							}else{
								LinkedList<SignalEnum> list = new LinkedList<SignalEnum>();
								list.add(SignalEnum.ZERO);
								list.add(SignalEnum.ZERO);
								list.add(SignalEnum.ZERO);
								result.add(new Tone(list));
							}
						}
					}else{

						if(result.size()>1){// Optimization available for messages that have more than 1 code
							for(int i=0;i<result.size();i++){
								transformOptimized(alphabet, result.get(i));
							}
						}
					}
					return result;
				}
				public void transformOptimized(BiMap<String, Tone> alphabet, Tone tone){

					final List<SignalEnum> temp = new LinkedList<SignalEnum>();
					final List<SignalEnum> result = new LinkedList<SignalEnum>();
					boolean endOptimization = false;

					for(int pos=0; pos<tone.getTone().size();pos++){
						temp.add(tone.getTone().get(pos));	

						if(!endOptimization && temp.size()==2){
							String tag =(temp.get(0).getTag()+temp.get(1).getTag());
							if(tag.matches("^(00)|(01)$")){
								endOptimization = process(tone, pos, temp, result, alphabet);
							}else{
								// End of optimization
								result.addAll(temp);
								temp.clear();
								endOptimization=true;
							}
						}else if(endOptimization){
							result.add(tone.getTone().get(pos));
						}


					}
					boolean allOnes = Collections2.filter(result, new Predicate<SignalEnum>() {
						@Override
						public boolean apply(SignalEnum arg) {
							return arg.equals(SignalEnum.ZERO);
						}
					}).size()==0;

					if(allOnes){
						process(tone, tone.getTone().size(), temp, result, alphabet);
					}

					tone.getTone().clear();
					tone.getTone().addAll(result);
				}

				public boolean process(final Tone original, final int pos, final List<SignalEnum> temp, final List<SignalEnum> result, Map<String, Tone> alphabet){
					boolean endOptimization=false;
					String tag = null;

					if(temp.size()==2){
						tag=temp.get(0).getTag()+temp.get(1).getTag();
					}

					if(tag==null||tag.matches("^00$")){
						Map<String, Tone> filtered = Maps.filterValues(alphabet, new Predicate<Tone>() {
							@Override
							public boolean apply(Tone arg) {
								if(result.size()!=0){

									for(SignalEnum signal: result){
										if(!signal.equals(SignalEnum.ONE))
											throw new IllegalArgumentException();
									}

									// Decode, except last
									List<SignalEnum> resultSemiDecoded = new LinkedList<SignalEnum>();
									Iterator<SignalEnum> ite = result.iterator();


									while(ite.hasNext()){
										ite.next();
										if(ite.hasNext()){
											resultSemiDecoded.add(SignalEnum.ZERO);
											resultSemiDecoded.add(SignalEnum.ZERO);
										}else{
											resultSemiDecoded.add(SignalEnum.ONE);
										}
									}
									List<SignalEnum> tone = arg.getTone();
									List<SignalEnum> matchesEnd = tone.subList(0, resultSemiDecoded.size());
									return matchesEnd.equals(resultSemiDecoded);
								}
								return false;
							}
						});
						boolean revert=false;
						if(filtered.size()==0){
							temp.clear();
							result.add(SignalEnum.ONE);

							{// verify case 11
								Tone verify =new Tone(original);
								int nextPos=pos+1;
								if(nextPos<verify.getTone().size() && verify.getTone().get(nextPos).equals(SignalEnum.ONE)){
									revert=true;
								}
							}
						}else{
							revert=true;
						}
						if(revert){
							endOptimization=true;
							result.remove(result.size()-1);
							result.add(SignalEnum.ZERO);
							result.add(SignalEnum.ZERO);
						}
					}else if(tag.matches("^01$")){
						result.addAll(temp);
						temp.clear();
						endOptimization=true;
					}
					return endOptimization;
				}


				public void sendCheckSum(boolean swapBitTimes, int jitter, int checkSumLength, long idCodeMappings, AtomicInteger givenRings) throws BrokenChannelException, MessageCancelledException{
					Tone tone = getCheckSumTone(checkSumLength).getFirst();
					tone.sendTone(swapBitTimes, number, jitter, getContext(), numberOfRings, givenRings);
				}
				public LinkedList<Tone> getCheckSumTone(int checkSumLength){
					LinkedList<Tone> result = new LinkedList<Tone>();
					if(checkSumLength!=0){
						final int reminderOf = (int)(Math.pow(checkSumLength, 2)-1);
						final int detectionCode = Math.abs(message.hashCode()%reminderOf);
						String detectionCodeStr = Integer.toBinaryString(detectionCode);
						Tone tone = SignalEnum.parseString(detectionCodeStr);
						result.add(tone);
					}
					return result;
				}

				public int calculateNumberOfRings(int jitter, int checkSumLength, long idCodeMappings){
					boolean isDefaultJitter=jitter==ThreadJitterEnum.LONG.ordinal();

					int jitter_length =0;
					if(!isDefaultJitter){
						jitter_length = Integer.toBinaryString(jitter).length()+1/*end jitter indication*/;
					}

					int checksum_length = checkSumLength;
					int message_length = 0;
					message_length=0;
					for(Tone code :getMessage(idCodeMappings)){
						message_length+=code.getTone().size();
					}

					message_length++;/*map indication*/

					return message_length+jitter_length+checksum_length+1/*swap bits indication*/+1/*end message indication*/;
				}
			}).start();



		};
	};




	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		idCodeMappings = 0;

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.chat);

		LinkedList<MsgDAO> messages = new LinkedList<MsgDAO>();

		synchronized (sendMessage) { target=this; }

		/*
		Date date = Calendar.getInstance().getTime();
		messages.add(new MessageDataObject(date, "oi", "916557758", DirectionEnum.OUTGOING));
		messages.add(new MessageDataObject(date, "oi", "916557758", DirectionEnum.INCOMING));
		 */

		messageAdapter = new MsgsAdapter(this, messages);

		messagesView = (ListView) findViewById(R.id.in);
		messagesView.setDivider(null);
		messagesView.setDividerHeight(10);
		messagesView.setAdapter(messageAdapter);


		mOutEditText =  (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.addTextChangedListener(new MsgWatcher(this, mOutEditText));
		hideMOutEditText = (MultiAutoCompleteTextView) findViewById(R.id.edit_text_out_multi);
		hideMOutEditText.setVisibility(View.GONE);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				TextView view = mOutEditText;
				String message = view.getText().toString();
				String number = getContactNumber();
				try {
					sendMessage(message, number);
				} catch (UrgentMessageException e) {
					Toast.makeText(ChatActivity.this, R.string.urgent_set_warning, Toast.LENGTH_LONG).show();
				}
			}
		});

		isUrgent=false;

	}

	@Override
	protected void onDestroy() {
		backAction();
		super.onDestroy();
	}

	protected boolean isOutInMultiModeBox(){
		return mOutEditText instanceof MultiAutoCompleteTextView;
	}
	private void swapOutBoxes(){

		Button delete = (Button)findViewById(R.id.button_delete_multi);
		if(isOutInMultiModeBox()){
			delete.setVisibility(View.GONE);
		}else{
			delete.setVisibility(View.VISIBLE);
		}


		EditText buffer = hideMOutEditText; 
		hideMOutEditText=mOutEditText;
		hideMOutEditText.setVisibility(View.GONE);
		/*layoutParams = hideMOutEditText.getLayoutParams();
		layoutParams.width=0;
		hideMOutEditText.setLayoutParams(layoutParams);*/

		mOutEditText=buffer;
		mOutEditText.setVisibility(View.VISIBLE);
		/*layoutParams = mOutEditText.getLayoutParams();
		layoutParams.width=ViewGroup.LayoutParams.FILL_PARENT;
		mOutEditText.setLayoutParams(layoutParams);*/

	}
	protected EditText getEdit(){
		synchronized (EditText.class) {
			if(mOutEditText.getClass().equals(EditText.class)){
				return mOutEditText; 
			}else{
				return hideMOutEditText;
			}
		}
	}
	protected void doSwapOutBoxes(long idCodeMappings){
		synchronized (EditText.class) {
			this.idCodeMappings = idCodeMappings;
			if((isOutInMultiModeBox() && idCodeMappings==0)||(!isOutInMultiModeBox() && idCodeMappings!=0)){
				swapOutBoxes();
			}
			if(isOutInMultiModeBox()){
				refreshBoxes(idCodeMappings);
			}
		}
	}
	protected void refreshBoxes(long idCodeMappings){
		synchronized (EditText.class) {
			this.idCodeMappings = idCodeMappings;
			if(mOutEditText instanceof MultiAutoCompleteTextView){
				final MultiAutoCompleteTextView editTextSuggestions = (MultiAutoCompleteTextView) mOutEditText;

				CodeMappingsDAO codeMap = CodeMappingsDataSource.getInstance(this).getCodeMapping(idCodeMappings);
				final Collection<String> codes = codeMap.getCodesMapping(this, true).values();
				{

					final List<String> passed = RotinesUtilsSingleton.getSortedStringList(codes);

					ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, passed){
						private Set<String> hide = new HashSet<String>();
						@Override
						public Filter getFilter() {
							return new Filter(){
								@Override
								protected FilterResults performFiltering(CharSequence constraint) {
									if(constraint == null) return new FilterResults();

									synchronized (EditText.class) {
										LinkedList<String> suggestions = new LinkedList<String>();
										for(String value: codes){
											if(value.startsWith(constraint.toString())){
												suggestions.add(value);
											}
										}

										FilterResults filterResults = new FilterResults();
										filterResults.values = suggestions;
										filterResults.count = suggestions.size();
										return filterResults;
									}

								}
								@SuppressWarnings("unchecked")
								@Override
								protected void publishResults(CharSequence constraint, FilterResults results) {
									if (results.count == 0) return;

									synchronized (EditText.class) {
										// Add itens in ordered fashion
										// BEGIN
										{
											final int total = getCount();
											for(int i=0;i<total;i++){
												String item = getItem(0);
												hide.add(item);
												remove(item);
											}
											notifyDataSetChanged();

											final List<String> ordered = RotinesUtilsSingleton.getSortedStringList(hide);
											hide.clear();

											for(String orderedItem: ordered){ add(orderedItem); }
											notifyDataSetChanged();
										}
										// END

										// Do filtering
										LinkedList<String> result =  (LinkedList<String>)results.values;
										final int total = getCount();
										int next=0;
										for(int i=0;i<total;i++){
											String item = getItem(next);
											if(!result.contains(item)){
												remove(item);
												hide.add(item);
											}else{next++;}
										}

										try{
											ChatActivity.this.runOnUiThread(new Runnable() {
												@Override
												public void run() { 
													notifyDataSetChanged();
													editTextSuggestions.showDropDown();
												}
											});
										}catch(BadTokenException e){ /* Activity not running. No Stress. */}

									}
								}
							};
						}
					};
					adapter.notifyDataSetChanged();
					editTextSuggestions.setAdapter(adapter);
				}
				editTextSuggestions.setThreshold(1);
				editTextSuggestions.setTokenizer(new NewLineTokenizer()); // new MultiAutoCompleteTextView.CommaTokenizer()
				((ArrayAdapter<?>)editTextSuggestions.getAdapter()).notifyDataSetChanged();

				final Button deleteMulti =((Button)findViewById(R.id.button_delete_multi));

				deleteMulti.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {	
						if(event.getAction()==MotionEvent.ACTION_DOWN){
							deleteMulti.setBackgroundResource(R.drawable.btnbs_pressed);
						}else if(event.getAction()==MotionEvent.ACTION_UP){
							deleteMulti.setBackgroundResource(R.drawable.btnbs_released);
						}
						return false;
					}
				});


				final MultiAutoCompleteTextView multiEditorParam = (MultiAutoCompleteTextView) mOutEditText;
				deleteTokenAction = new DeleteTokenAction(multiEditorParam);
				deleteMulti.setOnClickListener(deleteTokenAction);

				if( multiWatcher!=null){
					mOutEditText.removeTextChangedListener(multiWatcher);
				}
				multiWatcher = new MultiTextWatcher(this, multiEditorParam, codes, deleteTokenAction);
				mOutEditText.addTextChangedListener(multiWatcher);

				String currentContend = editTextSuggestions.getText().toString();
				if(currentContend.length()!=0){ // Avoid IllegalStateException when initializing
					editTextSuggestions.setText("");
					StringTokenizer st = new StringTokenizer(currentContend, NewLineTokenizer.SEPARATOR_STR, false);
					while(st.hasMoreTokens()){
						String token = st.nextToken();
						StringBuilder newText= new StringBuilder(editTextSuggestions.getText().toString());
						newText.append(token+NewLineTokenizer.SEPARATOR_STR);
						editTextSuggestions.setText(newText);
					}
				}
			}else{
				// Do nothing...
			}
		}
	}



	protected abstract void whatToDoOnNewCodeMap(String phone, long newAssociatedCode);
	protected abstract String getContactNumber();
	protected abstract Handler getRefresherContactHandler();
	protected abstract void backAction();
	protected abstract void clearAllFields();

	protected void assignFunctionToBack(int id){
		ImageView back = (ImageView) findViewById(id);
		back.setLongClickable(true);
		back.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				ImageView image = (ImageView)v;
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					image.setImageResource(R.drawable.back_active);
				}else if(event.getAction()==MotionEvent.ACTION_UP){
					image.setImageResource(R.drawable.back_rest);
					//backAction(mOutEditText.getText().toString());
					ChatActivity.this.finish();
				}
				return false;
			}
		});
	}



	protected void setName(String name, TextView contactNameInfo){
		if(name!=null){
			contactNameInfo.setText(name);
			contactNameInfo.setTypeface(null, Typeface.NORMAL);
		}else{
			contactNameInfo.setText(R.string.unknown_contact);
			formatNotValidName(contactNameInfo);
		}
	}
	protected void setNameUnavailable(TextView contactNameInfo){
		contactNameInfo.setText(R.string.unavailable);
		formatNotValidName(contactNameInfo);
	}
	private void formatNotValidName(TextView contactNameInfo){
		contactNameInfo.setTypeface(null, Typeface.BOLD);
	}

	protected final static int  SETTINGS_OPT 	= Menu.NONE;
	protected final static int  CLEAR_ALL_OPT 	= SETTINGS_OPT+1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		String[] menuItems = getResources().getStringArray(R.array.chat_option_menu);
		menu.add(Menu.NONE, SETTINGS_OPT, SETTINGS_OPT, menuItems[SETTINGS_OPT]).setIcon(android.R.drawable.ic_menu_manage);
		menu.add(Menu.NONE, CLEAR_ALL_OPT, CLEAR_ALL_OPT, menuItems[CLEAR_ALL_OPT]).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return super.onCreateOptionsMenu(menu); 
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case CLEAR_ALL_OPT: {
			clearAllFields();
			return true;
		}
		}
		return false;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {	
			case (PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1):{
				long beforeCodeMappings = idCodeMappings; 
				long idCodeMappings = data.getLongExtra(ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, this.idCodeMappings);
				
				boolean swap = (beforeCodeMappings == 0 && idCodeMappings!=0);
				swap=swap||(beforeCodeMappings != 0 && idCodeMappings==0);
				
				if(swap){ 
					doSwapOutBoxes(idCodeMappings); 
				}else{
					// Prevent refresh when it is picked for the first time...
					boolean refresh = data.getBooleanExtra(REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, false);
					refresh=refresh||idCodeMappings!=beforeCodeMappings;
					if(refresh){
						refreshBoxes(idCodeMappings);
					}
				}
				jitter=data.getIntExtra(JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, jitter);
				checkSumLength=data.getIntExtra(ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, checkSumLength);
				isUrgent=data.getBooleanExtra(IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, isUrgent);
				break;
			}
			}
		}
	}
	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backAction(mOutEditText.getText().toString());
		}
		return super.onKeyDown(keyCode, event);
	};*/

	@SuppressWarnings("serial")
	public class UrgentMessageException extends Exception{
		public UrgentMessageException() {
			super();
		}
	}

	protected abstract void sendMessageAction(final String message, final String number, final Date timeStamp);
	protected void sendMessage(String message, final String phoneNumber) throws UrgentMessageException {
		if (message.length() > 0 ) {
			if(ContactUtilsSingleton.isPhoneNumber(phoneNumber)){

				// The code inside this thread block is long running
				// if we used this code outside a thread is would make the aplication throw a ANR exception

				final boolean isRunning = PhoneStateListenerStarterService.getIsRunning();

				if(!isRunning){
					Intent intent = new Intent(ChatActivity.this, PhoneStateListenerStarterService.class);
					ChatActivity.this.startService(intent);
				}

				Date now = Calendar.getInstance().getTime();

				StringBuilder messageSent = new StringBuilder(message);
				if(idCodeMappings!=0){
					messageSent.delete(0, messageSent.length());
					BiMap<String, String> codesForMappings = CodeMappingsDataSource.getInstance(this).listCodeMapping(idCodeMappings);
					String[] codeStrings = message.split(NewLineTokenizer.SEPARATOR_STR);
					if(isUrgent && codeStrings.length!=1)
						throw new UrgentMessageException();
					for(String codeString: codeStrings){
						if(codesForMappings.values().contains(codeString)){
							messageSent.append(codeString);
							messageSent.append(NewLineTokenizer.SEPARATOR);
						}
					}
				}else{
					if(isUrgent && message.length()!=1){
						throw new UrgentMessageException();
					}
				}
				message=messageSent.toString();

				// This call can yield a error on the console if there is already a thread record for the phone number
				// but no worry the call will return -1 and the code will flow gracefully
				SQLiteDatabase writer = RingSMSDBHelper.getInstance(this).getWritableDatabase();
				writer.beginTransaction();
				ThreadDataSource.getInstance(this).insertThread(phoneNumber, jitter, checkSumLength, this.idCodeMappings);
				MsgStatusEnum status = isUrgent?MsgStatusEnum.URGENT_SENT:MsgStatusEnum.SUCCESS;
				long messageId=MessageDataSource.getInstance(this).insertMessage(phoneNumber, message, MsgDirectionEnum.OUTGOING, now, status);
				writer.setTransactionSuccessful();
				writer.endTransaction();

				// We have to use a handler because the service will only start after the caller method finished
				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putBoolean(SERVICE_WAS_RUNNING, isRunning);
				bundle.putString(MESSAGE, message);
				bundle.putString(NUMBER, phoneNumber);
				bundle.putLong(MESSAGE_ID, messageId);
				bundle.putBoolean(IS_URGENT, isUrgent);
				msg.setData(bundle);
				sendMessage.sendMessageDelayed(msg, 200);

				sendMessageAction(message, phoneNumber, now);

				MsgRefreshBroadcast.sendRefreshNotice(ChatActivity.this, phoneNumber, MsgDirectionEnum.OUTGOING);

			} else {
				Toast.makeText(this, this.getString(R.string.supply_valid_phone), Toast.LENGTH_SHORT).show();
			}
		}else{
			Toast.makeText(this, this.getString(R.string.message_empty), Toast.LENGTH_SHORT).show();
			return;   
		}
	}

}