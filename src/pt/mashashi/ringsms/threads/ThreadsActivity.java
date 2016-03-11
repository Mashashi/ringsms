package pt.mashashi.ringsms.threads;

import java.util.LinkedList;
import java.util.List;

import com.google.android.vending.licensing.util.Base64;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.autostart.PhoneStateListenerStarterService;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.compose.ComposeActivity;
import pt.mashashi.ringsms.chat.logged.LoggedActivity;
import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.interfazze.InterfazzeStarterService;
import pt.mashashi.ringsms.interfazze.ListenForMessageRequestBroacast;
import pt.mashashi.ringsms.talk.MsgRefreshBroadcast;
import pt.mashashi.ringsms.talk.OnNewRefreshMessageListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ThreadsActivity extends Activity implements OnNewRefreshMessageListener{

	public final static String DEBUG_TAG = "debug_dev";

	public final static int NEW_THREAD_ACTIVITY_REQUEST_1 = 0;
	public final static String NEW_THREAD_PHONE_NEW_THREAD_ACTIVITY_REQUEST_1_DATA = "phone";

	public final static int LOGGED_THREAD_ACTIVITY_REQUEST_2 = 1;

	private ThreadsAdapter threadsAdapter;

	private ContentObserver contentObserver;

	public static final int gps 	= 709065993; 
	public static final int gpm 	= 1753485592;
	public static final int gpi 	= -1528771871;
	public static final int gc		= -1893527162;
	public static final int gpk 	= 1128691107;
	public static final int what 	= 2497506;
	public static final int cer 	= -2086678445;

	public static String checkIntegrity(String gps, String gpm, String gpi, String gc, String gpk, String what, String cer) throws Exception{
		if(gps.hashCode()!=ThreadsActivity.gps){
			throw new Exception();
		}
		if(gpm.hashCode()!=ThreadsActivity.gpm){
			throw new Exception();
		}
		if(gc.hashCode()!=ThreadsActivity.gc){
			throw new Exception();
		}
		if(gpk.hashCode()!=ThreadsActivity.gpk){
			throw new Exception();
		}
		if(what.hashCode()!=ThreadsActivity.what){
			throw new Exception();
		}
		if(cer.hashCode()!=ThreadsActivity.cer){
			throw new Exception();
		}
		return new String(Base64.decode(what), "UTF-8");
	}


	public ThreadsActivity() {}

	private class ThreadsRefresher extends AsyncTask<Void, Void, List<ThreadDAO>>{
		private ThreadsAdapter threadsAdapter;
		public ThreadsRefresher(ThreadsAdapter threadsAdapter){
			this.threadsAdapter = threadsAdapter;
		}
		@Override
		protected List<ThreadDAO> doInBackground(Void... params) {
			ThreadDataSource instance = ThreadDataSource.getInstance(ThreadsActivity.this);
			List<ThreadDAO> list = instance.listThreads();
			return list;
		}
		@Override
		protected void onPostExecute(List<ThreadDAO> result) {
			threadsAdapter.newData(result);
		}
	} 

	/*static{
		Security.removeProvider("BC");
		int pos = Security.addProvider(new BouncyCastleProvider());
		MyLog.d(ThreadsActivity.DEBUG_TAG, pos+"");
	}*/

	/*try {
	Log.d(ThreadsActivity.DEBUG_TAG, ""+Crypto.encrypt("X.509", "22b7b4c95eb1498588846318ffbfdf16", "AES").hashCode());
	} catch (GeneralSecurityException e) {
		e.printStackTrace();
	}*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Set default values in any case will not set values that are already defined.
		//The readAgain flag makes it go throw all preferences in order to make sure that 
		//if one preference was unsetted it will assume its default value again
		//if false user preferences won't be overwritten
		PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);

		synchronized(ThreadsActivity.class){
			ctx = this;
		}

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.threads);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title_logs);

		RelativeLayout titleLogs = (RelativeLayout)findViewById(R.id.title_logs);
		titleLogs.setLongClickable(true);
		titleLogs.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					v.setBackgroundResource(R.drawable.new_message_active);
				}else if(event.getAction()==MotionEvent.ACTION_UP){
					v.setBackgroundResource(R.drawable.new_message_rest);
					Intent intent = new Intent(ThreadsActivity.this, ComposeActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivityForResult(intent, NEW_THREAD_ACTIVITY_REQUEST_1);
				}
				return false;
			}
		});

		List<ThreadDAO> messages = new LinkedList<ThreadDAO>();

		ListView threadsView = (ListView) this.findViewById(R.id.threads);

		LayoutInflater inflater = this.getLayoutInflater();
		View footer = inflater.inflate(R.layout.threads_footer, null);
		TextView footerThreadCount = ((TextView)footer.findViewById(R.id.counter_threads));
		threadsView.addFooterView(footer);
		threadsAdapter = new ThreadsAdapter(this, messages, footerThreadCount);
		threadsView.setAdapter(threadsAdapter);
		threadsAdapter.notifyDataSetChanged();

		new ThreadsRefresher(threadsAdapter).execute();

		threadsView.setDivider(null);
		threadsView.setDividerHeight(5);
		threadsView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View clickedView, int positionView, long rowId) {
				if(positionView<threadsAdapter.getCount()){

					Intent intent = new Intent(ThreadsActivity.this, LoggedActivity.class);
					intent.putExtra(LoggedActivity.THREAD_PHONE_NUMBER, threadsAdapter.getItem(positionView).getNumber());
					final int jitter = threadsAdapter.getItem(positionView).getJitter();
					final int checkSum = threadsAdapter.getItem(positionView).getCheckSum();
					final long idCodeMappings = threadsAdapter.getItem(positionView).getCodeMappingsId();

					intent.putExtra(LoggedActivity.THREAD_JITTER, jitter);
					intent.putExtra(LoggedActivity.THREAD_CHECKSUM_LENGTH, checkSum);
					intent.putExtra(LoggedActivity.THREAD_ID_CODE_MAPPINGS, idCodeMappings);

					startActivityForResult(intent, LOGGED_THREAD_ACTIVITY_REQUEST_2);
				}
			}
		});
		registerForContextMenu(threadsView);
		MsgRefreshBroadcast.register(this);


		contentObserver = new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {
				// This could be optimized
				new ThreadsRefresher(threadsAdapter).execute();
			}
		};

		getContentResolver().registerContentObserver(
				ContactUtilsSingleton.getUriAllContacts(), true, contentObserver
				);

	}

	@Override
	protected void onStart() {
		super.onStart();
		// Message is from interface launch Compose with special flags and ask to automatic send
		Intent intent = getIntent();

		String phone = intent.getStringExtra(ListenForMessageRequestBroacast.PHONE_NUMBER_ACTIVITY_REQUEST_1);
		String message = intent.getStringExtra(ListenForMessageRequestBroacast.MESSAGE_ACTIVITY_REQUEST_1);
		long idCodeMappings = intent.getLongExtra(ListenForMessageRequestBroacast.CODE_MAPPINGS_ACTIVITY_REQUEST_1, 0);

		if(phone!= null && message!=null){
			// If we don't remove the the return of the ComposeActivity will result in another unwanted start of ComposeActivity
			intent.removeExtra(ListenForMessageRequestBroacast.PHONE_NUMBER_ACTIVITY_REQUEST_1);
			intent.removeExtra(ListenForMessageRequestBroacast.MESSAGE_ACTIVITY_REQUEST_1);
			intent.removeExtra(ListenForMessageRequestBroacast.CODE_MAPPINGS_ACTIVITY_REQUEST_1);

			Intent intentNew = new Intent(ThreadsActivity.this, ComposeActivity.class);
			intentNew.putExtra(ListenForMessageRequestBroacast.PHONE_NUMBER_ACTIVITY_REQUEST_1, phone);
			intentNew.putExtra(ListenForMessageRequestBroacast.MESSAGE_ACTIVITY_REQUEST_1, message);
			intentNew.putExtra(ListenForMessageRequestBroacast.CODE_MAPPINGS_ACTIVITY_REQUEST_1, idCodeMappings);
			intentNew.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intentNew, NEW_THREAD_ACTIVITY_REQUEST_1);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		MsgRefreshBroadcast.unregister(this);
	}

	public static Context ctx;
	private static Handler launchService = new Handler(){
		public void handleMessage(android.os.Message msg) {

			synchronized(ThreadsActivity.class){
				if(ctx!=null){
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
					boolean launch_service = sp.getBoolean(GeneralPreferences.AUTOSTART, true);
					if(launch_service && !PhoneStateListenerStarterService.getIsRunning()){
						Intent intent = new Intent(ctx, PhoneStateListenerStarterService.class);
						ctx.startService(intent);
					}

					String interfazze_password = sp.getString(GeneralPreferences.INTERFAZZE_PASSWORD, "");
					if(interfazze_password.length()!=0 && !InterfazzeStarterService.isRunning()){
						Intent registerPhoneLister = new Intent(ctx, InterfazzeStarterService.class);
						ctx.startService(registerPhoneLister);
					}
				}
			}
		};
	};
	@Override
	protected void onResume() {
		super.onResume();
		launchService.sendEmptyMessage(0);
	}





	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){

		switch(requestCode){
		case NEW_THREAD_ACTIVITY_REQUEST_1:{
			if(resultCode==Activity.RESULT_OK){
				String phone = data.getStringExtra(NEW_THREAD_PHONE_NEW_THREAD_ACTIVITY_REQUEST_1_DATA);

				if(phone!=null){

					new ThreadsRefresher(threadsAdapter).execute();

					Intent intent = new Intent(ThreadsActivity.this, LoggedActivity.class);
					ThreadDAO thread = ThreadDataSource.getInstance(this).getThread(phone);

					intent.putExtra(LoggedActivity.THREAD_PHONE_NUMBER, thread.getNumber());
					intent.putExtra(LoggedActivity.THREAD_JITTER, thread.getJitter());
					intent.putExtra(LoggedActivity.THREAD_CHECKSUM_LENGTH, thread.getCheckSum());
					intent.putExtra(LoggedActivity.THREAD_ID_CODE_MAPPINGS, thread.getCodeMappingsId());

					startActivityForResult(intent, LOGGED_THREAD_ACTIVITY_REQUEST_2);

				}else{
					// Failed to insert it on the database do nothing
				}
			}
			break;
		}
		case LOGGED_THREAD_ACTIVITY_REQUEST_2:{
			if(resultCode==Activity.RESULT_OK){
				// Message was deleted
				new ThreadsRefresher(threadsAdapter).execute();
			}else if(resultCode==Activity.RESULT_CANCELED){
				// Back header bottom falls hear as the back arrow to
				MyLog.d(ThreadsActivity.DEBUG_TAG, "Result Cancelled");
			}
			break;
		}
		}
	}





	final int 	DELETE_ALL_OPT = Menu.NONE;
	final int 	  SETTINGS_OPT = DELETE_ALL_OPT+1;
	final int		  INFO_OPT = SETTINGS_OPT+1;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		String[] menuItems = getResources().getStringArray(R.array.threads_option_menu);
		menu.add(Menu.NONE, DELETE_ALL_OPT, DELETE_ALL_OPT, menuItems[DELETE_ALL_OPT]).setIcon(android.R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, SETTINGS_OPT, SETTINGS_OPT, menuItems[SETTINGS_OPT]).setIcon(android.R.drawable.ic_menu_manage);
		menu.add(Menu.NONE, INFO_OPT, INFO_OPT, menuItems[INFO_OPT]).setIcon(android.R.drawable.ic_dialog_info);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ALL_OPT: {
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete_all_threads_title)
			.setMessage(R.string.delete_all_threads_details)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Inform the notification arrival messsage MsgRefreshBroadcast that the message was read
					List<String> phones =threadsAdapter.getAllPhoneNumbers();
					for(String phone: phones){
						Intent intentEndMessage = new Intent(BroadcastEvents.MESSAGE_READ);
						intentEndMessage.putExtra(MsgRefreshBroadcast.REFRESH_NUMBER, phone);
						ThreadsActivity.this.sendBroadcast(intentEndMessage);
					}

					ThreadDataSource.getInstance(ThreadsActivity.this).deleteAllThreads();
					new ThreadsRefresher(threadsAdapter).execute();

				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
			break;
		}
		case INFO_OPT:{
			new AboutDialog(this).show();
			break;
		}
		case SETTINGS_OPT:{
			Intent intentConfigs = new Intent(this, GeneralPreferences.class);
			startActivity(intentConfigs);
			break;
		}
		default: 
			throw new IllegalArgumentException();
		}
		return super.onOptionsItemSelected(item);
	}





	private final int DELETE_OPT = Menu.NONE;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId()==R.id.threads) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			RelativeLayout target = (RelativeLayout) info.targetView;
			TextView textView = (TextView)(target.findViewById(R.id.name));
			if(textView!=null){
				String name = textView.getText().toString();
				String[] menuItems = getResources().getStringArray(R.array.thread_context);
				menu.setHeaderTitle(String.format(getString(R.string.thread_options_title), name));
				menu.add(Menu.NONE, DELETE_OPT, DELETE_OPT, menuItems[DELETE_OPT]);
			}else{
				// This is the footer view
			}
		}
	}
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch(item.getItemId()){
		case DELETE_OPT:{
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			String name = ((TextView)((RelativeLayout) info.targetView).findViewById(R.id.name)).getText().toString();
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(String.format(getString(R.string.delete_thread_menu), name))
			.setMessage(R.string.delete_thread_details)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
					ThreadDAO thread = threadsAdapter.getItem(info.position);
					ThreadDataSource.getInstance(ThreadsActivity.this).deleteThread(thread.getNumber());
					new ThreadsRefresher(threadsAdapter).execute();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
			break;
		}

		}
		return false;
	}

	@Override
	public void onNewMessage(String phone, MsgDirectionEnum direction) {
		new ThreadsRefresher(threadsAdapter).execute();
	}

	@Override
	public void onNewMessageNotificationPlaced(String phone) {/*Do nothing*/}

}
