package pt.mashashi.ringsms.database;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.chat.MsgDAO;
import pt.mashashi.ringsms.database.RingSMSDBHelper.MessageTable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDatabase;

public class MessageDataSource {

	private RingSMSDBHelper dbHelper;

	private static MessageDataSource instance;
	private MessageDataSource(Context ctx) {
		dbHelper = RingSMSDBHelper.getInstance(ctx);
	}
	public synchronized static MessageDataSource getInstance(Context ctx){
		if(instance==null){
			instance = new MessageDataSource(ctx);
		}
		return instance;
	} 

	/**
	 * 
	 * @param phone
	 */
	public List<MsgDAO> listThreadMessages(String phone) {

		LinkedList<MsgDAO> result = new LinkedList<MsgDAO>();
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		try{
			Cursor c = dbReader.query(MessageTable.TABLE_NAME, 
					new String[]{	MessageTable.FIELD_TIMESTAMP,
					MessageTable.FIELD_TEXT,
					MessageTable.FIELD_DIRECTION,
					MessageTable.FIELD_STATUS}, 
					MessageTable.FIELD_NUMBER+"=?", 
					new String[]{phone}, null, null, null);

			if(c!=null && c.moveToFirst()){

				do{
					String dateStr= c.getString(c.getColumnIndexOrThrow(MessageTable.FIELD_TIMESTAMP));
					Date timestamp = null;
					try {
						timestamp = RingSMSDBHelper.SQLLITE_DATE_FORMAT.parse(dateStr);
					} catch (ParseException e) {
						// This is not supposed to happen
						e.printStackTrace();
					}

					String text = c.getString(c.getColumnIndexOrThrow(MessageTable.FIELD_TEXT));

					int directionIndex = c.getInt(c.getColumnIndexOrThrow(MessageTable.FIELD_DIRECTION));
					MsgDirectionEnum direction = MsgDirectionEnum.values()[directionIndex];

					int statusIndex = c.getInt(c.getColumnIndexOrThrow(MessageTable.FIELD_STATUS));
					MsgStatusEnum status = MsgStatusEnum.values()[statusIndex];

					result.add(new MsgDAO(phone, text, direction, timestamp, status));
				}while(c.moveToNext());

			}else{
				// Show empty message list
			}
			if(!c.isClosed()){
				c.close();
			}
		}catch(StaleDataException e){
			// Do nothing...
		}
		//dbReader.close();
		return result;
	}

	/**
	 * 
	 * @param number
	 * @param text
	 * @param direction
	 */
	public long insertMessage(String phoneNumber, String text, MsgDirectionEnum direction, Date timestamp, MsgStatusEnum status) {
		SQLiteDatabase dbWriter = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MessageTable.FIELD_NUMBER, phoneNumber);
		values.put(MessageTable.FIELD_TEXT, text);
		values.put(MessageTable.FIELD_DIRECTION, direction.ordinal());
		values.put(MessageTable.FIELD_TIMESTAMP, RingSMSDBHelper.SQLLITE_DATE_FORMAT.format(timestamp));
		values.put(MessageTable.FIELD_STATUS, status.ordinal());
		long newRowId = dbWriter.insert(MessageTable.TABLE_NAME, null, values);

		//dbWriter.close();
		return newRowId;
	}


	public long updateMessageStatus(Long rowId, MsgStatusEnum newStatus){
		SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
		ContentValues values = new ContentValues();
		values.put(MessageTable.FIELD_STATUS, newStatus.ordinal());

		// Which row to update, based on the ID
		String select = MessageTable.FIELD_ID + " LIKE ?";
		String[] selectionArgs = { String.valueOf(rowId) };

		int affectedRows = dbReader.update(
				MessageTable.TABLE_NAME,
				values,
				select,
				selectionArgs);

		//dbReader.close();
		return affectedRows;
	}

}
