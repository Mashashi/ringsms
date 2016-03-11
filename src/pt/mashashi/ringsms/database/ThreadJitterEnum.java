package pt.mashashi.ringsms.database;

import pt.mashashi.ringsms.R;
import android.content.Context;

public enum ThreadJitterEnum { 

	SHORT(5, R.string.short_jitter),
	MEDIUM(7, R.string.medium_jitter),
	LONG(9, R.string.long_jitter),
	HUGE(10, R.string.huge_jitter);

	private int label;
	private int length;

	/**
	 * 
	 * @param length In seconds
	 * @param label
	 */
	private ThreadJitterEnum(int length, int label){
		this.length = length;
		this.label = label;
	}
	public int getLabel() {
		return label;
	}
	public int getLength() {
		return length;
	}
	public int getLengthInMilisecs(){
		return this.length*1000;
	}
	public static ThreadJitterEnum biggestJitterEnum(){
		ThreadJitterEnum biggestJitterEnum = null;
		for(ThreadJitterEnum value : values()){
			if(biggestJitterEnum==null||biggestJitterEnum.getLength()<value.length)
				biggestJitterEnum = value;
		}
		return biggestJitterEnum;
	}
	public static String[] getAllLabel(Context context) {
		String[] values = new String[values().length];
		for(int i=0;i<values().length;i++){
			values[i] = context.getString(values()[i].getLabel());
		}
		return values;
	}
	
}
