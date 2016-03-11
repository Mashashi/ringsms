package pt.mashashi.ringsms.codemap;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.http.ParseException;

import com.google.android.vending.licensing.util.Base64;
import com.google.android.vending.licensing.util.Base64DecoderException;
import com.google.common.collect.Ordering;

import pt.mashashi.ringsms.BroadcastEvents;
import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;
import pt.mashashi.ringsms.database.ThreadDataSource;
import pt.mashashi.ringsms.talk.MsgRefreshBroadcast;
import pt.mashashi.ringsms.talk.SignalEnum;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import pt.mashashi.ringsms.threads.ThreadDAO;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CodeMappingsEditListActivity extends ListActivity{

	private int requestCode;
	private long idCodeMappings;
	private CodeMappingsDAO codeMappingsObj;
	private EditText editTextMappingsName;

	private String phoneImported;

	public static final int NEW_CODE_MAP_VIA_IMPORT = 0;
	private CheckBox importPhone;

	public final static String CODE_MAP_ID_NEW_IMPORTED_CODE_MAP = "codeMapId";
	public final static String ASSOCIATED_PHONE_ID_NEW_IMPORTED_CODE_MAP = "associatedPhone";

	private ArrayAdapter<Entry<String, String>> getNewListAdapter(Context ctx, List<Entry<String,String>> mappings){
		return new ArrayAdapter<Entry<String, String>>(ctx, android.R.layout.simple_list_item_1, mappings){
			@Override
			public View getView(int position, View convertView, android.view.ViewGroup parent){
				TextView row;            
				if(convertView == null){
					LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);                    
				}else{
					row = (TextView) convertView;
				}
				Entry<String, String> data = super.getItem(position);
				row.setText(data.getValue());

				return row;
			}
			@Override
			public void add(Entry<String, String> object) {
				int indexInsert = -1;
				cycle: for(int i=0;i<this.getCount();i++){
					String item = this.getItem(i).getValue();
					int result = item.compareTo(object.getValue());
					if(result>0){
						indexInsert = i;
						insert(object, indexInsert);
						break cycle;
					}
				}
				if(indexInsert==-1){
					super.add(object);
				}
			}
		};
	}

	private TextView codeAssociationsTextView;
	private static CodeMappingsEditListActivity target;
	public static class NewCodeMapImportedRefreshBroadcast extends BroadcastReceiver{
		public NewCodeMapImportedRefreshBroadcast(){}
		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (NewCodeMapImportedRefreshBroadcast.class) {
				if(target!=null){
					TextView usedTextView = target.codeAssociationsTextView;
					if(target!=null && usedTextView!=null){
						target.setUsedBy(target.idCodeMappings, usedTextView);
					}
				}
			}
		}
	}


	public void mySetTitle(CharSequence title) {
		TextView titleView = ((TextView)getWindow().findViewById(R.id.code_map_edit_title));
		titleView.setText(title);
	}
	public void mySetTitle(int strResource) {
		mySetTitle(getString(strResource));
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title_edit_code_map);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.dummy_code_edit);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title_edit_code_map);
		mySetTitle(R.string.new_code_map);

		View header = getLayoutInflater().inflate(R.layout.header_new_code_map, null);
		getListView().addHeaderView(header,null,false);
		editTextMappingsName = (EditText)header.findViewById(R.id.mappings_name_in);

		requestCode=getIntent().getIntExtra(CodeMappingsListActivity.REQ_CODE_ACTIVITY_REQUEST_1_2_DATA,-1);
		idCodeMappings=getIntent().getLongExtra(CodeMappingsListActivity.ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, 0);

		List<Entry<String, String>> mappings = new LinkedList<Entry<String, String>>();

		if(idCodeMappings!=0){
			
			codeMappingsObj = CodeMappingsDataSource.getInstance(this).getCodeMapping(idCodeMappings);
			editTextMappingsName.setText(codeMappingsObj.getMappingsName());
			mappings =  new LinkedList<Entry<String, String>>(codeMappingsObj.getCodesMapping(this, false).entrySet());
			
			if(requestCode==CodeMappingsListActivity.EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2){
				mySetTitle(String.format(getString(R.string.code_mappings_editing_title),codeMappingsObj.getMappingsName()));
			}
			
		}
		Ordering<Entry<String, String>> ordering= new Ordering<Entry<String, String>>() {
			public int compare(Entry<String, String> left, Entry<String, String> right) {
				return left.getValue().compareTo(right.getValue());
			}
		};
		setListAdapter(getNewListAdapter(this, ordering.sortedCopy(mappings)));

		Button button = (Button) findViewById(R.id.submit_code_map);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				@SuppressWarnings("unchecked")
				ArrayAdapter<Entry<String,String>> arrayAdaper=(ArrayAdapter<Entry<String, String>>) CodeMappingsEditListActivity.this.getListAdapter();
				if(arrayAdaper.getCount()==0){
					Toast.makeText(CodeMappingsEditListActivity.this, R.string.empty_code_list, Toast.LENGTH_LONG).show();
					return;
				}
				
				Intent intent = new Intent();
				CodeMappingsDataSource dataSource = CodeMappingsDataSource.getInstance(CodeMappingsEditListActivity.this);
				String mappingsName =editTextMappingsName.getText().toString();
				switch(requestCode){
				case CodeMappingsListActivity.NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1:{
					idCodeMappings =dataSource.insertCodeMappings(mappingsName, buildMapFromAdapter(arrayAdaper));
					
					break;
				}
				case CodeMappingsListActivity.EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2:{
					HashMap<String, String> newCodeList = buildMapFromAdapter(arrayAdaper);
					//newCodeList.put("11111111", "Como vai isso?"); // TODO Testing delete this
					dataSource.updateCodeMappings(idCodeMappings, mappingsName, newCodeList);
					break;
				}
				case NEW_CODE_MAP_VIA_IMPORT:{
					idCodeMappings =dataSource.insertCodeMappings(mappingsName, buildMapFromAdapter(arrayAdaper));
					Intent intentBroadCast = new Intent(BroadcastEvents.NEW_IMPORTED_CODE_MAP);
					String wasAssociated = phoneImported;
					if(importPhone.isChecked()){
						int affectedRows = ThreadDataSource.getInstance(CodeMappingsEditListActivity.this).updateThreadCodeMappings(phoneImported, idCodeMappings);
						if(affectedRows==0){
							Context ctx = CodeMappingsEditListActivity.this;
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
							final int jitter = prefs.getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_JITTER);
							final int checkSumLength = prefs.getInt(GeneralPreferences.JITTER, GeneralPreferences.PREFERENCE_DEFAULT_CHECK_SUM_LENGTH);
							ThreadDataSource.getInstance(ctx).insertThread(phoneImported, jitter, checkSumLength, idCodeMappings);
							MsgRefreshBroadcast.sendRefreshNotice(ctx, phoneImported, null);
						}
					}else{
						wasAssociated = null;
					}
					intentBroadCast.putExtra(ASSOCIATED_PHONE_ID_NEW_IMPORTED_CODE_MAP, wasAssociated);
					intentBroadCast.putExtra(CODE_MAP_ID_NEW_IMPORTED_CODE_MAP, idCodeMappings);
					sendBroadcast(intentBroadCast);
					Toast.makeText(CodeMappingsEditListActivity.this, R.string.code_imported_success, Toast.LENGTH_LONG).show();
					break;
				}
				}
				if(requestCode==CodeMappingsListActivity.NEW_CODE_MAPPINGS_ACTIVITY_REQUEST_1||requestCode==CodeMappingsListActivity.EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2){
					intent.putExtra(CodeMappingsListActivity.ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, idCodeMappings);
					setResult(Activity.RESULT_OK, intent);
					Toast.makeText(CodeMappingsEditListActivity.this, R.string.code_you_may_want_share, Toast.LENGTH_LONG).show();
				}
				finish();
			}
		});

		{
			boolean imported = importCodeMappings(header, getIntent());
			if(!imported){
				synchronized (NewCodeMapImportedRefreshBroadcast.class) {
					target=this;
				}
			}
		}
		codeAssociationsTextView  = (TextView)header.findViewById(R.id.code_associations);
		if(requestCode==CodeMappingsListActivity.EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2){
			setUsedBy(idCodeMappings, codeAssociationsTextView);
		}else{
			((LinearLayout)header).removeView(codeAssociationsTextView);
			codeAssociationsTextView=null;
		}

		Button newCodeMap = ((Button)findViewById(R.id.add_new));
		newCodeMap.setText(R.string.new_code_string);
		newCodeMap.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				final EditText input = new EditText(CodeMappingsEditListActivity.this);
				
				input.setSingleLine();
				input.setSingleLine(true);
				input.setMaxLines(1);
				
				new AlertDialog.Builder(CodeMappingsEditListActivity.this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.new_code_string)
				.setMessage(null)
				.setView(input)
				.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						final String inText = input.getText().toString();
						@SuppressWarnings("unchecked")
						ArrayAdapter<Entry<String, String>> listAdapter = ((ArrayAdapter<Entry<String, String>>)CodeMappingsEditListActivity.this.getListAdapter());
						
						if(inText.length()==0){
							Toast.makeText(CodeMappingsEditListActivity.this, R.string.empty_string_not_allowed, Toast.LENGTH_LONG).show();
							return;
						}
						
						for(int i =0;i<listAdapter.getCount();i++){
							if(inText.equals(listAdapter.getItem(i).getValue())){
								Toast.makeText(CodeMappingsEditListActivity.this, R.string.there_is_already_a_code, Toast.LENGTH_LONG).show();
								return;
							}
						}
						try {
							int availableCodeVal = 0;
							String availableCode;
							availableCode = getUnifomizeCodeWord(availableCodeVal);

							HashMap<String, String> newCodeList = buildMapFromAdapter(listAdapter);
							while(newCodeList.containsKey(availableCode)){
								availableCodeVal++;
								availableCode = getUnifomizeCodeWord(availableCodeVal);
							}
							final String finalAvailableCode = availableCode;
							listAdapter.add(new Entry<String,String>(){
								private String key = finalAvailableCode;
								private String value = inText;
								@Override public String getKey() {return key; }
								@Override public String getValue() {return value;}
								@Override public String setValue(String object) { String before = value; value = object; return before; }
							});
							listAdapter.notifyDataSetChanged();
						} catch (IntegerInvalidException e) {
							Toast.makeText(CodeMappingsEditListActivity.this, R.string.full_code_list, Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
			}
		});

		/*new Handler().postDelayed(new Runnable() { 
	        @Override 
	        public void run() { 
	        	openOptionsMenu(); 
	        } 
		}, 500);*/ 

	}

	private void setUsedBy(long idCodeMappings, TextView codeAssociationsTextView){
		CodeMappingsDAO codeMappingsDAO = CodeMappingsDataSource.getInstance(this).getCodeMapping(idCodeMappings);
		String usedBy=getString(R.string.code_map_isnt_associated_to_any_number);
		if(codeMappingsDAO!=null && codeMappingsDAO.getUsedBy().size()!=0){
			usedBy = String.format(getString(R.string.code_map_used_by_enumeration), codeMappingsDAO.getUsedBy().toString());	
		}
		codeAssociationsTextView.setText(usedBy);
	}

	private static HashMap<String, String> buildMapFromAdapter(ArrayAdapter<Entry<String, String>> listAdapter){
		HashMap<String, String> newCodeList = new HashMap<String, String>();
		for(int i=0; i<listAdapter.getCount();i++){
			Entry<String, String> entry = listAdapter.getItem(i);
			newCodeList.put(entry.getKey(), entry.getValue());
		}
		return newCodeList;
	}

	@SuppressWarnings("serial")
	private static class IntegerInvalidException extends Exception{}
	private static String getUnifomizeCodeWord(int codeWord) throws IntegerInvalidException{
		if(codeWord<0||codeWord>256)
			throw new IntegerInvalidException();
		StringBuilder codeWordStr = new StringBuilder(Integer.toBinaryString(codeWord));
		while(codeWordStr.length()!=SignalEnum.CODE_SIZE){
			codeWordStr.insert(0, "0");
		}
		return codeWordStr.toString();
	}

	protected boolean importCodeMappings(View header, Intent intent) {
		super.onNewIntent(intent);
		/*
		Mom
		912129003
		{00000011=Não sei, 00000001=Faz-me o jantar, 00000000=Faz-me o almoço}
		CRC32:3054592125*/
		String action = intent.getAction();
		if(action!=null && action.equals("android.intent.action.VIEW")){
			try {

				MyLog.d(ThreadsActivity.DEBUG_TAG, getIntent().getData().toString());
				InputStream inputStreamContent = this.getContentResolver().openInputStream(getIntent().getData());
				Scanner scannerContent = new Scanner(inputStreamContent);
				StringBuilder builder = new StringBuilder("");
				while(scannerContent.hasNextLine()){
					builder.append(scannerContent.nextLine());
					if(scannerContent.hasNextLine()){ builder.append("\n"); }
				}

				String file = new String(Base64.decode(builder.toString()), "UTF-8");
				MyLog.d(ThreadsActivity.DEBUG_TAG, "File:"+file);
				if(!file.matches("^.*\n.*\n.*\nCRC32\\:[0-9]*$")){
					throw new ParseException(getString(R.string.corrupted_file));
				}
				Scanner parser = new Scanner(file);

				String codeMappingsName = null;
				phoneImported = null;
				String codeMappingsStr = null;
				HashMap<String, String> codeMappings = null;
				String crc32Str = null;

				codeMappingsName = parser.nextLine();

				phoneImported = parser.nextLine();
				if(!ContactUtilsSingleton.isPhoneNumber(phoneImported)){
					throw new ParseException(getString(R.string.supply_valid_phone));
				}

				codeMappingsStr = parser.nextLine();
				codeMappings = parseHashMap(this, codeMappingsStr);
				
				
				{
					String checkSumTarget = codeMappingsName+"\n"+phoneImported+"\n"+codeMappingsStr+"\nCRC32:";
					// We have to exclude the BOM order mark character and other invisible characters
					crc32Str = parser.nextLine().substring(6).replaceAll("[^0-9]", "");
					if(crc32Str.length()!=0 && Long.parseLong(crc32Str)!=RotinesUtilsSingleton.doChecksum(checkSumTarget)){
						throw new ParseException(getString(R.string.corrupted_file));
					}
				}

				// Parsed
				editTextMappingsName.setText(codeMappingsName);
				setListAdapter(getNewListAdapter(this, new LinkedList<Entry<String, String>>(codeMappings.entrySet())));

				importPhone = new CheckBox(this);
				importPhone.setText(String.format(getString(R.string.code_association), phoneImported));
				importPhone.setChecked(true);
				((LinearLayout)header).addView(importPhone);

				ThreadDAO thread = ThreadDataSource.getInstance(this).getThread(phoneImported);

				if(thread!=null && thread.getCodeMappingsId()!=0){

					final TextView warningOverriding = new TextView(this);
					warningOverriding.setText(R.string.code_overriding);
					warningOverriding.setTextColor(Color.RED);
					((LinearLayout)header).addView(warningOverriding);
					importPhone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							warningOverriding.setVisibility(isChecked?View.VISIBLE:View.INVISIBLE);
						}
					});
				}

				requestCode = NEW_CODE_MAP_VIA_IMPORT;

			} catch (FileNotFoundException e) {
				Toast.makeText(this, R.string.not_possible_try_again_later, Toast.LENGTH_LONG).show();
			} catch (UnsupportedEncodingException e) {
				Toast.makeText(this, R.string.utf_8_not_supported, Toast.LENGTH_LONG).show();
			} catch (Base64DecoderException e) {
				Toast.makeText(this, R.string.corrupted_file, Toast.LENGTH_LONG).show();
			} catch (ParseException e){
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (Exception e){
				Toast.makeText(this, R.string.corrupted_file, Toast.LENGTH_LONG).show();
			}

			return true;
		}
		return false;
	}

	public HashMap<String, String> parseHashMap(Context ctx, String foo) {
		HashMap<String, String> mapResult = new HashMap<String, String>();
		String foo2 = foo.substring(1, foo.length() - 1);  // hack off braces
		StringTokenizer st = new StringTokenizer(foo2, ",");
		while (st.hasMoreTokens()) {
			String thisToken = st.nextToken();
			StringTokenizer st2 = new StringTokenizer(thisToken, "=");
			String code = st2.nextToken().trim();
			if(!code.matches(CodeMappingsDataSource.PATTERN_CODE))
				throw new ParseException(ctx.getString(R.string.corrupted_file));
			String str = st2.nextToken();
			if(mapResult.values().contains(str)||str.length()==0)
				throw new ParseException(ctx.getString(R.string.corrupted_file));
			mapResult.put(code, str);
		}
		return mapResult;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final ArrayAdapter<Entry<String, String>> listAdapter = (ArrayAdapter<Entry<String, String>>)getListAdapter();
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setSingleLine(true);
		input.setMaxLines(1);
		
		final Entry<String, String> item = (listAdapter.getItem(position-1/*The header counts to*/));

		input.setText(item.getValue());
		new AlertDialog.Builder(this)
		.setTitle(R.string.editing_code_string)
		.setMessage(null)
		.setView(input)
		.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				item.setValue(input.getText().toString());
				listAdapter.notifyDataSetChanged();
			}
		})

		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		})
		.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listAdapter.remove(item);
				listAdapter.notifyDataSetChanged();
			}
		})
		.show();
	} 

	private final static int DELETE_OPT = Menu.NONE;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		String[] menuLabels = this.getResources().getStringArray(R.array.edit_code_mappings_menu);
		if(requestCode == CodeMappingsListActivity.EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_2){
			menu.add(Menu.NONE, DELETE_OPT, DELETE_OPT, menuLabels[DELETE_OPT]).setIcon(android.R.drawable.ic_menu_delete);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case DELETE_OPT:{
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(String.format(getString(R.string.code_map_alert_delete_title), codeMappingsObj.getMappingsName()))
			.setMessage(R.string.code_map_alert_delete)
			.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					CodeMappingsDataSource.getInstance(CodeMappingsEditListActivity.this).deleteCodeMappings(idCodeMappings);
					Intent intent = new Intent();
					intent.putExtra(CodeMappingsListActivity.ID_CODE_EDIT_CODE_MAPPINGS_ACTIVITY_REQUEST_1_2_DATA, idCodeMappings);
					setResult(Activity.RESULT_OK, intent);
					finish();
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();
			break;
		}
		}
		return super.onOptionsItemSelected(item);

	}
}
