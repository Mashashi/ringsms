package pt.mashashi.ringsms.chat.compose;

import android.net.Uri;
import android.os.Bundle;
import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ObservContact;

public class ObservContactCompose extends ObservContact{

	public ObservContactCompose(ChatActivity act, Uri selectedContact, String phone) {
		super(act, selectedContact, phone);
	}

	@Override
	protected void updateName(String name, Bundle bundleRefreshMessage) {
		bundleRefreshMessage.putString(ChatActivity.CONTACT_NAME_REFRESH_CONTACT_HANDLER_WHAT_1_DATA, name);
	}

}
