package pt.mashashi.ringsms.codemap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.google.android.vending.licensing.util.Base64;

import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ContactDTO;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.ContactDTO.ContactAttributeEnum;
import pt.mashashi.ringsms.chat.logged.LoggedActivity;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.RingSMSDBHelper;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.talk.MsgRefreshBroadcast;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class CodeMappingsListActivity extends ListActivity {

	private static CodeMappingsListActivity target;
	public static class NewCodeMapImportedRefreshBroadcast extends BroadcastReceiver{
		public NewCodeMapImportedRefreshBroadcast(){}
		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {

			synchronized (NewCodeMapImportedRefreshBroadcast.class) {
				if(target!=null){
					/*String associatedPhone = intent.getStringExtra(CodeMappingsEditListActivity.ASSOCIATED_PHONE_ID_NEW_IMPORTED_CODE_MAP);
					String phoneUsed = target.phone;
					if((phoneUsed!=null&&associatedPhone!=null) && phoneUsed.equals(associatedPhone)){
						target.selectedIdCodeMappins = intent.getLongExtra(CodeMappingsEditListActivity.CODE_MAP_ID_NEW_IMPORTED_CODE_MAP, 0);
					}*/
					new LoadCodeMaps(target, (ArrayAdapter<CodeMappingsDAO>)target.getListAdapter()).execute();
				}
			}

		}
	}

	private String phone;

	public static final int NOT_WAITING_FOR_RESULT=-1;

	private long selectedIdCodeMappins; 
	private List<Long> editedIdsCodeMappings;


	private static class LoadCodeMaps extends AsyncTask<Void, Void, List<CodeMappingsDAO>>{
		private ArrayAdapter<CodeMappingsDAO> adapter;
		private CodeMappingsListActivity ctx;
		public LoadCodeMaps(CodeMappingsListActivity ctx, ArrayAdapter<CodeMappingsDAO> adapter) {
			this.adapter = adapter;
			this.ctx = ctx;
		}

		@Override
		protected List<CodeMappingsDAO> doInBackground(Void... params) {
			List<CodeMappingsDAO> listing = CodeMappingsDataSource.getInstance(ctx).listCodeMappings();
			if(ctx.phone!=null){
				main: for(CodeMappingsDAO codeMappings: listing){
					for(String usedBy: codeMappings.getUsedBy()){
						if(RotinesUtilsSingleton.comparePhones(ctx.phone,
								usedBy)){
							ctx.selectedIdCodeMappins=codeMappings.getId();
							break main;
						}
					}
				}
			}
			return listing;
		}
		@Override
		protected void onPostExecute(List<CodeMappingsDAO> result) {
			super.onPostExecute(result);
			adapter.clear();
			for(CodeMappingsDAO codeMappings: result){
				adapter.add(codeMappings);
			}
			adapter.notifyDataSetChanged();
		}
	} 

	public static final int NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1=1;
	public static final int EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2=NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1+1;
	public static final String ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA="idCodeMappings";
	public static final String REQ_CODE_ACTIVITY_REQUEST_1_2_DATA="requestCode";

	public static final int PICK_CONTACT_ACTIVITY_REQUEST_3=EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2+1;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dummy_code_edit);

		editedIdsCodeMappings =  new LinkedList<Long>();

		synchronized (NewCodeMapImportedRefreshBroadcast.class) {
			target=this;
		}

		setTitle(R.string.code_map_preferences);

		phone = getIntent().getStringExtra(LoggedActivity.PHONE_PREFERENCE_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA);
		selectedIdCodeMappins = getIntent().getLongExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, (long)0);


		TextView help = new TextView(this);
		StringBuilder strBuilder= new StringBuilder("");
		if(phone!=null){
			strBuilder.append(getString(R.string.code_map_list_help_use));
			strBuilder.append("\n");
			strBuilder.append(getString(R.string.code_map_list_help_deselect));
			strBuilder.append("\n");
		}else{
			strBuilder.append(getString(R.string.code_map_list_help_edit));
			strBuilder.append("\n");
		}
		strBuilder.append(getString(R.string.code_map_list_help_options));
		help.setText(strBuilder.toString());

		this.getListView().addFooterView(help);

		List<CodeMappingsDAO> list = CodeMappingsDataSource.getInstance(this).listCodeMappings();
		setListAdapter(new ArrayAdapter<CodeMappingsDAO>(this, android.R.layout.simple_list_item_2, list){
			@Override
			public View getView(int position, View convertView, android.view.ViewGroup parent){
				TwoLineListItem row;            
				if(convertView == null){
					LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = (TwoLineListItem)inflater.inflate(android.R.layout.simple_list_item_2, null);                    
				}else{
					row = (TwoLineListItem)convertView;
				}
				CodeMappingsDAO data = super.getItem(position);
				row.getText1().setText(data.getMappingsName());
				String usedBy = String.format(getString(R.string.code_map_used_by_enumeration), data.getUsedBy().toString());
				row.getText2().setText(usedBy);
				if(data.getId()==selectedIdCodeMappins){
					row.setBackgroundColor(Color.parseColor("#708989"));
				}else{
					row.setBackgroundColor(Color.parseColor("#000000"));
				}

				return row;
			}
		});
		registerForContextMenu(this.getListView());

		Button newCodeMap = ((Button)findViewById(R.id.add_new));
		newCodeMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CodeMappingsListActivity.this,CodeMappingsEditListActivity.class);
				intent.putExtra(REQ_CODE_ACTIVITY_REQUEST_1_2_DATA, NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1);
				startActivityForResult(intent, NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1);
			}
		});
		/*new Handler().postDelayed(new Runnable() { 
	        @Override 
	        public void run() { 
	        	openOptionsMenu(); 
	        } 
		}, 500);*/ 

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ListAdapter adapter = this.getListAdapter();
		if(position<adapter.getCount()){
			CodeMappingsDAO selected = ((ArrayAdapter<CodeMappingsDAO>)adapter).getItem(position);
			if(selectedIdCodeMappins!=NOT_WAITING_FOR_RESULT){
				Intent bundle = new Intent();
				long idCodeMappings = selected.getId();
				if(selectedIdCodeMappins==idCodeMappings){
					idCodeMappings=0;
				}
				bundle.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, idCodeMappings);
				if(phone!=null){
					ThreadDataSource.getInstance(this).updateThreadCodeMappings(phone, idCodeMappings);
				}
				bundle.putExtra(ChatActivity.REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, editedIdsCodeMappings.contains(idCodeMappings));
				setResult(Activity.RESULT_OK, bundle);
				finish();
			}else{
				Intent intent = new Intent(this, CodeMappingsEditListActivity.class);
				intent.putExtra(REQ_CODE_ACTIVITY_REQUEST_1_2_DATA, EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2);
				intent.putExtra(ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, selected.getId());
				startActivityForResult(intent, EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2);
			}
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check if user deleted a code map and then pressed back key
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(selectedIdCodeMappins!=CodeMappingsListActivity.NOT_WAITING_FOR_RESULT){
				CodeMappingsDAO selected = CodeMappingsDataSource.getInstance(this).getCodeMapping(selectedIdCodeMappins);
				Intent bundle = new Intent();
				if(selected==null){
					bundle.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, (long)0); // Don't use code map
				}else{
					bundle.putExtra(ChatActivity.ID_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, selectedIdCodeMappins);
					bundle.putExtra(ChatActivity.REFRESH_CODE_MAPPINGS_PICK_SPECIFIC_PREFERENCES_ACTIVITY_REQUEST_1_DATA, editedIdsCodeMappings.contains(selectedIdCodeMappins));
				}
				setResult(Activity.RESULT_OK, bundle);
				finish();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private static final int EDIT_OPT = Menu.NONE;
	private static final int SHARE_OPT = EDIT_OPT+1;
	private static final int DELETE_OPT = SHARE_OPT+1;
	private static final int ASSOCIATE_WITH_OPT = DELETE_OPT+1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		if(info.position<this.getListAdapter().getCount()){
			{
				CodeMappingsDAO selected = (CodeMappingsDAO)this.getListView().getAdapter().getItem(info.position);
				String title = String.format(getString(R.string.code_mappings_options), selected.getMappingsName());
				menu.setHeaderTitle(title);
			}

			String[] menuItems = getResources().getStringArray(R.array.code_mappings_context);
			menu.add(Menu.NONE, EDIT_OPT, EDIT_OPT, menuItems[EDIT_OPT]);
			menu.add(Menu.NONE, SHARE_OPT, SHARE_OPT, menuItems[SHARE_OPT]);
			menu.add(Menu.NONE, DELETE_OPT, DELETE_OPT, menuItems[DELETE_OPT]);
			menu.add(Menu.NONE, ASSOCIATE_WITH_OPT, ASSOCIATE_WITH_OPT, menuItems[ASSOCIATE_WITH_OPT]);
		}
	}

	public final String PREFERENCE_SAVED_MY_PHONE_NUMBER = "myPhoneNumber";

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		/*TwoLineListItem target = (TwoLineListItem) info.targetView;
		String name= ((TextView)target.findViewById(android.R.id.text1)).getText().toString();*/

		final CodeMappingsDAO codeMappingsSelected = (CodeMappingsDAO) CodeMappingsListActivity.this.getListAdapter().getItem((int)info.id);

		switch(item.getItemId()){
		case EDIT_OPT:{
			Intent intent = new Intent(this, CodeMappingsEditListActivity.class);
			intent.putExtra(REQ_CODE_ACTIVITY_REQUEST_1_2_DATA, EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2);
			intent.putExtra(ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, codeMappingsSelected.getId());
			startActivityForResult(intent, EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2);
			return true;
		}
		case SHARE_OPT:{
			final EditText input = new EditText(this);
			input.setInputType(EditorInfo.TYPE_CLASS_PHONE);
			final SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
			input.setText(pm.getString(PREFERENCE_SAVED_MY_PHONE_NUMBER, ""));
			new AlertDialog.Builder(this)
			.setTitle(R.string.send_code_map_phone_number_title)
			.setMessage(R.string.send_code_map_phone_number)
			.setView(input)
			.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String phone = input.getText().toString();
					if(ContactUtilsSingleton.isPhoneNumber(phone)){
						Editor editor = pm.edit();
						editor.putString(PREFERENCE_SAVED_MY_PHONE_NUMBER, phone);
						editor.commit();
						sendEmailCodeMappings(codeMappingsSelected, phone);
					}else{
						Toast.makeText(CodeMappingsListActivity.this, R.string.supply_valid_phone, Toast.LENGTH_LONG).show();
					}
				}
			}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
			return true;
		}
		case DELETE_OPT:{
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(String.format(getString(R.string.code_map_alert_delete_title), codeMappingsSelected.getMappingsName()))
			.setMessage(R.string.code_map_alert_delete)
			.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
					CodeMappingsDAO selected = (CodeMappingsDAO)CodeMappingsListActivity.this.getListAdapter().getItem(info.position);
					CodeMappingsDataSource.getInstance(CodeMappingsListActivity.this).deleteCodeMappings(selected.getId());
					CodeMappingsListActivity ctx = CodeMappingsListActivity.this;
					new LoadCodeMaps(ctx, (ArrayAdapter<CodeMappingsDAO>)ctx.getListAdapter()).execute();
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();
			return true;

		}
		case ASSOCIATE_WITH_OPT:{

			editAssociatedNumber = new EditText(this);
			editAssociatedNumber.setInputType(InputType.TYPE_CLASS_PHONE);

			{
				Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.associate_with)
				.setMessage(null)
				.setView(editAssociatedNumber)
				.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
					@SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
						CodeMappingsListActivity ctx = CodeMappingsListActivity.this;
						final String phoneText = editAssociatedNumber.getText().toString();
						if(ContactUtilsSingleton.isPhoneNumber(phoneText)){
							SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
							int  jitter = sp.getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
							int checkSumLength = sp.getInt(GeneralPreferences.CHECK_SUM, GeneralPreferences.PREFERENCE_DEFAULT_CHECK_SUM_LENGTH);
							SQLiteDatabase writer = RingSMSDBHelper.getInstance(ctx).getWritableDatabase();

							writer.beginTransaction();
							ThreadDataSource dataSource = ThreadDataSource.getInstance(ctx);
							dataSource.insertThread(phoneText, jitter, checkSumLength, null);
							dataSource.updateThreadCodeMappings(phoneText, codeMappingsSelected.getId());
							writer.setTransactionSuccessful();
							writer.endTransaction();

							new LoadCodeMaps(target, (ArrayAdapter<CodeMappingsDAO>)ctx.getListAdapter()).execute();
							MsgRefreshBroadcast.sendRefreshNotice(ctx, phoneText, null);
						}
					}
				})
				.setNeutralButton(R.string.search_contact, null)
				.setNegativeButton(R.string.cancel, null);
				final AlertDialog alert = builder.create();
				alert.setOnShowListener(new OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Button b = alert.getButton(AlertDialog.BUTTON_NEUTRAL);
						b.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								Intent intent = new Intent(Intent.ACTION_PICK);
								intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
								startActivityForResult(intent, PICK_CONTACT_ACTIVITY_REQUEST_3); 
								alert.show();
							}
						});
					}
				});
				alert.show();
			}
			/*alert.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					//((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					//MyLog.d(ThreadsActivity.DEBUG_TAG, .getClass().getName());
					alert.show();
				}
			});*/

			break;
		}
		default:{break;}
		}
		return super.onContextItemSelected(item);
	}

	public void sendEmailCodeMappings(CodeMappingsDAO selected, String phoneAssociation){
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Toast.makeText(this, R.string.sd_card_not_mounted, Toast.LENGTH_LONG).show();
			return;
		}

		Intent email = new Intent(Intent.ACTION_SEND);
		email.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
		String subject = String.format(getString(R.string.code_map_send_subject), selected.getMappingsName());
		email.putExtra(Intent.EXTRA_SUBJECT, subject);

		StringBuilder message = new StringBuilder("");
		message.append(selected.getMappingsName());
		message.append("\n");
		message.append(phoneAssociation);
		message.append("\n");
		message.append(selected.getCodesMapping(this, false).toString());
		message.append("\n");
		message.append("CRC32:");

		try {
			message.append(String.valueOf(RotinesUtilsSingleton.doChecksum(message.toString())));
			String msgFinal = Base64.encode( message.toString().getBytes() );
			String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
			File file = new File(absolutePath);
			file.mkdirs();
			absolutePath = absolutePath+File.separator+"code_mappings.ringsms";
			file = new File(absolutePath);
			if(!file.exists()){
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file,false);
			email.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+absolutePath));
			Writer out = new OutputStreamWriter(fos, "UTF-8");
			// Crypto.encrypt(message.toString(), ThreadsActivity.PASSWORD_APPLICATION_ENCRYPTION) // It is better for the succes of the app we dont' obfuscate the format
			out.write(msgFinal);
			out.flush();
			out.close();
			email.putExtra(Intent.EXTRA_TEXT, "");
			/*plain/text*/
			email.setType("application/octet-stream");
			startActivity(Intent.createChooser(email, getString(R.string.choose_target)));

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.utf_8_not_supported, Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.not_possible_try_again_later, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.not_possible_try_again_later, Toast.LENGTH_LONG).show();
		}
	}











	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {	
			case (NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1):{
				long insertedId = data.getLongExtra(ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, 0);
				if(insertedId!=0){
					new LoadCodeMaps(this, (ArrayAdapter<CodeMappingsDAO>)this.getListAdapter()).execute();
				}
				break;
			}
			case (EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2):{
				long idCodeMap = data.getLongExtra(ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, 0);
				if(idCodeMap!=0){
					new LoadCodeMaps(this, (ArrayAdapter<CodeMappingsDAO>)this.getListAdapter()).execute();
					editedIdsCodeMappings.add(idCodeMap);
				}
				break;
			}
			case (PICK_CONTACT_ACTIVITY_REQUEST_3):{
				List<ContactDTO> contactList;
				try {
					contactList = ContactUtilsSingleton.getContacts(this, data.getData());

					if(contactList.size()==0){
						Toast.makeText(this,  R.string.no_phone_general, Toast.LENGTH_LONG).show();
					}else if(contactList.size()==1){
						String phone =  ContactUtilsSingleton.filterContacts(ContactAttributeEnum.PHONE,contactList).getFirst();
						Message msg = new Message();
						Bundle dataMsg = new Bundle();
						dataMsg.putString(ContactUtilsSingleton.PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER, phone);
						msg.setData(dataMsg);
						setAssociatedNumber.sendMessage(msg);
					}else{
						LinkedList<String> phones = ContactUtilsSingleton.filterContacts(ContactAttributeEnum.PHONE, contactList);
						ContactUtilsSingleton.choosePhoneFromContact(this, getString(R.string.which_phone_general), phones, null, null, setAssociatedNumber);
					}
					
				} catch (UnableToRetrieveDataException e) {
					Toast.makeText(this, R.string.not_possible_try_again_later, Toast.LENGTH_LONG).show();
				}
			}
			}
		}
	}
	private EditText editAssociatedNumber;
	private static final Handler setAssociatedNumber = new Handler(){
		public void handleMessage(android.os.Message msg) {
			synchronized (NewCodeMapImportedRefreshBroadcast.class) {
				if(target!=null && target.editAssociatedNumber!=null){
					String contactPhone = msg.getData().getString(ContactUtilsSingleton.PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER);
					target.editAssociatedNumber.setText(contactPhone);
				}
			}
		};
	};
}
