package pt.mashashi.ringsms.threads;

import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.autostart.PhoneStateListenerStarterService;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.codemap.CodeMappingsListActivity;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import pt.mashashi.ringsms.interfazze.InterfazzeStarterService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.ListView;
import android.widget.Toast;

public class GeneralPreferences extends PreferenceActivity{
	
	
	private static GeneralPreferences target;
	
	public final static String ACCESS_APP_DATA_PASSWORD = "accessAppDataPassword";
	
	// Preferences names
	public final static String					AUTOSTART = "autostart";
	public final static String		SERVICE_ENABLE_LAUNCH = "service_enable_launch";
	public final static String			  		CHECK_SUM = "check_sum";
	public final static String					   JITTER = "jitter";
	public final static String		  INTERFAZZE_PASSWORD = "interfazze_password";
	
	// ++++++++++++++++++++++++++
	// Preferences Default Values Also Defined in the folder res/xml
	// ++++++++++++++++++++++++++
	
	public static final boolean			 PREFERENCE_DEFAULT_AUTOSTART = true; 
	
	private static final boolean 						 CHECK_SUM_ON = false;
	public static final int 		   MOCKED_UP_CHECKSUM_ON_OFF_SIZE = 3;
	public static final int		  PREFERENCE_DEFAULT_CHECK_SUM_LENGTH = CHECK_SUM_ON?MOCKED_UP_CHECKSUM_ON_OFF_SIZE:0;
	
	public static final int					PREFERENCE_DEFAULT_JITTER = 2; 
	
	private SharedPreferences.Editor spEditorJitter;
	
	private CheckBoxPreference serviceEnableLaunch;
	public static final int SERVICE_CHECKBOX_UPDATE=0;
	private static final Handler refreshCheckBoxServiceStatus = new Handler(){
		public void handleMessage(Message msg) {
			if(msg.what==SERVICE_CHECKBOX_UPDATE){
				synchronized (GeneralPreferences.class) {
					if(target!=null){
						target.serviceEnableLaunch.setChecked(PhoneStateListenerStarterService.getIsRunning());
					}
				}
			}
		};
	};
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.general_preferences);
		
		synchronized (GeneralPreferences.class) {
			target=this;
		}
		
		if(spEditorJitter==null){
			spEditorJitter = PreferenceManager.getDefaultSharedPreferences(this).edit();
		}
		
		
		
		/*
		CheckBoxPreference errorDetection = (CheckBoxPreference) findPreference(ERROR_DETECTION);
		errorDetection.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean checked = Boolean.parseBoolean(newValue.toString());
				// TODO
				return true;
			}
		});
		
		CheckBoxPreference autostart = (CheckBoxPreference) findPreference(AUTOSTART);
		autostart.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean checked = Boolean.parseBoolean(newValue.toString());
				// TODO
				return true;
			}
		});*/
		serviceEnableLaunch = (CheckBoxPreference) findPreference(SERVICE_ENABLE_LAUNCH);
		boolean serviceRunning = PhoneStateListenerStarterService.getIsRunning();
		serviceEnableLaunch.setChecked(serviceRunning);
		serviceEnableLaunch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean checked = Boolean.parseBoolean(newValue.toString());
				if(checked){
					Intent registerPhoneListener = new Intent(GeneralPreferences.this, PhoneStateListenerStarterService.class);
					GeneralPreferences.this.startService(registerPhoneListener);
				}else{
					Intent stopPhoneListener = new Intent(GeneralPreferences.this, PhoneStateListenerStarterService.class);
					GeneralPreferences.this.stopService(stopPhoneListener);
				}
				return false;
			}
		});
		
		serviceChecker = new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized(GeneralPreferences.class){
					try {
						while(!GeneralPreferences.this.finished){
							GeneralPreferences.class.wait();
							refreshCheckBoxServiceStatus.sendEmptyMessage(SERVICE_CHECKBOX_UPDATE);
						}
					} catch (InterruptedException e) {
						// This is normal when the activity finished interrupt() method will be called on this thread
						MyLog.d(ThreadsActivity.DEBUG_TAG, "Service watcher is stopping now");
					}
				}
			}
		});
		serviceChecker.start();
		
		jitter = findPreference(JITTER);
		jitter.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(GeneralPreferences.this);
				int selected = sp.getInt(GeneralPreferences.JITTER, PREFERENCE_DEFAULT_JITTER);
				showChooseJitterOptions(GeneralPreferences.this, selected, refreshJitterPreference);
				return false;
			}
		});
		int jitterId = PreferenceManager.getDefaultSharedPreferences(this).getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
		String label = getString(ThreadJitterEnum.values()[jitterId].getLabel());
		jitter.setTitle(jitter.getTitle()+" "+String.format(getString(R.string.using), label));
		
		Preference codeMapSetting = findPreference("code_map");
		codeMapSetting.setTitle(codeMapSetting.getTitle());
		codeMapSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(GeneralPreferences.this, CodeMappingsListActivity.class);
				intent.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, (long)-1);
				startActivity(intent);
				return false;
			}
		});
		
		
		EditTextPreference interfazzePassword = (EditTextPreference) findPreference(INTERFAZZE_PASSWORD);
		interfazzePassword.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String newPassword = (String)newValue;
				String oldPassword =((EditTextPreference)preference).getText();
				//
				if(newPassword.length()!=0 && newPassword.length()<6){
					Toast.makeText(GeneralPreferences.this, R.string.signinng_password, Toast.LENGTH_LONG).show();
					return false;
				}
				Intent registerInterfazzeListner = new Intent(GeneralPreferences.this, InterfazzeStarterService.class);
				if(newPassword.length()==0 && oldPassword.length()!=0){
					/* Stop service */
					GeneralPreferences.this.stopService(registerInterfazzeListner);
				}else if(newPassword.length()!=0 && oldPassword.length()==0){
					/* Start service */
					GeneralPreferences.this.startService(registerInterfazzeListner);
				}
				return true;
			}
		});
	}
	
	private Thread serviceChecker;
	private boolean finished;
	@Override
	protected void onDestroy() {
		super.onDestroy();
		serviceChecker.interrupt();
		finished=true;
	}
	@Override
	protected void onResume() {
		super.onResume();
		finished=false;
	}
	
	public static final String NEW_VALUE_JITTER = "newValueJitter";
	public static void showChooseJitterOptions(Context ctx, int selected, final  Handler publish){
		final String[] jitterLabels = ThreadJitterEnum.getAllLabel(ctx);
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.select_the_connection_jitter);
		
		builder.setSingleChoiceItems(jitterLabels, selected, null)
		.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,int which) {
				if(publish!=null){
					ListView lw = ((AlertDialog)dialog).getListView();
					int index = lw.getCheckedItemPosition();
					
					Message msg = new Message();
					msg.what=0;
					Bundle bundle = new Bundle();
					bundle.putInt(NEW_VALUE_JITTER, index);
					msg.setData(bundle);
					
					publish.sendMessage(msg);
				}
			}
		}).setNegativeButton(R.string.cancel, null).show();
	}
	private Preference jitter;
	private static final Handler refreshJitterPreference = new Handler(){
		public void handleMessage(Message msg) {
			synchronized (GeneralPreferences.class) {
				if(target!=null){
					int newJitter = msg.getData().getInt(NEW_VALUE_JITTER, PREFERENCE_DEFAULT_JITTER);
					target.spEditorJitter.putInt(JITTER, newJitter);
					target.spEditorJitter.commit();
					String label = target.getString(ThreadJitterEnum.values()[newJitter].getLabel());
					target.jitter.setTitle(target.getString(R.string.jitter)+" "+String.format(target.getString(R.string.using), label));
				}
			}
		};
	};
	
}
