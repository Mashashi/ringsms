package pt.mashashi.ringsms.licensing;

import android.content.Context;
import android.provider.Settings.Secure;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.ResponseData;

/**
 * 
 * @author Rafael
 * 
 * See MILLIS_PER_MINUTE on APKExpansionPolicy
 * 
 */
public class MyPolicy  extends APKExpansionPolicy{
	
	private static final byte[] SALT = new byte[] {
	     6, 53, 103, 46, (byte)220, (byte)163, (byte)132, (byte)255, 115, (byte)231, 21, 
	     (byte)241, 33, 2, 114, (byte)245, 52, 57, 36, (byte)204 
	     };
	
	public MyPolicy(Context ctx) {
		super(ctx, new AESObfuscator(SALT, ctx.getPackageName(), Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID)));
	}
	@Override
	public void processServerResponse(int response, ResponseData rawData) {
		super.processServerResponse(response, rawData);
	}
	@Override
	public boolean allowAccess() {
		return super.allowAccess();
	}
	
}
