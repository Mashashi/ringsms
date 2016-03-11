package pt.mashashi.ringsms.database;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Ordering;

import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ContactDTO;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.database.RingSMSDBHelper.MessageTable;
import pt.mashashi.ringsms.database.RingSMSDBHelper.ThreadTable;
import pt.mashashi.ringsms.threads.ThreadDAO;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

public class ThreadDataSource {

	private RingSMSDBHelper dbHelper;
	private Context ctx;

	private static ThreadDataSource instance;
	private ThreadDataSource(Context ctx) {
		dbHelper = RingSMSDBHelper.getInstance(ctx);
		this.ctx = ctx; 
	}
	public synchronized static ThreadDataSource getInstance(Context ctx){
		if(instance==null){
			instance = new ThreadDataSource(ctx);
		}
		return instance;
	} 

	/**
	 * 
	 * @return
	 */
	public List<ThreadDAO> listThreads() {
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		List<ThreadDAO> result = new LinkedList<ThreadDAO>();;

		dbReader.beginTransaction();
		Cursor c = dbReader.rawQuery(RingSMSDBHelper.ThreadTable.SQL_QUERY_LIST_THREADS, null);
		Cursor b = dbReader.rawQuery(RingSMSDBHelper.ThreadTable.SQL_QUERY_LIST_THREADS_SINGLE, null);
		dbReader.endTransaction();

		boolean singleRead = false;


		if(c!=null && b!=null){
			try{
				boolean goOn = c.moveToFirst();
				if(!goOn){
					singleRead=true;
					if(!c.isClosed()){ c.close(); }
					goOn=b.moveToFirst();
					if(goOn){ c = b; }
				}
				if(goOn){

					do{
						String phoneNumber = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_NUMBER));
						String dateStr = c.getString((c.getColumnIndexOrThrow("thread_last_msg_timestamp")));
						Date lastMsgTimeStamp = null;
						try {
							if(dateStr!=null)
								lastMsgTimeStamp = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(dateStr);
						} catch (ParseException e) {
							// This is not supposed to hapen
							e.printStackTrace();
						}
						int totalMsgs = c.getInt(c.getColumnIndexOrThrow("thread_messages_count"));
						int jitter = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_JITTER));

						int checkSum = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_CHECK_SUM_LENGTH));

						String lastMsg = c.getString(c.getColumnIndexOrThrow("lastMessage"));

						String name = null;
						Bitmap photo = null;
						try {
							ContactDTO contact = ContactUtilsSingleton.getContactFromPhone(ctx, phoneNumber);
							if(contact!=null){
								photo = ContactUtilsSingleton.getContactPhoto(ctx.getContentResolver(), contact.getUri());
								name = contact.getName();
							} else {
								name = phoneNumber;
							}
						} catch (UnableToRetrieveDataException e) {
							// Do not present photo neither the user name
						}
						Date lastAccessTimeStamp = null;
						{
							String lastAccess = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_LAST_ACCESS));
							if(lastAccess!=null){
								try {
									lastAccessTimeStamp = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(lastAccess);
								} catch (ParseException e) {
									// This is not supposed to happen
									e.printStackTrace();
								}
							}
						}

						String scratch = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_SCRATCH));

						int codeMappingsTable = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_CODE_MAPPINGS_TABLE));

						result.add(new ThreadDAO(
								name, phoneNumber, photo, jitter, 
								checkSum, lastAccessTimeStamp, lastMsg, 
								lastMsgTimeStamp, totalMsgs, scratch, codeMappingsTable));
						if(c.isLast() && !singleRead){

							singleRead=true;
							if(!c.isClosed()){ c.close(); }
							goOn=b.moveToFirst();
							if(goOn){ c = b; }

						}else {
							goOn=c.moveToNext();
						}

					}while(goOn);
				}
				if(!c.isClosed()){
					c.close();
				}
			}catch(StaleDataException e){
				// Do nothing...
			}
		}else{
			// Show empty message list
		}
		Ordering<ThreadDAO> ordering= new Ordering<ThreadDAO>() {
			public int compare(ThreadDAO left, ThreadDAO right) {
				return left.getName().compareTo(right.getName());
			}
		};

		//dbReader.close();
		return ordering.sortedCopy(result);
	}


	/**
	 * 
	 * @param phone
	 * @return
	 */
	public ThreadDAO getThread(String phone){
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		ThreadDAO result = null;

		dbReader.beginTransaction();
		Cursor c = dbReader.rawQuery(RingSMSDBHelper.ThreadTable.SQL_QUERY_LIST_THREADS+" HAVING t."+ThreadTable.FIELD_NUMBER+"=?", new String[]{phone});
		Cursor b = dbReader.rawQuery(RingSMSDBHelper.ThreadTable.SQL_QUERY_LIST_THREADS_SINGLE+" AND t."+ThreadTable.FIELD_NUMBER+"=?", new String[]{phone});
		dbReader.endTransaction();

		if(c!=null && b!=null){
			try{
				if(!c.moveToFirst()){
					if(!c.isClosed())
						c.close();
					c=b;
				}else{
					if(!b.isClosed())
						b.close();
				}

				if(c.moveToFirst()){

					String phoneNumber = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_NUMBER));
					String dateStr = c.getString((c.getColumnIndexOrThrow("thread_last_msg_timestamp")));
					Date lastMsgTimeStamp = null;
					try {
						if(dateStr!=null)
							lastMsgTimeStamp = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(dateStr);
					} catch (ParseException e) {
						// This is not supposed to hapen
						e.printStackTrace();
					}
					int totalMsgs = c.getInt(c.getColumnIndexOrThrow("thread_messages_count"));
					int jitter = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_JITTER));

					int checkSum = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_CHECK_SUM_LENGTH));

					String lastMsg = c.getString(c.getColumnIndexOrThrow("lastMessage"));

					String name = null;
					Bitmap photo = null;
					try {
						ContactDTO contact = ContactUtilsSingleton.getContactFromPhone(ctx, phoneNumber);
						if(contact!=null){
							photo = ContactUtilsSingleton.getContactPhoto(ctx.getContentResolver(), contact.getUri());
							name = contact.getName();
						} else {
							name = phoneNumber;
						}
					} catch (UnableToRetrieveDataException e) {
						// Do not present photo neither the user name
					}
					Date lastAccessTimeStamp = null;
					{
						String lastAccess = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_LAST_ACCESS));
						if(lastAccess!=null){
							try {
								lastAccessTimeStamp = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(lastAccess);
							} catch (ParseException e) {
								// This is not supposed to happen
								e.printStackTrace();
							}
						}
					}

					String scratch = c.getString(c.getColumnIndexOrThrow(ThreadTable.FIELD_SCRATCH));

					int codeMappingsTable = c.getInt(c.getColumnIndexOrThrow(ThreadTable.FIELD_CODE_MAPPINGS_TABLE));

					result = new ThreadDAO(
							name, phoneNumber, photo, jitter, 
							checkSum, lastAccessTimeStamp, lastMsg, 
							lastMsgTimeStamp, totalMsgs, scratch, codeMappingsTable);
				}
				if(!c.isClosed()){
					c.close();
				}
			}catch(StaleDataException e){
				// Do nothing...
			}
		}else{
			// Show empty message list
		}

		//dbReader.close();
		return result;
	}

	/**
	 * Don't worry if the entry already exist it will yield an axception in the console but the code will flow as normal.
	 * 
	 * @param phoneNumber
	 * @param jitter
	 * @param checkSumLength
	 * @return
	 */
	public long insertThread(String phoneNumber, int jitter, int checkSumLength, Long codeMap) {
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ThreadTable.FIELD_NUMBER, phoneNumber);
		values.put(ThreadTable.FIELD_JITTER, jitter);
		values.put(ThreadTable.FIELD_CHECK_SUM_LENGTH, checkSumLength);
		values.put(ThreadTable.FIELD_LAST_ACCESS, (String)null);
		values.put(ThreadTable.FIELD_SCRATCH, "");
		if(codeMap!=null && codeMap==0){codeMap=null;}
		values.put(ThreadTable.FIELD_CODE_MAPPINGS_TABLE, codeMap);
		long newRowId = dbWriter.insert(RingSMSDBHelper.ThreadTable.TABLE_NAME, null, values);

		//dbWriter.close();
		return newRowId;
	}

	/**
	 * 
	 * @param phoneNumber
	 * @return
	 */
	public int deleteThread(String phoneNumber) {
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		int affectedRows = dbWriter.delete(
				ThreadTable.TABLE_NAME, 
				ThreadTable.FIELD_NUMBER+" LIKE ?", 
				new String[]{phoneNumber});

		//dbWriter.close();
		return affectedRows;
	}

	/**
	 * 
	 */
	public void deleteAllThreads() {
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		dbWriter.execSQL(ThreadTable.SQL_DELETE_ALL_THREADS);
		dbWriter.execSQL(MessageTable.SQL_RESTART_NUMBERING);

		//dbWriter.close();
	}

	/**
	 * 
	 * @param phoneNumber
	 * @param jitter
	 * @return
	 */
	public int updateThreadJitter(String phoneNumber, int jitter) {
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		ContentValues values = new ContentValues();
		values.put(ThreadTable.FIELD_JITTER, jitter);

		// Which row to update, based on the ID
		String select = ThreadTable.FIELD_NUMBER + " LIKE ?";
		String[] selectionArgs = { String.valueOf(phoneNumber) };

		int affectedRows = dbReader.update(
				ThreadTable.TABLE_NAME,
				values,
				select,
				selectionArgs);

		//dbReader.close();
		return affectedRows;
	}

	/**
	 * 
	 * @param phoneNumber
	 * @param checkSum
	 * @return
	 */
	public int updateThreadCheckSumLength(String phoneNumber, int checkSum) {
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		ContentValues values = new ContentValues();
		values.put(ThreadTable.FIELD_CHECK_SUM_LENGTH, checkSum);

		String select = ThreadTable.FIELD_NUMBER + " LIKE ?";
		String[] selectionArgs = { String.valueOf(phoneNumber) };

		int affectedRows = dbReader.update(
				ThreadTable.TABLE_NAME,
				values,
				select,
				selectionArgs);

		//dbReader.close();
		return affectedRows;
	}

	/**
	 * 
	 * @param phoneNumber
	 * @param scratch
	 * @return
	 */
	public int updateThreadScratch(String phoneNumber, String scratch){
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		ContentValues values = new ContentValues();
		values.put(ThreadTable.FIELD_SCRATCH, scratch);

		String select = ThreadTable.FIELD_NUMBER + " LIKE ?";
		String[] selectionArgs = { String.valueOf(phoneNumber) };

		int affectedRows = dbReader.update(
				ThreadTable.TABLE_NAME,
				values,
				select,
				selectionArgs);

		return affectedRows;
	}

	/**
	 * 
	 * @param phoneNumber
	 * @param idCodeMappings
	 * @return The new value
	 */
	public int updateThreadCodeMappings(String phoneNumber, long idCodeMappings){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ThreadTable.FIELD_CODE_MAPPINGS_TABLE, idCodeMappings>0 ? String.valueOf(idCodeMappings) : (String)null );
		String select = ThreadTable.FIELD_NUMBER + " LIKE ?";
		String[] selectionArgs = { String.valueOf(phoneNumber) };
		int affectedRows = dbWriter.update(ThreadTable.TABLE_NAME,values,select,selectionArgs);
		return affectedRows;
	}

	/**
	 * Updates the last access to the current timestamp
	 * @param phoneNumber
	 */
	public int updateLastAccess(String phoneNumber){
		SQLiteDatabase dbAccesser = dbHelper.getWritableDatabase();
		//SQLiteDatabase dbReader = ctx.openOrCreateDatabase(RingSMS_DBHelper.DATABASE_NAME, Context.MODE_WORLD_WRITEABLE, null);

		ContentValues values = new ContentValues();
		Date now = Calendar.getInstance().getTime();
		String strNow = RingSMSDBHelper.SQLLITE_DATE_FORMAT.format(now);
		values.put(ThreadTable.FIELD_LAST_ACCESS, strNow);

		int affectedRows = 0;
		Date timeStampMsg = null;
		Date timeStampThread = null;

		dbAccesser.beginTransaction();
		Cursor beforeThreadLastAccessQuery = null;
		Cursor maxTimeStampMsgQuery = null;
		try{

			{
				beforeThreadLastAccessQuery = 
						dbAccesser.query(ThreadTable.TABLE_NAME, new String[]{ThreadTable.FIELD_LAST_ACCESS}, 
								ThreadTable.FIELD_NUMBER+"=?", 
								new String[]{phoneNumber}, null, null, null);

				if(beforeThreadLastAccessQuery!=null && beforeThreadLastAccessQuery.moveToFirst()){
					String strDate = beforeThreadLastAccessQuery.getString(beforeThreadLastAccessQuery.getColumnIndexOrThrow(ThreadTable.FIELD_LAST_ACCESS));
					if(strDate!=null){
						try {
							timeStampThread = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(strDate);
						} catch (ParseException e) {
							// This is note supposed to happen
							e.printStackTrace();
						}
					}
				}

			}

			affectedRows = dbAccesser.update(
					ThreadTable.TABLE_NAME,
					values,
					ThreadTable.FIELD_NUMBER + " = ?",
					new String[]{ phoneNumber });


			{
				maxTimeStampMsgQuery = 
						dbAccesser.query(MessageTable.TABLE_NAME, new String[]{"MAX("+MessageTable.FIELD_TIMESTAMP+") as maxMsgTimestamp"}, 
								MessageTable.FIELD_NUMBER+"=?", 
								new String[]{phoneNumber}, null, null, null);
				if(maxTimeStampMsgQuery!=null && maxTimeStampMsgQuery.moveToFirst()){
					String strDate = maxTimeStampMsgQuery.getString(maxTimeStampMsgQuery.getColumnIndexOrThrow("maxMsgTimestamp"));
					try {
						if(strDate!=null){
							timeStampMsg = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(strDate);
						}
					} catch (ParseException e) {
						// This is not supposed to happen
						e.printStackTrace();
					}
				}

			}
		}catch(StaleDataException e){
			// Do nothing...
		}

		if(maxTimeStampMsgQuery!=null && !maxTimeStampMsgQuery.isClosed()){
			maxTimeStampMsgQuery.close();
		}

		if(beforeThreadLastAccessQuery!=null && !beforeThreadLastAccessQuery.isClosed()){
			beforeThreadLastAccessQuery.close();
		}

		dbAccesser.setTransactionSuccessful();
		dbAccesser.endTransaction();

		if(timeStampMsg!=null){
			if(timeStampThread!=null && timeStampThread.compareTo(timeStampMsg)>=0){
				affectedRows = 0;
			}
		}else{
			// This may happen if the thread as no messages
			affectedRows = 0;	// No need for refreshing
		}




		//dbReader.close();
		return affectedRows;
	}

	/**
	 * 
	 * @param phoneNumber
	 * @return
	 */
	public int deleteAllMessagesFromThread(String phoneNumber){
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		int affectedRows = dbWriter.delete(MessageTable.TABLE_NAME, MessageTable.FIELD_NUMBER+" LIKE ?", new String[]{phoneNumber});

		//dbWriter.close();
		return affectedRows;

	}
}
