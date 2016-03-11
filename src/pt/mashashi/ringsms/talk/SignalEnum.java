package pt.mashashi.ringsms.talk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.database.ThreadJitterEnum;
import android.content.Context;

/**
 * 
 * @author Rafael
 *
 */
public enum SignalEnum {
	
	ZERO(1, "0"), ONE(2, "1"), END(3, "end");
	
	private int factor;
	private String tag;
	public static final int CODE_SIZE = 6;
	public static final int  MINIMUM_CODE_LENGTH_AFTER_COMPRESSION = 4;
	private static Map<String,Tone> alphabet = new HashMap<String, Tone>();
	
	private SignalEnum(int duration, String tag){
		this.factor = duration;
		this.tag = tag;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getFactor() {
		return factor;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getTag() {
		return tag;
	}
	
	/**
	 * 
	 * @param duration
	 * @return
	 */
	public static SignalEnum getSignal(long duration, ThreadJitterEnum jitterEnum, boolean invert){
		if(duration<=jitterEnum.getLength()*ZERO.factor){
			return invert?ONE:ZERO;
		}else if(duration<=jitterEnum.getLength()*ONE.factor){
			return invert?ZERO:ONE;
		}
		return END;
	}
	
	/**
	 * 
	 * @param next
	 * @return
	 */
	public static SignalEnum getSignal(String next) {
		for(SignalEnum value : values()){
			if(next.equals(value.tag)){
				return value;
			}
		}
		return null;
	}
	
	/**
	 * Will remove the durations that ultimately resulted in the parsed character
	 * @param durations
	 * @param alphabet
	 * @return
	 */
	public static String parseString(List<Long> durations, Map<String, Tone> alphabet, ThreadJitterEnum jitterEnum, boolean invert){
		
		String result = null;
		int toRemove = 0;
		
		ArrayList<SignalEnum> signal = new ArrayList<SignalEnum>(); 
		main : for(Long duration: durations){
			signal.add(getSignal(duration,jitterEnum, invert));
			
			for(Entry<String, Tone> entry : alphabet.entrySet()){
				if(signal.equals(entry.getValue().getTone())){
					toRemove = signal.size();
					result = entry.getKey();
					break main;
				}
			}
			
		}
		
		while(toRemove--!=0){
			durations.remove(0);
		}
		
		return result;
	}
	public static final int CHARACTER_SIZE_RINGS_DEFAULT = 6;
	final static Pattern encodingMatcher = Pattern.compile("^.=\\[((0|1)\\s{1,})*(0|1)\\]$", Pattern.DOTALL);
	public static Map<String,Tone> getAlphabet(Context ctx){
		synchronized(alphabet){
			if(alphabet.size()==0){
				String[] tonesEnglish = ctx.getResources().getStringArray(R.array.tones);
				for(String tone: tonesEnglish){
					if(encodingMatcher.matcher(tone).matches()){
		
						String bits = tone.substring(3,tone.length()-1);
						Scanner bitScanner = new Scanner(bits);
						bitScanner.useDelimiter("\\s{1,}");
						ArrayList<SignalEnum> values = new ArrayList<SignalEnum>();
						while(bitScanner.hasNext()){
							values.add(SignalEnum.getSignal(bitScanner.next()));
						}
						
						alphabet.put(tone.charAt(0)+"", new Tone(values));
					}else{
						throw new IllegalArgumentException();
					}
				}
				alphabet = Collections.unmodifiableMap(alphabet);
			}
		}
		return alphabet;
	} 
	
	public static Tone parseString(String toneStr){
		ArrayList<SignalEnum> tone = new ArrayList<SignalEnum>();
		for(char c: toneStr.toCharArray()){
			tone.add(getSignal(c+""));
		}
		return new Tone(tone);
	}
	
	public static BiMap<String, Tone> convertToToneMap(Map<String, String> inMap){
		BiMap<String, Tone> outMap = HashBiMap.create();
		for(Entry<String, String> entry: inMap.entrySet()){
			ArrayList<SignalEnum> values = new ArrayList<SignalEnum>();
			for(char elem: entry.getKey().toCharArray()){
				values.add(SignalEnum.getSignal(""+elem));
			}
			outMap.put(entry.getValue(), new Tone(values));
		}
		return outMap;
	}
	
}