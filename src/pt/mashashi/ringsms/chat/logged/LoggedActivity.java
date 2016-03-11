package pt.mashashi.ringsms.chat.logged;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ContactDTO;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.MsgDAO;
import pt.mashashi.ringsms.chat.ThreadPreferences;
import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.MsgStatusEnum;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.talk.MsgRefreshBroadcast;
import pt.mashashi.ringsms.talk.OnNewRefreshMessageListener;
import pt.mashashi.ringsms.threads.ThreadDAO;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class LoggedActivity extends ChatActivity implements OnNewRefreshMessageListener{

	private String phoneNumber;

	private TextView contactNumber;
	private ObservContactLogged observContactLogged;

	public final static String THREAD_PHONE_NUMBER = "phoneNumber";
	public final static String THREAD_JITTER = "threadJitter";
	public final static String THREAD_CHECKSUM_LENGTH = "checkSumLength";
	public final static String THREAD_ID_CODE_MAPPINGS = "idCodeMappings";

	public static final String PHONE_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA = "phoneNumber";

	public static final String CONTACT_PHOTO_HANDLE_1 = "contactPhoto";

	private AtomicBoolean isVisible;

	/**
	 * Loads the saved scratch into the message EditText
	 *
	 */
	public class LoadScratch extends AsyncTask<String, Void, String>{

		private EditText target;
		private Context ctx;

		public LoadScratch(EditText target, Context ctx){
			this.target = target;
			this.ctx = ctx;
		}

		@Override
		protected String doInBackground(String... phones) {
			return ThreadDataSource.getInstance(ctx).getThread(phones[0]).getScratch();
		}
		@Override
		protected void onPostExecute(String result) {
			target.setText(result);
			target.setSelection(result.length());
		}
	}

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title_chat);

		assignFunctionToBack(R.id.back);


		isVisible = new AtomicBoolean(false);

		ContactDTO fetched = null;

		{ 	// Fetch contact info
			phoneNumber = getIntent().getStringExtra(LoggedActivity.THREAD_PHONE_NUMBER);

			if(phoneNumber==null)
				throw new IllegalArgumentException();

			new MessagesRefresher(messageAdapter).execute(phoneNumber);
			new LoadScratch(super.getEdit(), this).execute(phoneNumber);


			try {
				fetched = ContactUtilsSingleton.getContactFromPhone(this, phoneNumber);
			} catch (UnableToRetrieveDataException e) {
				// Go ahead the interface will be presented displaying only the contact number...
			}
			if(fetched==null){
				fetched = new ContactDTO(null, phoneNumber, null);
			}
		}

		{
			ThreadDAO thisThread = ThreadDataSource.getInstance(this).getThread(phoneNumber);
			doSwapOutBoxes(thisThread.getCodeMappingsId());


			super.jitter=getIntent().getIntExtra(LoggedActivity.THREAD_JITTER, -1);
			super.checkSumLength=getIntent().getIntExtra(LoggedActivity.THREAD_CHECKSUM_LENGTH, -1);
			super.idCodeMappings=getIntent().getLongExtra(LoggedActivity.THREAD_ID_CODE_MAPPINGS, -1);

			if(super.jitter==-1 && super.checkSumLength==-1 && super.idCodeMappings==-1){
				// This activity was recreated by pressing the pending intent notification and the values were not passed

				super.jitter=thisThread.getJitter();
				super.checkSumLength=thisThread.getCheckSum();
				super.idCodeMappings=thisThread.getCodeMappingsId();
			}
		}
		
		{ // Set name
			TextView contactName = (TextView) findViewById(R.id.contact_name);
			setName(fetched.getName(), contactName);
		}

		{ // Set photo
			Bitmap photo = ContactUtilsSingleton.getContactPhoto(this.getContentResolver(), fetched.getUri());
			final ImageView contactPhoto = (ImageView) findViewById(R.id.contact_photo);
			setContactPhoto(photo, contactPhoto);
		}

		contactNumber = (TextView) findViewById(R.id.contact_number);
		contactNumber.setText(fetched.getPhone());

		Uri contactPhoneUri = ContactUtilsSingleton.getUriFromContactPhone(fetched.getPhone());
		Uri uriForRegistration = ContactUtilsSingleton.getContactUriForObserver(contactPhoneUri);

		observContactLogged = new ObservContactLogged(this, contactPhoneUri, fetched.getPhone());
		getContentResolver().registerContentObserver(uriForRegistration, true, observContactLogged);

		MsgRefreshBroadcast.register((OnNewRefreshMessageListener)this);
	}

	// ++++++++++++++++++
	// Overridden methods
	// ++++++++++++++++++
	@SuppressLint("HandlerLeak")
	@Override
	protected Handler getRefresherContactHandler(){
		return new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.what==REFRESH_CONTACT_HANDLER_WHAT_1){
					String name = msg.getData().getString(LoggedActivity.CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA);
					byte[] photoBytes = msg.getData().getByteArray(LoggedActivity.CONTACT_PHOTO_HANDLE_1);
					Bitmap photo = null;
					if(photoBytes!=null){
						photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
					}
					final TextView contactName = (TextView) findViewById(R.id.contact_name);
					final ImageView contactPhoto = (ImageView) findViewById(R.id.contact_photo);
					setName(name, contactName);
					setContactPhoto(photo, contactPhoto);
				}
			}
		};
	}
	@Override
	public String getContactNumber() {
		return contactNumber.getText().toString();
	}
	@Override
	protected void sendMessageAction(String message, String number, Date timeStamp) {
		super.mOutEditText.setText("");
		messageAdapter.addItem(new MsgDAO(number,message,MsgDirectionEnum.OUTGOING, timeStamp, MsgStatusEnum.SUCCESS));
	}
	@Override
	protected void backAction() {
		EditText edit = getEdit();
		if(edit.getClass().equals(EditText.class)){
			ThreadDataSource.getInstance(LoggedActivity.this).updateThreadScratch(phoneNumber,edit.getText().toString());
		}
	};
	@Override
	protected void clearAllFields() {
		super.mOutEditText.setText("");	
	}
	@Override
	protected void whatToDoOnNewCodeMap(String phone, long newAssociatedCode) {
		if(RotinesUtilsSingleton.comparePhones(phone, phoneNumber)){
			if(isOutInMultiModeBox()){
				super.idCodeMappings = newAssociatedCode;
				super.refreshBoxes(idCodeMappings);
			}
		}
	}
	// +++++++++++++++++++
	// Activity life cycle
	// +++++++++++++++++++

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(observContactLogged!=null)
			getContentResolver().unregisterContentObserver(observContactLogged);
		MsgRefreshBroadcast.unregister((OnNewRefreshMessageListener)this);
	}
	@Override
	protected void onResume() {
		super.onResume();
		(new Thread(){
			@Override
			public void run() {
				if(ThreadDataSource.getInstance(LoggedActivity.this).updateLastAccess(phoneNumber)==1){
					refreshMessage();
				}
			};
		}).start();
		isVisible.set(true);
	}
	private void refreshMessage(){
		MsgRefreshBroadcast.sendRefreshNotice(LoggedActivity.this, phoneNumber, MsgDirectionEnum.INCOMING);
		// Inform the notification arrival messsage MsgRefreshBroadcast that the message was read
		Intent intentEndMessage = new Intent(BroadcastEvents.MESSAGE_READ);
		intentEndMessage.putExtra(MsgRefreshBroadcast.REFRESH_NUMBER, phoneNumber);
		ThreadDataSource.getInstance(LoggedActivity.this).updateLastAccess(phoneNumber);
		LoggedActivity.this.sendBroadcast(intentEndMessage);
	}
	@Override
	protected void onPause() {
		super.onPause();
		isVisible.set(false);
	}
	// ++++++++++++++++
	// The options menu
	// ++++++++++++++++

	private final static int		DELETE_MESSAGES = CLEAR_ALL_OPT+1;
	private final static int		DELETE_OPT = DELETE_MESSAGES+1;



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		String[] menuItems = getResources().getStringArray(R.array.chat_option_menu);
		menu.add(Menu.NONE, DELETE_MESSAGES, DELETE_MESSAGES, menuItems[DELETE_MESSAGES]).setIcon(R.drawable.ic_menu_end_conversation);
		menu.add(Menu.NONE, DELETE_OPT, DELETE_OPT, menuItems[DELETE_OPT]).setIcon(android.R.drawable.ic_menu_delete);

		return super.onCreateOptionsMenu(menu); 
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean consumed=false;		
		switch (item.getItemId()) {
		case DELETE_OPT: {
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete_thread_title_selected)
			.setMessage(R.string.delete_thread_details)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent();
					setResult(Activity.RESULT_OK, intent);
					ThreadDataSource.getInstance(LoggedActivity.this).deleteThread(phoneNumber);

					// Inform the notification arrival messsage MsgRefreshBroadcast that the message was read
					Intent intentEndMessage = new Intent(BroadcastEvents.MESSAGE_READ);
					intentEndMessage.putExtra(MsgRefreshBroadcast.REFRESH_NUMBER, phoneNumber);
					LoggedActivity.this.sendBroadcast(intentEndMessage);

					LoggedActivity.this.finish();   
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
			consumed = true;
			break;
		}
		case SETTINGS_OPT: {
			Intent intentConfigs = new Intent(this, ThreadPreferences.class);
			intentConfigs.putExtra(JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, jitter);
			intentConfigs.putExtra(ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, checkSumLength);
			intentConfigs.putExtra(ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, idCodeMappings);
			intentConfigs.putExtra(PHONE_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, phoneNumber);

			intentConfigs.putExtra(IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, isUrgent);

			startActivityForResult(intentConfigs, PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1);
			consumed = true;
			break;
		}
		case DELETE_MESSAGES:{
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete_all_messages)
			.setMessage(R.string.delete_all_messages_details)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ThreadDataSource.getInstance(LoggedActivity.this).deleteAllMessagesFromThread(phoneNumber);
					new MessagesRefresher(messageAdapter).execute(phoneNumber);
					MsgRefreshBroadcast.sendRefreshNotice(LoggedActivity.this, phoneNumber, null); 
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();

			consumed = true;
			break;
		}
		default: { break; }
		}

		if(!consumed){
			consumed = super.onOptionsItemSelected(item);
		}
		return consumed;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {	
		case (PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1):{

			// This is already done on super!
			//super.jitter = data.getIntExtra(JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, super.jitter);
			//super.checkSumLength = data.getIntExtra(ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, super.checkSumLength);
			//super.idCodeMappings = data.getLongExtra(ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, super.idCodeMappings);

			ThreadDataSource.getInstance(this).updateThreadCheckSumLength(phoneNumber, super.checkSumLength);

			ThreadDataSource.getInstance(this).updateThreadJitter(phoneNumber, super.jitter);

			MsgRefreshBroadcast.sendRefreshNotice(this.getApplicationContext(), phoneNumber, MsgDirectionEnum.INCOMING);
			break;
		}
		}


	}




	public void setContactPhoto(Bitmap photo, ImageView viewPhoto){
		if(photo==null){
			photo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_pressed);
		}
		viewPhoto.setImageBitmap(photo);
	}

	@Override
	public void onNewMessage(String phone, MsgDirectionEnum direction) {
		if(RotinesUtilsSingleton.comparePhones(phone, phoneNumber)){
			if(direction.equals(MsgDirectionEnum.OUTGOING)){
				ThreadDataSource.getInstance(LoggedActivity.this).updateLastAccess(phoneNumber);
			}
			new MessagesRefresher(messageAdapter).execute(phoneNumber);
		}
	}
	@Override
	public void onNewMessageNotificationPlaced(String phone) {
		if(isVisible.get()){
			refreshMessage();
		}
	}
}
