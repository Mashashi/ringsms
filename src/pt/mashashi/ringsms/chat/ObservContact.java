package pt.mashashi.ringsms.chat;

import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ContactDTO.ContactAttributeEnum;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;

public abstract class ObservContact extends ContentObserver{

	private Uri uri;
	private String phone;
	protected ChatActivity act;

	public ObservContact(ChatActivity act, Uri selectedContact, String phone) {
		super(null);
		this.uri = selectedContact;
		this.act = act;
		this.phone=phone;
	}

	@Override
	public void onChange(boolean selfChange) {
		List<ContactDTO> fetched = contactInsertedCheck();
		if(uri!=null){
			try {
				if(fetched==null){
					fetched = ContactUtilsSingleton.getContacts(act, uri);
				}
				String name = null;
				Uri uriContact = null;
	
				if(fetched.size()!=0){
					name = fetched.get(0).getName();
					uriContact = fetched.get(0).getUri();
				}else{
					invalidateSelectedContact();
				}
	
				Message refreshMessage = new Message();
				refreshMessage.what = ChatActivity.REFRESH_CONTACT_HANDLER_WHAT_1;
	
				{
					Bundle bundleRefreshMessage = new Bundle();
					updateName(name, bundleRefreshMessage);
					updatePhoto(uriContact, bundleRefreshMessage);
					refreshMessage.setData(bundleRefreshMessage);
				}
	
				act.refresherContact.sendMessage(refreshMessage);
			} catch (UnableToRetrieveDataException e) {
				// Skip interface refresh 
			}
		}
	}

	protected abstract void updateName(String name, Bundle bundleRefreshMessage);
	protected void updatePhoto(Uri uriContact, Bundle bundleRefreshMessage){}

	private void invalidateSelectedContact(){
		uri=null;
		act.getContentResolver().unregisterContentObserver(this);
		act.getContentResolver().registerContentObserver(ContactUtilsSingleton.getUriAllContacts(), true, this);
	}

	/**
	 * The contact didn't exist and we are currently listening for events for all the contacts
	 * waiting for a new contact with the phone number selected being introduced
	 */
	private LinkedList<ContactDTO> contactInsertedCheck(){
		LinkedList<ContactDTO> result = null;

		if(uri==null){
			// The contact didn't exist and we are currently listening for events for all the contacts
			// waiting for a new contact with the phone number selected being introduced
			try {
				List<ContactDTO> allContacts = ContactUtilsSingleton.getContacts(act, ContactUtilsSingleton.getUriAllContacts());
				
				result = ContactUtilsSingleton.contactsContainsKeyValues(ContactAttributeEnum.PHONE, phone, allContacts);
				if(result.size()!=0){
					ContactDTO selected = result.getFirst();
					uri = ContactUtilsSingleton.getUriFromContactPhone(phone);
					act.getContentResolver().unregisterContentObserver(this);
					act.getContentResolver().registerContentObserver(uri, true, this);
					result = new LinkedList<ContactDTO>();
					result.add(selected);
				}
			} catch (UnableToRetrieveDataException e) {
				// Do nothing skip interface refresh
			}
		}
		return result;
	}

	public Uri getUri(){
		return uri;
	}

}
