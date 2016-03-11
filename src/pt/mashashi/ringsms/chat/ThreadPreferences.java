package pt.mashashi.ringsms.chat;

import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.chat.compose.ComposeActivity;
import pt.mashashi.ringsms.chat.logged.LoggedActivity;
import pt.mashashi.ringsms.codemap.CodeMappingsEditListActivity;
import pt.mashashi.ringsms.codemap.CodeMappingsListActivity;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

public class ThreadPreferences extends PreferenceActivity{
	
	private Preference codeMapSetting;
	private Preference jitter;

	private boolean errorDetection;
	private int jitterValue;
	private long idCodeMappings;
	
	private String phone; 
	private boolean refreshCodeList;
	
	private boolean isUrgent;
	
	private static ThreadPreferences target;
	public static class NewCodeMapImportedRefreshBroadcast extends BroadcastReceiver{
		public NewCodeMapImportedRefreshBroadcast(){}
		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (ThreadPreferences.class) {
				if(target!=null){
					String associatedPhone=intent.getStringExtra(CodeMappingsEditListActivity.ASSOCIATED_PHONE_ID_NEW_IMPORTED_CODE_MAP);
					String phoneUsed=target.phone;
					if((phoneUsed!=null&&associatedPhone!=null)&&phoneUsed.equals(associatedPhone)){
						long idCodeMappings = intent.getLongExtra(CodeMappingsEditListActivity.CODE_MAP_ID_NEW_IMPORTED_CODE_MAP, 0);
						String title = target.setUsingIdCodeMappings(idCodeMappings);
						target.codeMapSetting.setTitle(target.getString(R.string.code_map)+" "+title);
						target.idCodeMappings=idCodeMappings;
					}
				}
			}
			
		}
	}
	
	
	
	
	
	public static final Handler handlerJitterSelection = new Handler(){
		public void handleMessage(Message msg) {
			synchronized (ThreadPreferences.class) {
				if(target!=null){	
					target.jitterValue = msg.getData().getInt(GeneralPreferences.NEW_VALUE_JITTER);
					String baseTitle = target.jitter.getContext().getString(R.string.jitter);
					target.jitter.setTitle(baseTitle+" "+setUsingIdJitter(target.jitter.getContext(), target.jitterValue));
				}
			}
		};
	};
	
	public static final int GET_NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1 = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		synchronized (ThreadPreferences.class) {
			target=this;
		}
		
		refreshCodeList=false;
		
		isUrgent=getIntent().getBooleanExtra(ChatActivity.IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, false);
		
		addPreferencesFromResource(R.xml.thread_preferences);
		phone = getIntent().getStringExtra(LoggedActivity.PHONE_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA);
		final int jitterValueRead = getIntent().getIntExtra(ChatActivity.JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
		
		int errorDetectionValue = getIntent().getIntExtra(ChatActivity.ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, GeneralPreferences.PREFERENCE_DEFAULT_CHECK_SUM_LENGTH);
		errorDetection = (errorDetectionValue==GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE);
		this.jitterValue = jitterValueRead;
		
		idCodeMappings = getIntent().getLongExtra(ComposeActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, 0);
		
		final CheckBoxPreference errorDetection = (CheckBoxPreference) findPreference(GeneralPreferences.CHECK_SUM);
		errorDetection.setChecked(this.errorDetection);
		errorDetection.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				ThreadPreferences.this.errorDetection = (Boolean) newValue;				
				return true;
			}
		});
		jitter = findPreference(GeneralPreferences.JITTER);
		jitter.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				GeneralPreferences.showChooseJitterOptions(ThreadPreferences.this, ThreadPreferences.this.jitterValue, handlerJitterSelection);
				return false;
			}
		});
		{
			String baseTitle = getString(R.string.jitter);
			jitter.setTitle(baseTitle+" "+setUsingIdJitter(this, jitterValueRead));
		}
		
		
		codeMapSetting = findPreference("code_map");
		codeMapSetting.setTitle(getString(R.string.code_map)+" "+setUsingIdCodeMappings(idCodeMappings));
		codeMapSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(ThreadPreferences.this, CodeMappingsListActivity.class);
				intent.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, idCodeMappings);
				intent.putExtra(LoggedActivity.PHONE_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, phone);
				startActivityForResult(intent, GET_NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1);
				return false;
			}
		});
		
		CheckBoxPreference sendUrgent = (CheckBoxPreference)findPreference("send_urgent");
		performChangeUrgent(isUrgent, errorDetection.isChecked(), errorDetection);
		sendUrgent.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			boolean beforeErrorCorrection = ThreadPreferences.this.errorDetection;
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				isUrgent = (Boolean)newValue;
				beforeErrorCorrection = performChangeUrgent(isUrgent, beforeErrorCorrection, errorDetection);
				return true;
			}
		});
		sendUrgent.setChecked(isUrgent);
	}
	
	private boolean performChangeUrgent(boolean isUrgent, boolean beforeErrorCorrection, CheckBoxPreference errorDetection){
		if(isUrgent){
			beforeErrorCorrection=errorDetection.isChecked();
			errorDetection.setChecked(false);
		}else{
			errorDetection.setChecked(beforeErrorCorrection);
		}
		errorDetection.setEnabled(!isUrgent);
		return beforeErrorCorrection;
	}
	
	
	private String setUsingIdCodeMappings(long idCodeMappings){
		String usingCodeMap = "";
		if(idCodeMappings!=0){
			String name = CodeMappingsDataSource.getInstance(this).getCodeMapping(idCodeMappings).getMappingsName();
			usingCodeMap = String.format(getString(R.string.using), name);
		}
		return usingCodeMap;
	}
	
	private static String setUsingIdJitter(Context ctx, int jitter){
		String usingCodeMap = "";
		String name = ctx.getString(ThreadJitterEnum.values()[jitter].getLabel());		
		usingCodeMap = String.format(ctx.getString(R.string.using), name);
		return usingCodeMap;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {	
			case (GET_NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1):{
				idCodeMappings = data.getLongExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, this.idCodeMappings);
				refreshCodeList = data.getBooleanExtra(ChatActivity.REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, false);
				codeMapSetting.setTitle(getString(R.string.code_map)+" "+setUsingIdCodeMappings(idCodeMappings));
				break;
			}
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(idCodeMappings!=CodeMappingsListActivity.NOT_WAITING_FOR_RESULT){
				Intent intentReturn = new Intent();
				intentReturn.putExtra(ChatActivity.JITTER_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, jitterValue);
				int checkSumLength = 0;
				if(errorDetection){
					checkSumLength=GeneralPreferences.MOCKED_UP_CHECKSUM_ON_OFF_SIZE;
				}
				intentReturn.putExtra(ChatActivity.ERROR_CORRECTION_SPECIFIC_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, checkSumLength);
				intentReturn.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, idCodeMappings);
				intentReturn.putExtra(ChatActivity.REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, refreshCodeList);
				
				intentReturn.putExtra(ChatActivity.IS_URGENT_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, isUrgent);
				
				setResult(Activity.RESULT_OK, intentReturn);
			}
			this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
