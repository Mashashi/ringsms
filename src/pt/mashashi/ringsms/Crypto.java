package pt.mashashi.ringsms;

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class Crypto {
	
	/**
	 * 
	 * @param value
	 * @param key
	 * @param algorithm AES, RC2, Blowfish...
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static String encrypt(String value, String key, String algorithm) throws GeneralSecurityException {
		SecretKeySpec sks = new SecretKeySpec(RotinesUtilsSingleton.hexStringToByteArray(key), algorithm);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, sks, cipher.getParameters());
		byte[] encrypted = cipher.doFinal(value.getBytes());
		return RotinesUtilsSingleton.byteArrayToHexString(encrypted);
	}

	public static String decrypt(String message, String key, String algorithm) throws GeneralSecurityException {
		SecretKeySpec sks = new SecretKeySpec(RotinesUtilsSingleton.hexStringToByteArray(key), algorithm);
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, sks);
		byte[] decrypted = cipher.doFinal(RotinesUtilsSingleton.hexStringToByteArray(message));
		return new String(decrypted);
	}   
}