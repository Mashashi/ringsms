package pt.mashashi.ringsms.database;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import pt.mashashi.ringsms.codemap.CodeMappingsDAO;
import pt.mashashi.ringsms.database.RingSMSDBHelper.CodeMappingTable;
import pt.mashashi.ringsms.database.RingSMSDBHelper.CodeMappingsTable;
import pt.mashashi.ringsms.database.RingSMSDBHelper.ThreadTable;
import pt.mashashi.ringsms.talk.SignalEnum;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDatabase;

public class CodeMappingsDataSource {

	private RingSMSDBHelper dbHelper;

	private static CodeMappingsDataSource instance;
	private CodeMappingsDataSource(Context ctx) {
		dbHelper = RingSMSDBHelper.getInstance(ctx);
	}
	public synchronized static CodeMappingsDataSource getInstance(Context ctx){
		if(instance==null){
			instance = new CodeMappingsDataSource(ctx);
		}
		return instance;
	} 

	/**
	 * 
	 * @param idCodeMappings
	 * @param alphabeticOrder If true orders alphabetically if false by cod
	 * @return
	 */
	public CodeMappingsDAO getCodeMapping(long idCodeMappings){
		CodeMappingsDAO result = null;
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		dbReader.beginTransaction();
		try{
			Cursor c = dbReader.query(CodeMappingsTable.TABLE_NAME, 
					new String[]{CodeMappingsTable.FIELD_MAPPINGS_NAME}, 
					CodeMappingsTable.FIELD_ID+"=?",  new String[]{""+idCodeMappings}, null, null, null);
			if(c!=null && c.moveToFirst()){
				do{
					List<String> usedBy = new LinkedList<String>();
					final String name = c.getString(c.getColumnIndexOrThrow(CodeMappingsTable.FIELD_MAPPINGS_NAME));


					Cursor m = dbReader.query(ThreadTable.TABLE_NAME, 
							new String[]{ThreadTable.FIELD_NUMBER}, 
							ThreadTable.FIELD_CODE_MAPPINGS_TABLE+"=?",  new String[]{""+idCodeMappings}, null, null, null);

					if(m!=null && m.moveToFirst()){
						do{
							usedBy.add(m.getString(m.getColumnIndexOrThrow(ThreadTable.FIELD_NUMBER)));
						}while(m.moveToNext());
						if(!m.isClosed()){
							m.close();
						}
					}

					result =  new CodeMappingsDAO(idCodeMappings, name, usedBy);
				}while(c.moveToNext());
			}else{
				// It was not possible to list the mappings
			}
		}catch(StaleDataException e){
			// Do nothing...
		}
		dbReader.endTransaction();
		return result;
	}
	/**
	 * 
	 * @param idCodeMappings
	 * @return
	 */
	public BiMap<String, String> listCodeMapping(long idCodeMappings){
		BiMap<String, String> result = HashBiMap.create();
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		try{
			Cursor c = dbReader.query(CodeMappingTable.TABLE_NAME, 
					new String[]{CodeMappingTable.FIELD_CODE, CodeMappingTable.FIELD_MAPPING_STRING}, 
					CodeMappingTable.FIELD_CODE_MAPPINGS+"=?",  new String[]{""+idCodeMappings}, null, null, CodeMappingTable.FIELD_MAPPING_STRING+" ASC");
			if(c!=null && c.moveToFirst()){
				do{
					final String code = c.getString(c.getColumnIndexOrThrow(CodeMappingTable.FIELD_CODE));
					final String codeMapping = c.getString(c.getColumnIndexOrThrow(CodeMappingTable.FIELD_MAPPING_STRING));
					result.put(code,codeMapping);
				}while(c.moveToNext());
	
			}else{
				// It was not possible to list the mappings
			}
			if(c!=null && !c.isClosed()){
				c.close();
			}
		}catch(StaleDataException e){
			// Do nothing...
		}
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public List<CodeMappingsDAO> listCodeMappings(){
		LinkedList<CodeMappingsDAO> result = new LinkedList<CodeMappingsDAO>();
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();

		dbReader.beginTransaction();
		try{
			Cursor c = dbReader.query(CodeMappingsTable.TABLE_NAME, 
					new String[]{CodeMappingsTable.FIELD_ID, CodeMappingsTable.FIELD_MAPPINGS_NAME}, null,  null, null, null, CodeMappingsTable.FIELD_MAPPINGS_NAME+" ASC");
			if(c!=null && c.moveToFirst()){
				do{
					final int id = c.getInt(c.getColumnIndexOrThrow(CodeMappingsTable.FIELD_ID));
					final String mappingsName = c.getString(c.getColumnIndexOrThrow(CodeMappingsTable.FIELD_MAPPINGS_NAME));
					List<String> usedBy = usedByPhones(dbReader, id);
					result.add(new CodeMappingsDAO(id, mappingsName, usedBy));
				}while(c.moveToNext());
			}else{
				// It was not possible to list the mappings
			}
			if(c!=null && !c.isClosed()){
				c.close();
			}
		}catch(StaleDataException e){
			// Do nothing...
		}
		dbReader.setTransactionSuccessful();
		dbReader.endTransaction();

		return result;
	}

	/**
	 * 
	 * @param idCodeMappings
	 * @return
	 */
	public List<String> usedByPhones(SQLiteDatabase dbReader, int idCodeMappings){
		LinkedList<String> result = new LinkedList<String>();
		try{
			Cursor c = dbReader.query(ThreadTable.TABLE_NAME, 
					new String[]{	ThreadTable.FIELD_NUMBER}, 
					ThreadTable.FIELD_CODE_MAPPINGS_TABLE+"=?", 
					new String[]{""+idCodeMappings}, null, null, null);
			if(c!=null && c.moveToFirst()){
				do{
					String phone = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_NUMBER));
					result.add(phone);
				}while(c.moveToNext());
			}else{
				// It was not possible to acquire which contact numbers use this code mappings
			}
			if(c!=null && !c.isClosed()){
				c.close();
			}
		}catch(StaleDataException e){
			// Do nothing...
		}
		return result;
	}

	/**
	 * 
	 * @param mappingsName
	 * @param codesMapping
	 * @return
	 */
	public long insertCodeMappings(String mappingsName, Map<String, String> codesMapping){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(CodeMappingsTable.FIELD_MAPPINGS_NAME, mappingsName);

		dbWriter.beginTransaction();
		
		long newRowId = dbWriter.insert(CodeMappingsTable.TABLE_NAME, null, values);
		if(newRowId!=-1){
			insertCodesMapping(dbWriter, newRowId, codesMapping);
		}

		dbWriter.setTransactionSuccessful();
		dbWriter.endTransaction();

		return newRowId;
	}

	public long updateCodeMappings(long idCodeMappings, String mappingsName, Map<String, String> codeMappings){

		SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CodeMappingsTable.FIELD_MAPPINGS_NAME, mappingsName);

		// Which row to update, based on the ID
		String select = CodeMappingsTable.FIELD_ID + " LIKE ?";
		String[] selectionArgs = { String.valueOf(idCodeMappings) };

		dbWrite.beginTransaction();

		int affectedRows = dbWrite.update(
				CodeMappingsTable.TABLE_NAME,
				values,
				select,
				selectionArgs);

		dbWrite.delete(
				CodeMappingTable.TABLE_NAME, 
				CodeMappingTable.FIELD_CODE_MAPPINGS+" LIKE ?", 
						new String[]{String.valueOf(idCodeMappings)});
		insertCodesMapping(dbWrite, idCodeMappings, codeMappings);

		dbWrite.setTransactionSuccessful();
		dbWrite.endTransaction();

		return affectedRows;
	}

	public static final String PATTERN_CODE = "^(0|1){"+SignalEnum.CODE_SIZE+"}$";
	private int insertCodesMapping(SQLiteDatabase dbWrite, long idCodeMappings, Map<String, String> codeMappings){
		int result = 0;
		ContentValues values = new ContentValues();
		Set<Entry<String,String>> entries= codeMappings.entrySet();
		for(Entry<String,String> entry: entries){
			if(!entry.getKey().matches(PATTERN_CODE))
				throw new IllegalArgumentException("Code format is invalid");
			values.clear();
			values.put(CodeMappingTable.FIELD_CODE, entry.getKey());
			values.put(CodeMappingTable.FIELD_CODE_MAPPINGS, idCodeMappings);
			values.put(CodeMappingTable.FIELD_MAPPING_STRING, entry.getValue());
			if(dbWrite.insert(CodeMappingTable.TABLE_NAME, null, values)!=-1){
				result++;
			}
		}
		return result;
	}

	public int deleteCodeMappings(long idCodeMappings){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		int affectedRows = dbWriter.delete(
				CodeMappingsTable.TABLE_NAME, 
				CodeMappingsTable.FIELD_ID+" LIKE ?", 
				new String[]{""+idCodeMappings});
		return affectedRows;
	}

	public long insertCodeMappings(String mappingsName){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CodeMappingsTable.FIELD_MAPPINGS_NAME, mappingsName);
		long newRowId = dbWriter.insert(RingSMSDBHelper.ThreadTable.TABLE_NAME, null, values);
		return newRowId;
	}
}
