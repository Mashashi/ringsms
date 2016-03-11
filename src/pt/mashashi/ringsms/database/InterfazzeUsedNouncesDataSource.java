package pt.mashashi.ringsms.database;

import java.util.Calendar;
import java.util.Date;

import pt.mashashi.ringsms.database.RingSMSDBHelper.InterfazzeUsedNounces;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class InterfazzeUsedNouncesDataSource {
	

	private RingSMSDBHelper dbHelper;
	
	private static InterfazzeUsedNouncesDataSource instance;
	private InterfazzeUsedNouncesDataSource(Context ctx) {
		dbHelper = RingSMSDBHelper.getInstance(ctx);
	}
	public synchronized static InterfazzeUsedNouncesDataSource getInstance(Context ctx){
		if(instance==null){
			instance = new InterfazzeUsedNouncesDataSource(ctx);
		}
		return instance;
	} 
	
	public boolean existsNounce(long nounce){
		boolean hasElement = true; // Play it safe and block the new message
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		Cursor c = dbReader.query(InterfazzeUsedNounces.TABLE_NAME, 
				new String[]{InterfazzeUsedNounces.FIELD_USED_NOUNCE}, InterfazzeUsedNounces.FIELD_USED_NOUNCE+"=?",  
				new String[]{""+nounce}, null, null, null);
		if(c!=null){
			hasElement = c.moveToFirst();
		}
		if(c!=null && !c.isClosed()){
			c.close();
		}
		return hasElement; 
	}
	
	public boolean insertNounce(int nounce){
		
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(InterfazzeUsedNounces.FIELD_USED_NOUNCE, nounce);
		
		long newRowId = dbWriter.insert(InterfazzeUsedNounces.TABLE_NAME, null, values);
		return newRowId==nounce;
		
	}
	
	/**
	 * 
	 * @param expireDays The maximum number of days a nounce is registered
	 * @return
	 */
	public int deleteAllExpired(int expireDays){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		Calendar expireCal = Calendar.getInstance();
		String expirationDateFormatted =null;
		{
			expireCal.add(Calendar.DAY_OF_MONTH, -expireDays);
			Date now = expireCal.getTime();
			expirationDateFormatted = RingSMSDBHelper.SQLLITE_DATE_FORMAT.format(now);
		}
		int affectedRows = dbWriter.delete(InterfazzeUsedNounces.TABLE_NAME, 
		"strftime('%s',"+InterfazzeUsedNounces.FIELD_USED_DATE+")<strftime('%s','"+expirationDateFormatted+"')", null);
		return affectedRows;
	}
	
}
