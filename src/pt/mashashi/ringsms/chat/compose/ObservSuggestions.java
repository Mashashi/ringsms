package pt.mashashi.ringsms.chat.compose;

import java.util.ArrayList;
import java.util.List;

import pt.mashashi.ringsms.chat.ChatActivity;
import pt.mashashi.ringsms.chat.ContactDTO;
import pt.mashashi.ringsms.chat.ContactUtilsSingleton;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;

public class ObservSuggestions extends ContentObserver{

	private final ChatActivity act;

	public ObservSuggestions(ChatActivity act) {
		super(null);
		this.act = act;
	}

	@Override
	public void onChange(boolean selfChange) {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					List<ContactDTO> result = ContactUtilsSingleton.getAllContactObjects(act);
					Message updateSuggestionList = new Message();
					updateSuggestionList.what=ComposeActivity.REFRESH_SUGGESTION_LIST;
					Bundle bundle = new Bundle();
					bundle.putParcelableArrayList(ComposeActivity.NEW_SUGGESTION_LIST,new ArrayList<ContactDTO>(result));
					updateSuggestionList.setData(bundle);
					ComposeActivity.refresherSuggestions.sendMessage(updateSuggestionList);
				}catch(Exception e){
					// This is not supposed to happen
					e.printStackTrace();
				}
			}
		})).start();
	}	

	public Uri getUri(){
		return ContactUtilsSingleton.getUriAllContacts();
	}

}
