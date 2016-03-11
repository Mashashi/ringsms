package pt.mashashi.ringsms.database;

import java.text.SimpleDateFormat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RingSMSDBHelper extends SQLiteOpenHelper {
	
	//public native void load(SQLiteDatabase databaseName);
	/*static {
		MyLog.d(ThreadsActivity.DEBUG_TAG, "Loading module");
        System.loadLibrary("RingSMS");
        MyLog.d(ThreadsActivity.DEBUG_TAG, "Module loaded");
    }
	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		SQLiteDatabase rd = super.getReadableDatabase();
		//load(rd);
		return rd;
	}
	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		SQLiteDatabase rd = super.getWritableDatabase();
		//load(rd);
		return rd;
	}*/
	
	
	
	@SuppressLint("SimpleDateFormat")
	public static final SimpleDateFormat SQLLITE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static class InterfazzeUsedNounces{
		
		public static final String TABLE_NAME = "interfazze_used_nounce";
		
		public static final String FIELD_USED_NOUNCE = "_id";
		public static final String FIELD_USED_DATE = "timeStampUsed";
		
		private static final String SQL_CREATE_INTERFAZZE_USED_NOUNCES_TABLE = 
				"CREATE TABLE " + TABLE_NAME + " (" 
				+ FIELD_USED_NOUNCE + " INTEGER PRIMARY KEY," 
				+ FIELD_USED_DATE + " DATE DEFAULT CURRENT_TIMESTAMP NOT NULL);";
		
		@SuppressWarnings("unused")
		private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
		
	}
	
	public static class CodeMappingTable{
		
		public static final String TABLE_NAME = "code_mapping";
		
		public static final String FIELD_CODE_MAPPINGS = "_id_personal_code_mappings";
		
		public static final String FIELD_CODE = "code";		
		public static final String FIELD_MAPPING_STRING = "mapping_string";
		
		private static final String SQL_CREATE_CODE_MAPPING_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" +
				FIELD_CODE_MAPPINGS + " INTEGER," +
				FIELD_CODE + " TEXT," +
				FIELD_MAPPING_STRING +" TEXT," + // The text can not repeat it self for the same code mappings and cannot be empty
				//"CONSTRAINT chk_"+FIELD_MAPPING_STRING+" CHECK (SELECT true WHERE EXISTS (SELECT COUNT(*)=1 as countit FROM "+TABLE_NAME+" as a,"+TABLE_NAME+" as b WHERE a."+FIELD_MAPPING_STRING+" LIKE b."+FIELD_MAPPING_STRING+" AND a."+FIELD_CODE_MAPPINGS+" = b."+FIELD_CODE_MAPPINGS+" GROUP BY a."+FIELD_MAPPING_STRING+")<=1),"+
				"PRIMARY KEY("+FIELD_CODE_MAPPINGS+","+FIELD_CODE+"),"+
				"FOREIGN KEY(" + FIELD_CODE_MAPPINGS + ") REFERENCES " + CodeMappingsTable.TABLE_NAME + "(" + CodeMappingsTable.FIELD_ID+ ") ON DELETE CASCADE);";
		
		@SuppressWarnings("unused")
		private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
		
	}
	
	public static class CodeMappingsTable{
		
		public static final String TABLE_NAME = "code_mappings";
		
		public static final String FIELD_ID = "_id";
		public static final String FIELD_MAPPINGS_NAME = "mappings_name";
		
		private static final String SQL_CREATE_CODE_MAPPINGS_TABLE = 
				"CREATE TABLE " + TABLE_NAME + " (" +
				FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
				FIELD_MAPPINGS_NAME + " TEXT);";
		
		@SuppressWarnings("unused")
		private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
		
	}

	public class ThreadTable{

		public static final String TABLE_NAME = "thread";

		
		
		// Primary key must be called _id in order to fully use the features provided by android platform
		public static final String FIELD_NUMBER = "_id";
		// In seconds, the end message code is 3 times this value
		public static final String FIELD_JITTER = "jitter";
		// In bits number
		public static final String FIELD_CHECK_SUM_LENGTH = "check_sum_length";
		// The last time the thread was accessed
		public static final String FIELD_LAST_ACCESS = "last_access";
		// Saved scratch
		public static final String FIELD_SCRATCH = "scratch";
		//
		public static final String FIELD_CODE_MAPPINGS_TABLE = "_id_personal_code_mappings";
		
		private static final String SQL_CREATE_THREAD_TABLE =
						"CREATE TABLE " + TABLE_NAME + " (" +
						FIELD_NUMBER + " TEXT PRIMARY KEY," +
						FIELD_JITTER + " INTEGER NOT NULL,"+
						FIELD_CHECK_SUM_LENGTH +" INTEGER NOT NULL,"+
						FIELD_LAST_ACCESS+" DATETIME DEFAULT NULL," + 
						FIELD_SCRATCH+" TEXT NOT NULL,"+
						FIELD_CODE_MAPPINGS_TABLE+" INTEGER DEFAULT NULL,"+
						"FOREIGN KEY(" + FIELD_CODE_MAPPINGS_TABLE + ") REFERENCES " + CodeMappingsTable.TABLE_NAME + "(" + CodeMappingsTable.FIELD_ID+ ") ON DELETE SET DEFAULT);";

		@SuppressWarnings("unused")
		private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
		//By deleting all threads we are deleting all associated messages to
		public static final String SQL_DELETE_ALL_THREADS = "DELETE FROM " + TABLE_NAME; 
		
		
		public static final String SQL_QUERY_LIST_THREADS_SINGLE = "SELECT t."+FIELD_NUMBER+", t."+FIELD_SCRATCH+", t."+FIELD_JITTER+", t."+FIELD_CHECK_SUM_LENGTH+", NULL as lastMessage, "
				+ "t."+FIELD_LAST_ACCESS+", t."+ FIELD_CODE_MAPPINGS_TABLE+", 0 as thread_messages_count,"
				+ " NULL as thread_last_msg_timestamp FROM "+TABLE_NAME+" as t WHERE t."+FIELD_NUMBER+" NOT IN"
				+ " (SELECT ti."+FIELD_NUMBER+" FROM "+TABLE_NAME+" as ti INNER JOIN "+MessageTable.TABLE_NAME+" as m ON "
				+ "ti."+FIELD_NUMBER+"=m."+MessageTable.FIELD_NUMBER+")";

		public static final String SQL_QUERY_LIST_THREADS = "SELECT "
				+ "t."+FIELD_NUMBER+","
				+ " (SELECT "+FIELD_SCRATCH+" FROM "+TABLE_NAME+" WHERE "+TABLE_NAME+"."+FIELD_NUMBER+"=t."+FIELD_NUMBER+") as "+FIELD_SCRATCH+","
				+ " (SELECT "+FIELD_JITTER+" FROM "+TABLE_NAME+" WHERE "+TABLE_NAME+"."+FIELD_NUMBER+"=t."+FIELD_NUMBER+") as "+FIELD_JITTER+","
				+ " (SELECT "+FIELD_CHECK_SUM_LENGTH+" FROM "+TABLE_NAME+" WHERE "+TABLE_NAME+"."+FIELD_NUMBER+"=t."+FIELD_NUMBER+") as "+FIELD_CHECK_SUM_LENGTH+","
				+ " (SELECT "+MessageTable.FIELD_TEXT+" FROM "+MessageTable.TABLE_NAME+" WHERE "+MessageTable.TABLE_NAME+"."+MessageTable.FIELD_ID+"=m."+MessageTable.FIELD_ID+") as lastMessage,"
				+ " (SELECT "+FIELD_LAST_ACCESS+" FROM "+TABLE_NAME+" WHERE "+TABLE_NAME+"."+FIELD_NUMBER+"=t."+FIELD_NUMBER+") as "+FIELD_LAST_ACCESS+","
				+ " (SELECT "+FIELD_CODE_MAPPINGS_TABLE+" FROM "+TABLE_NAME+" WHERE "+TABLE_NAME+"."+FIELD_NUMBER+"=t."+FIELD_NUMBER+") as "+FIELD_CODE_MAPPINGS_TABLE+","
				+ " COUNT(t."+FIELD_NUMBER+") as thread_messages_count, "
				+ " MAX(m."+MessageTable.FIELD_TIMESTAMP+") as thread_last_msg_timestamp"
				+ " FROM "+TABLE_NAME+" t INNER JOIN "+MessageTable.TABLE_NAME+" m ON "
				+ " t."+FIELD_NUMBER+"=m."+MessageTable.FIELD_NUMBER
				+ " GROUP BY t."+FIELD_NUMBER;
		
	}

	public class MessageTable{

		public static final String TABLE_NAME = "message";

		public static final String FIELD_ID = "_id";
		public static final String FIELD_NUMBER = "thread_number";
		public static final String FIELD_TEXT = "text";
		public static final String FIELD_DIRECTION = "direction";
		public static final String FIELD_TIMESTAMP = "timestamp";
		public static final String FIELD_STATUS = "status";
		
		private static final String SQL_CREATE_MESSAGE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" +
						FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						FIELD_NUMBER + " TEXT NOT NULL CONSTRAINT [FK_Number] REFERENCES ["+ThreadTable.TABLE_NAME+"](["+ThreadTable.FIELD_NUMBER+"]),"+
						FIELD_TEXT + " TEXT NOT NULL," +
						FIELD_DIRECTION + " INTEGER NOT NULL,"+
						FIELD_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,"+
						FIELD_STATUS	+ " INTEGER NOT NULL,"+
						"FOREIGN KEY(" + FIELD_NUMBER + ") REFERENCES " + ThreadTable.TABLE_NAME + "(" + ThreadTable.FIELD_NUMBER + ") ON DELETE CASCADE);";

		@SuppressWarnings("unused")
		private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + MessageTable.TABLE_NAME;
		public static final String SQL_RESTART_NUMBERING = "DELETE FROM sqlite_sequence WHERE name='"+MessageTable.TABLE_NAME+"'";

	}

	// If you change the database schema, you must increment the database version.
	public static final int DATABASE_VERSION = 5;
	public static final String DATABASE_NAME = "RingSMS.db";
	
	// Is a singleton
	private static RingSMSDBHelper instance;
	private RingSMSDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		//context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
		//load(this.getReadableDatabase());
		//MyLog.d(ThreadsActivity.DEBUG_TAG, "Load called");
	}
	public synchronized static RingSMSDBHelper getInstance(Context ctx){
		if(instance==null){
			instance = new RingSMSDBHelper(ctx);
		}
		return instance;
	}

	public void onCreate(SQLiteDatabase db) {
		//load(db);
		
		db.execSQL(InterfazzeUsedNounces.SQL_CREATE_INTERFAZZE_USED_NOUNCES_TABLE);
		
		db.execSQL(ThreadTable.SQL_CREATE_THREAD_TABLE);
		db.execSQL(MessageTable.SQL_CREATE_MESSAGE_TABLE);
		
		db.execSQL(CodeMappingsTable.SQL_CREATE_CODE_MAPPINGS_TABLE);
		db.execSQL(CodeMappingTable.SQL_CREATE_CODE_MAPPING_TABLE);
		
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// The upgrade policy can be simply to discard the data and start over 
		/*db.execSQL(InterfazzeUsedNounces.SQL_DROP_TABLE);
		
		db.execSQL(MessageTable.SQL_DROP_TABLE);
		db.execSQL(ThreadTable.SQL_DROP_TABLE);
		
		db.execSQL(CodeMappingTable.SQL_DROP_TABLE);
		db.execSQL(CodeMappingsTable.SQL_DROP_TABLE);
		
		onCreate(db);*/
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

}