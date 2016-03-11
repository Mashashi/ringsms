package pt.mashashi.ringsms;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.google.android.vending.licensing.util.Base64;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateFormat;

public class RotinesUtilsSingleton {

	private RotinesUtilsSingleton(){}
	
	
	public static String formatTimeStamp(Context ctx, Date timeStamp){
		Format dateFormatLocal = DateFormat.getDateFormat(ctx);
		Format timeFormatLocal = DateFormat.getTimeFormat(ctx);
		String dateStr = dateFormatLocal.format(timeStamp);
		String timeStr = timeFormatLocal.format(timeStamp);
		String dateTimeStr = String.format(ctx.getString(R.string.time_stamp), dateStr, timeStr);
		return dateTimeStr;
	}

	public static long doChecksum(String text) throws UnsupportedEncodingException {
		byte bytes[] = text.getBytes("UTF-8"); // This makes this hash function platform independent
		Checksum checksum = new CRC32();
		checksum.update(bytes,0,bytes.length);
		long lngChecksum = checksum.getValue();
		return lngChecksum;
	}
	
	public static List<String> getSortedStringList(Collection<String> toSort){
		List<String> sorted = new LinkedList<String>();
		final PriorityQueue<String> ordered = new PriorityQueue<String>(toSort.size()+1/*In case the toSort is empty*/, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				lhs = lhs.replaceAll("[^a-zA-Z0-9]", "");
				rhs = rhs.replaceAll("[^a-zA-Z0-9]", "");
				int result = rhs.compareTo(lhs);
				return result;
			}
		});
		ordered.addAll(toSort);
		int originalSize=ordered.size();
		for(int i=0;i<originalSize;i++){
			sorted.add(ordered.poll());
		}
		return sorted;
	}
	
	public static byte[] hexStringToByteArray(String s) {
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++){
			int index = i * 2;
			int v = Integer.parseInt(s.substring(index, index + 2), 16);
			b[i] = (byte)v;
		}
		return b;
	}
	
	
	public static String byteArrayToHexString(byte[] b){
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++){
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase(Locale.US);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// DANGER! THIS IS AN DEV API METHOD CHANGE IT CAN LEAD TO INCOMPATIBILITIES IN PROGRAMS DEVELOPED TO RINGSMS
	public static String doHmac(String text, String pass) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException{
		SecretKeySpec key = new SecretKeySpec(pass.getBytes("UTF-8"), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(key);
		byte[] bytes = mac.doFinal(text.getBytes("UTF-8"));
		return new String(Base64.encode(bytes));
	}
	// DANGER! THIS IS AN DEV API METHOD CHANGE IT CAN LEAD TO INCOMPATIBILITIES IN PROGRAMS DEVELOPED TO RINGSMS
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
		// Using guava library. Note if we make a declaratio
		List<String> listHexString = Lists.transform(result, new Function<Integer, String>() {
			@Override
			public String apply(Integer arg) {
				return Integer.toHexString(arg);
			}
		});
		return Joiner.on("").join(listHexString);
	}


	public static boolean comparePhones(String phone1, String phone2) {
		return PhoneNumberUtils.compare(phone1.trim(), phone2.trim());
	}
	
}
