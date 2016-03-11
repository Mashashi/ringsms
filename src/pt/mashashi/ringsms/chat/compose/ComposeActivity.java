package pt.mashashi.ringsms.chat.compose;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ContactDTO;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.ThreadPreferences;
import pt.mashashi.ringsms.chat.ContactDTO.ContactAttributeEnum;
import pt.mashashi.ringsms.interfazze.ListenForMessageRequestBroacast;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.StaleDataException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ComposeActivity  extends ChatActivity{

	private static TextView contactNameTemp;
	private static AutoCompleteTextView	number;

	private ObservContactCompose contactNameObserver;
	private ObservSuggestions suggestionListObserver;

	private static ToCleaner toCleaner;

	public static final int 	REFRESH_SUGGESTION_LIST = 0;
	public static final String  NEW_SUGGESTION_LIST 				  = "newSuggestionList";
	public static final String  NEW_SUGGESTION_LIST_LOAD_SAVED_NUMBER = "newSuggestionListLoadSavedNumber";
	public final static Handler refresherSuggestions = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what==REFRESH_SUGGESTION_LIST){

				ArrayList<ContactDTO> list = msg.getData().getParcelableArrayList(NEW_SUGGESTION_LIST);
				ContactSugestionAdapter adapter = ((ContactSugestionAdapter)ComposeActivity.number.getAdapter());
				adapter.newData(list);

				boolean loadSavedNumber = msg.getData().getBoolean(NEW_SUGGESTION_LIST_LOAD_SAVED_NUMBER);
				if(loadSavedNumber){
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(number.getContext());
					String numberSaved = sp.getString(PREFERENCE_COMPOSED_NUMBER, "");
					if(numberSaved.length()!=0){
						number.setEnabled(true);
						number.setText(numberSaved);
					}
				}

			}
		}
	};




	private static LoadingAnimation loadingContactName;
	private static class LoadingAnimation extends AsyncTask<Void, String, Void>{
		private Context ctx;
		private volatile boolean updatedUserInfo;
		private TextView contactNameTemp;
		public LoadingAnimation(Context ctx, TextView contactNameTemp) {
			this.ctx = ctx;
			this.updatedUserInfo=false;
			this.contactNameTemp = contactNameTemp;
		}
		@Override
		protected Void doInBackground(Void... params) {
			int dotState = 1;
			String textPlaced=null;
			try{
				while(true){
					String dots = "";
					for(int dot=0;dot<dotState;dot++) dots=dots+".";
					textPlaced=ctx.getString(R.string.loading)+dots;
					synchronized (LoadingAnimation.class) {
						publishProgress(textPlaced); waitTextPlaced(textPlaced);
					}

					if(isCancelled()){
						throw new InterruptedException();
					}
					Thread.sleep(300);
					dotState = dotState==3?0:dotState+1;
				}
			}
			catch(InterruptedException e)	{ MyLog.d(ThreadsActivity.DEBUG_TAG, "Terminating animation"); }
			catch(Exception e)				{ MyLog.d(ThreadsActivity.DEBUG_TAG, e.getMessage()); }

			return null;
		}
		public void waitTextPlaced(String textPlaced) throws InterruptedException{
			while(!updatedUserInfo){
				try {
					LoadingAnimation.class.wait();
				} catch (InterruptedException e1) {
					throw new InterruptedException();
				}
			}
			updatedUserInfo=false;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			synchronized (LoadingAnimation.class) {
				String newText = values[0];
				contactNameTemp.setText(newText);

				updatedUserInfo=true;
				LoadingAnimation.class.notify();

				MyLog.d(ThreadsActivity.DEBUG_TAG, "Load string updated to: "+newText);
			}
		}
	};

	private static final String PREFERENCE_COMPOSED_TEXT = "composedText";
	private static final String PREFERENCE_COMPOSED_NUMBER = "composedNumber";

	public static final int PICK_CONTACT_ACTIVITY_REQUEST_2 = 1;

	public static final int CONTACT_NAME_UNAVAILABLE_HANDLER_WHAT_1 = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		idCodeMappings = 0;

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title_compose);
		assignFunctionToBack(R.id.back);

		{
			android.content.SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			super.jitter=sp.getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
			super.checkSumLength= sp.getBoolean(GeneralPreferences.CHECK_SUM, false)?GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE:0;

		}


		ImageView selectContact = (ImageView) findViewById(R.id.select_contact);
		selectContact.setLongClickable(true);
		selectContact.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				ImageView image = (ImageView)v;
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					image.setImageResource(R.drawable.select_contact_pressed);
				}else if(event.getAction()==MotionEvent.ACTION_UP){
					image.setImageResource(R.drawable.select_contact_released);
				}
				return false;
			}
		});
		selectContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
				startActivityForResult(intent, PICK_CONTACT_ACTIVITY_REQUEST_2); 
			}
		});

		contactNameTemp = (TextView) findViewById(R.id.name);

		toCleaner=new ToCleaner();

		number = (AutoCompleteTextView) findViewById(R.id.number);
		number.setOnTouchListener(toCleaner);
		number.addTextChangedListener(new TextWatcher() {
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				final String inText = s.toString();
				onNumberEdition(inText);
			}
		});



		final ContactSugestionAdapter adapterSuggestion = new ContactSugestionAdapter(this, R.layout.contact_suggestion, new LinkedList<ContactDTO>());
		number.setAdapter(adapterSuggestion);
		number.setThreshold(1);



		suggestionListObserver = new ObservSuggestions(this);
		getContentResolver().registerContentObserver(
				suggestionListObserver.getUri(), true, suggestionListObserver
				);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		{
			String textComposed = sp.getString(PREFERENCE_COMPOSED_TEXT, "");
			super.mOutEditText.setText(textComposed);
			mOutEditText.setSelected(true);
			mOutEditText.setSelection(textComposed.length());
		}

		String numberSaved = sp.getString(PREFERENCE_COMPOSED_NUMBER, "");
		if(numberSaved.length()!=0){
			toCleaner.setExecuted(number);
			number.setEnabled(false);
			contactNameTemp.setText(R.string.loading);
			loadingContactName = new LoadingAnimation(this, contactNameTemp);
			loadingContactName.execute();
		}

		Thread contactListSetup = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					// The call getAllContactObjects takes to long like more than 75% of the quantum time
					// of the method onCreate was being used by this call it should be in a async task
					List<ContactDTO> result = ContactUtilsSingleton.getAllContactObjects(ComposeActivity.this);
					Message updateSuggestionList = new Message();
					updateSuggestionList.what=ComposeActivity.REFRESH_SUGGESTION_LIST;
					Bundle bundle = new Bundle();
					bundle.putParcelableArrayList(ComposeActivity.NEW_SUGGESTION_LIST,new ArrayList<ContactDTO>(result));
					bundle.putBoolean(NEW_SUGGESTION_LIST_LOAD_SAVED_NUMBER, true);
					updateSuggestionList.setData(bundle);
					if(loadingContactName!=null){
						loadingContactName.cancel(true);
					}
					ComposeActivity.refresherSuggestions.sendMessage(updateSuggestionList); // This will only be executed after the onCreate method is finished
				} catch (UnableToRetrieveDataException e) { e.printStackTrace(); }

			}
		});
		contactListSetup.setPriority(Thread.NORM_PRIORITY);
		contactListSetup.start();
	}

	// +++++++++++++++++++
	// Activity life cycle
	// +++++++++++++++++++

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(suggestionListObserver!=null)
			getContentResolver().unregisterContentObserver(suggestionListObserver);
		if(contactNameObserver!=null)
			getContentResolver().unregisterContentObserver(contactNameObserver);
		if(loadingContactName!=null){
			loadingContactName.cancel(true);
		}
	}
	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = getIntent();
		String phone = intent.getStringExtra(ListenForMessageRequestBroacast.PHONE_NUMBER_ACTIVITY_REQUEST_1);
		String message = intent.getStringExtra(ListenForMessageRequestBroacast.MESSAGE_ACTIVITY_REQUEST_1);
		super.idCodeMappings = intent.getLongExtra(ListenForMessageRequestBroacast.CODE_MAPPINGS_ACTIVITY_REQUEST_1, 0);
		if(phone!= null && message!=null){
			try {
				super.sendMessage(message, phone);
			} catch (UrgentMessageException e) {
				// This doesn't happen when functioning has proxy the message is never urgent...
			}
		}
	}
	@Override
	protected void onRestart() {
		super.onRestart();
		MyLog.d(ThreadsActivity.DEBUG_TAG, "ReStarting");
	}
	@Override
	protected void onResume() {
		super.onResume();
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Resuming");
	}
	// ++++++++++++++++
	// The options menu
	// ++++++++++++++++

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (reqCode) {
			case (PICK_CONTACT_ACTIVITY_REQUEST_2):{
				refreshContactInfo(data.getData());
				break;
			}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean consumed = false;
		switch (item.getItemId()) {
		case SETTINGS_OPT: {
			Intent intentConfigs = new Intent(this, ThreadPreferences.class);
			intentConfigs.putExtra(JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, jitter);
			intentConfigs.putExtra(ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, checkSumLength);
			intentConfigs.putExtra(ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, idCodeMappings);
			
			intentConfigs.putExtra(IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, super.isUrgent);
			
			startActivityForResult(intentConfigs, PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1);
			consumed=true;
			break;
		}
		default: {
			break;
		}
		}
		if(!consumed){
			consumed = super.onOptionsItemSelected(item);
		}
		return consumed;
	}

	// ++++++++++++++++++
	// Overridden methods
	// ++++++++++++++++++

	@Override
	public String getContactNumber() {
		return number.getText().toString();
	}
	@SuppressLint("HandlerLeak")
	@Override
	protected Handler getRefresherContactHandler(){
		return new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.what==REFRESH_CONTACT_HANDLER_WHAT_1){
					String name = msg.getData().getString(CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA);
					setName(name, ComposeActivity.contactNameTemp);
				}else if(msg.what==CONTACT_NAME_UNAVAILABLE_HANDLER_WHAT_1){
					setNameUnavailable(ComposeActivity.contactNameTemp);
				}
			}
		};
	}

	@Override
	protected void sendMessageAction(String message, String phoneNumber, Date timeStamp) {
		Intent result = new Intent();
		result.putExtra(ThreadsActivity.NEW_THREAD_PHONE_NEW_THREAD_ACTIVITY_REQUEST_1_DATA, phoneNumber);
		setResult(Activity.RESULT_OK, result);

		/*Editor spe = PreferenceManager.getDefaultSharedPreferences(this).edit();
		spe.putString(PREFERENCE_COMPOSED_TEXT, "");
		spe.putString(PREFERENCE_COMPOSED_NUMBER, "");
		spe.commit();*/
		mOutEditText.setText("");
		number.setText("");
		this.finish();
	}
	@Override
	protected void backAction() {
		Editor spe = PreferenceManager.getDefaultSharedPreferences(this).edit();
		EditText edit = getEdit();
		if(edit.getClass().equals(EditText.class)){
			spe.putString(PREFERENCE_COMPOSED_TEXT, edit.getText().toString());
		}
		String number = ComposeActivity.number.getText().toString();
		if(ContactUtilsSingleton.isPhoneNumber(number)){
			spe.putString(PREFERENCE_COMPOSED_NUMBER, number.toString());
		}else{
			spe.putString(PREFERENCE_COMPOSED_NUMBER, "");
		}
		spe.commit();
	};
	@Override
	protected void clearAllFields() {
		if(number.isEnabled()){
			toCleaner.setExecuted(number);
			number.setText("");
		}
		super.mOutEditText.setText("");
	}
	@Override
	protected void whatToDoOnNewCodeMap(String phone, long newAssociatedCode) {
		/*if(PhoneNumberUtils.compare(phone, number.getText().toString())){
			if(isOutInMultiModeBox()){
				super.idCodeMappings = newAssociatedCode;
				super.refreshBoxes();
			}
		}*/
		// Do nothing...
	}


	public void onNumberEdition(String inText){
		if(inText.length()!=0){	// This will prevent java.lang.IllegalArgumentException: URI content://com.android.contacts/phone_lookup throw by ContactUtilsSingleton.getContactByUri
			ContactDTO selected = null;
			ContactSugestionAdapter adapter = ((ContactSugestionAdapter)ComposeActivity.number.getAdapter());
			for(ContactDTO contact: adapter.getAll()){
				if(RotinesUtilsSingleton.comparePhones(contact.getPhone(), inText)){
					selected = contact;
					break;
				}
			}
			refreshContactInfo(selected);
		}
	}


	/**
	 * Used by the auto complete feature to return the contact name of the correspondent phone to the interface.
	 * 
	 * @param fetched 
	 */

	private void refreshContactInfo(ContactDTO fetched){
		String name = null;

		if(fetched!=null){
			name = fetched.getName();

			Uri selectedContact = ContactUtilsSingleton.getUriFromContactPhone(fetched.getPhone());
			//Uri selectedContact = fetched.getUri();

			String phone = fetched.getPhone();
			ChatActivity act = ComposeActivity.this;

			contactNameObserver = new ObservContactCompose(act, selectedContact, phone);
			getContentResolver().registerContentObserver(selectedContact, true, contactNameObserver); 
		}else if(contactNameObserver!=null){
			getContentResolver().unregisterContentObserver(contactNameObserver);
		}

		Message refreshMessage = new Message();
		refreshMessage.what = REFRESH_CONTACT_HANDLER_WHAT_1;
		Bundle bundleRefreshMessage = new Bundle();
		bundleRefreshMessage.putString(CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA, name);
		refreshMessage.setData(bundleRefreshMessage);

		super.refresherContact.sendMessage(refreshMessage);
	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++
	// Methods used by the select phone from contact list
	// ++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * Refreshes the contact data based on the Uri of the contact. 
	 * It enable the possibility to select another phone from the contact list.
	 * 
	 * In the event that the contact as no phone number no action is taken and the user is informed.
	 * 
	 * If a contact was found uses {@link #getComponentAt(String, String, String) openContactConversation} to apply it.
	 * 
	 * If more than a contact was found it uses {@link #choosePhoneFromContact(LinkedList<String>, String, String) choosePhoneFromContact}
	 * to choose which phone of the contact to select.
	 * 
	 * @param contactData
	 */
	private void refreshContactInfo(Uri contactData){
		Cursor c = null;
		c =  managedQuery(contactData, null, null, null, ContactUtilsSingleton.ORDER_POLICY);

		try {
			if(c!=null){
				try{
					if (c.moveToFirst()) {

						Object[] contact = new Object[2];

						String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));  
						String contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

						contact[0] = name;
						contact[1] = contactId;

						List<ContactDTO> contactList = ContactUtilsSingleton.getContacts(this, contactData);


						if(contactList.size()==0){
							Context ctx = ComposeActivity.this;
							String noPhone = String.format(ctx.getString(R.string.no_phone), name);
							Toast.makeText(ctx,  noPhone, Toast.LENGTH_LONG).show();
						}else if(contactList.size()==1){
							String phone =  ContactUtilsSingleton.filterContacts(ContactAttributeEnum.PHONE,contactList).getFirst();
							Message msg = new Message();
							Bundle data = new Bundle();
							data.putString(ContactUtilsSingleton.NAME_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER, name);
							data.putString(ContactUtilsSingleton.PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER, phone);
							msg.setData(data);
							ComposeActivity.setData.sendMessage(msg);
						}else{
							LinkedList<String> phones = ContactUtilsSingleton.filterContacts(ContactAttributeEnum.PHONE, contactList);
							String title =String.format(getString(R.string.which_phone), name);
							ContactUtilsSingleton.choosePhoneFromContact(this,title,phones, name, contactId, setData);
							
						}

					}
				}catch(StaleDataException e){}
				if(c!=null && !c.isClosed()){
					this.stopManagingCursor(c);
					c.close();
				}
			}else{
				throw new UnableToRetrieveDataException("Cursor is null");
			}
		} catch (UnableToRetrieveDataException e) {
			Toast.makeText(this, R.string.not_possible_try_again_later, Toast.LENGTH_LONG).show();
		}

	}
	
	private final static Handler setData = new Handler(){
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			String contactName = data.getString(ContactUtilsSingleton.NAME_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER);
			String contactPhone = data.getString(ContactUtilsSingleton.PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER);
			toCleaner.setExecuted(number);
			contactNameTemp.setText(contactName);
			number.setText(contactPhone);
		};
	};

}
