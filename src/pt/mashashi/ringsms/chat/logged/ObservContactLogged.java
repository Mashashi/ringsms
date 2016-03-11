package pt.mashashi.ringsms.chat.logged;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import pt.mashashi.ringsms.chat.ObservContact;

public class ObservContactLogged extends ObservContact{

	public ObservContactLogged(ChatActivity act, Uri selectedContact, String phone) {
		super(act, selectedContact, phone);
	}

	@Override
	protected void updatePhoto(Uri uriContact, Bundle bundleRefreshMessage) {
		Bitmap photo = null;
		if(uriContact!=null){
			photo = ContactUtilsSingleton.getContactPhoto(act.getContentResolver(), uriContact);
		}
		byte[] photoData = null;
		if(photo!=null){
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			photo.compress(CompressFormat.JPEG, /*quality*/ 100, blob);
			photoData = blob.toByteArray();
		}
		bundleRefreshMessage.putByteArray(LoggedActivity.CONTACT_PHOTO_HANDLE_1, photoData);
	}

	@Override
	protected void updateName(String name, Bundle bundleRefreshMessage) {
		bundleRefreshMessage.putString(LoggedActivity.CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA, name);
	}
	
}
