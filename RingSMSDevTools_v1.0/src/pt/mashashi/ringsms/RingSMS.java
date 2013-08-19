package pt.mashashi.ringsms;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * A graphic element that plots mathematical functions.
 * 
 * @author Rafael Campos
 * @version 1.0
 */
public class RingSMS {
	
	private RingSMS(){
		throw new IllegalArgumentException("This isn't instantiable.");
	}
	
	/*public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, RequestOverAllowedMaxCharactersException {
		
		Calendar cal = Calendar.getInstance();
		cal.set(2013, 7, 14, 16, 3, 0);
		System.out.println(buildRequestCode("aaaa", 555555555, "916557758", cal.getTime(), "00000000.00000000"));
		
		String password = transformIn256BitHexKey("oi");
		System.out.println(password);
		System.out.println(password.length());
		
	}*/
	
	/*public static void main(String[] args) {
		System.out.println("Nothing to see here");
	}*/
	
	public static final int MAXIMUM_CHARACTERS_ALLOWED = 80;
	
	@SuppressWarnings("serial")
	public static class RequestOverAllowedMaxCharactersException extends Exception{
		public RequestOverAllowedMaxCharactersException() {
			super("The resulting code would have more then 80 characters.");
		}
	}
	
	public static final String PHONE_NUMBER_PATTERN_MANDATORY = "^([^0-9*#+]*[0-9*#+]+[^0-9*#+]*)+$"; 
	public static final String PHONE_NUMBER_PATTERN = "^[0-9/(),.N*;#+ ]+$";
	public static boolean isPhoneNumber(String phone){
		boolean isPhoneNumber = phone.matches(PHONE_NUMBER_PATTERN);
		boolean isValidPhoneNumber = phone.matches(PHONE_NUMBER_PATTERN_MANDATORY);
		return isPhoneNumber && isValidPhoneNumber;
	}
	
	private final static SimpleDateFormat TIME_STAMP = new SimpleDateFormat("yyyyMMddHHmm");
	
	public static String buildRequestCode(String password, long nounce,String message_destination, Date timestamp,String codes) 
			throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, RequestOverAllowedMaxCharactersException{
		
		if(nounce<0){
			throw new IllegalArgumentException("nounce can't be negative");
		}
		if(!isPhoneNumber(message_destination)){
			throw new IllegalArgumentException("message_destination is not a valid phone number");
		}
		if(!codes.matches("^(0|1|\\.)+$")){
			throw new IllegalArgumentException("code must match ^(0|1|\\.)+$");
		}
		
		StringBuilder request = new StringBuilder("");
		request.append(nounce);
		request.append("|");
		request.append(message_destination);
		request.append("|");
		request.append(TIME_STAMP.format(timestamp));
		request.append(",");
		request.append(codes);
		request.append(",");
		
		request.append(doHmac(request.toString(), password));
		
		if(request.length()>MAXIMUM_CHARACTERS_ALLOWED){
			throw new RequestOverAllowedMaxCharactersException();
		}
		return request.toString();
	}
	
	private static String doHmac(String text, String pass) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException{
		SecretKeySpec key = new SecretKeySpec(pass.getBytes("UTF-8"), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(key);
		byte[] bytes = mac.doFinal(text.getBytes("UTF-8"));
		return new String(Base64.encode(bytes));
	}
	
	public static String transformIn256BitHexKey(String key){
		final int NECESSARY_BYTES = 256/8;
		byte[] bytes = key.getBytes();
		List<Integer> result = new LinkedList<Integer>();
		for(int i=0;i<bytes.length && result.size()<NECESSARY_BYTES;i++){
		
			int left = bytes[i] & 0x0f;
			int right = bytes[i] & 0xf0; right>>=4;
		
			if(right!=0){ result.add(right);}
			result.add(left);
			
		}
		while(result.size()<NECESSARY_BYTES){
			result.add(0);
		}
		
		List<String> listHexString = Lists.transform(result, new Function<Integer, String>() {
			@Override
			public String apply(Integer arg) {
				return Integer.toHexString(arg);
			}
		});
		return Joiner.on("").join(listHexString);
	}
	
}
