package pt.mashashi.ringsms.chat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.UnableToRetrieveDataException;
import pt.mashashi.ringsms.chat.ContactDTO.ContactAttributeEnum;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.ListView;

@SuppressWarnings("unused")
public class ContactUtilsSingleton {

	private ContactUtilsSingleton(){}
	
	
	// DANGER! THIS IS AN DEV API METHOD CHANGE IT CAN LEAD TO INCOMPATIBILITIES IN PROGRAMS DEVELOPED TO RINGSMS
	public static final String PHONE_NUMBER_PATTERN_MANDATORY = "^([^0-9*#+]*[0-9*#+]+[^0-9*#+]*)+$"; 
	public static final String PHONE_NUMBER_PATTERN = "^[0-9/(),.N*;#+ ]+$";
	public static boolean isPhoneNumber(String phone){
		boolean isPhoneNumber = phone.matches(ContactUtilsSingleton.PHONE_NUMBER_PATTERN);
		boolean isValidPhoneNumber = phone.matches(ContactUtilsSingleton.PHONE_NUMBER_PATTERN_MANDATORY);
		return isPhoneNumber && isValidPhoneNumber;
	}



	public static final String ORDER_POLICY = ContactsContract.Contacts.DISPLAY_NAME+" ASC";

	public static final String CURSOR_NULL_EXCEPTION_MSG = "The cursor returned null";

	/**
	 * This will do a exact search on the specified URI.
	 * 
	 * @param phone
	 * @return  null if no entry was found else it returns the first occurrence by alphabetic ascendent order
	 * @throws UnableToRetrieveDataException 
	 */
	public static ContactDTO  getContactByUri(Context ctx, Uri uri) throws UnableToRetrieveDataException {
		Cursor c=null;
		ContactDTO result = null;
		try{
			// Returning null
			c =  ctx.getContentResolver().query(uri,  new String[]{PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER, ContactsContract.Contacts._ID},null,null, ORDER_POLICY);
			if(c==null)
				throw new UnableToRetrieveDataException(CURSOR_NULL_EXCEPTION_MSG);
			if (c.moveToFirst()) {
				String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				String phone = c.getString(c.getColumnIndexOrThrow(PhoneLookup.NUMBER));
				String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
				result =new ContactDTO(name, phone, getUriFromContactId(id));
			}
		}catch(IllegalArgumentException e){
			// Can be thrown in the method query if a exception is thrown because of the parameter phone is not valid
		}catch(StaleDataException e){	
		}finally{
			if (c!=null){
				c.close();
			}
		}

		return result;
	}

	/**
	 * This will match any phone entry which as for instances 91 in is number so 219102929 would get matched.
	 * 
	 * @param contactData
	 * @return A list of the available phones for the given Uri
	 */
	public static List<ContactDTO> searchContactPhones(Activity act, String phoneNumberFrag){
		LinkedList<ContactDTO> phonesList = new LinkedList<ContactDTO>();

		String phoneWhere = ContactsContract.CommonDataKinds.Phone.NUMBER + " like ? "; 
		String[] phoneWhereParams = new String[]{phoneNumberFrag};
		Cursor phoneCur = null;

		phoneCur = act.managedQuery(ContactsContract.Data.CONTENT_URI, null, phoneWhere, phoneWhereParams, ORDER_POLICY);

		try{
			if(phoneCur!=null && phoneCur.moveToFirst()){
				LinkedList<String> addedPhones = new LinkedList<String>();
				do{
					String name = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					String phone = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER ));
					String id = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID ));
					addedPhones.add(phone);
					if(phone!=null && ContactUtilsSingleton.isPhoneNumber(phone) && !addedPhones.contains(phone)){
						// Contact have an available phone number
						phonesList.add(new ContactDTO(name, phone, getUriFromContactId(id)));
					}
				}while(phoneCur.moveToNext());
			}
		}catch(StaleDataException e){
		}finally{
			if(phoneCur!=null){
				if(!phoneCur.isClosed()){
					act.stopManagingCursor(phoneCur);
					phoneCur.close(); 
				} 
			}else{
				// On managedQuery return null, retrieve the empty list
			}
		}
		return phonesList;
	}



	/**
	 * 
	 * @param contactData
	 * @return A list of the available phones for the given Uri
	 * @throws UnableToRetrieveDataException 
	 */
	public static List<ContactDTO> getContacts(Activity act, Uri contactData) throws UnableToRetrieveDataException{
		LinkedList<ContactDTO> phonesList = new LinkedList<ContactDTO>();
		try{
			Cursor c =null;
			c =  act.managedQuery(contactData, null, null, null, ORDER_POLICY);

			if(c==null)
				throw new UnableToRetrieveDataException(CURSOR_NULL_EXCEPTION_MSG);

			if (!c.isClosed() && c.moveToFirst()) {
				LinkedList<String> addedPhones = new LinkedList<String>();
				do{
					String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
					final String trueness = "1";

					if (hasPhone.equals(trueness)) {
						String name = c.getString(c.getColumnIndex( ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
						String contactId = c.getString(c.getColumnIndex( ContactsContract.Contacts._ID));

						phonesList.addAll(getPhoneNumbersFromContactId(act,addedPhones, contactId, name));

					}
				}while( !c.isClosed() // This guard is necessary because sometimes the activity is closed and the resources allocated to that activity are freed 
						&& 
						c.moveToNext());
			}

			if(!c.isClosed()){
				act.stopManagingCursor(c);
				c.close();
			}
		}catch(StaleDataException e){
			// To once and for all solve the all issue of accessing a cursor after it has been closed
		}
		return phonesList;
	}
	private static LinkedList<ContactDTO> getPhoneNumbersFromContactId(Activity act, List<String> addedPhones, String contactId, String name){

		LinkedList<ContactDTO> result = new LinkedList<ContactDTO>();

		Cursor phones = null;
		phones = act.getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
		if(phones!=null){
			try{
				if(phones.moveToFirst()){
					do{
						String number = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));
						if(!addedPhones.contains(number)){
		
							Uri uri = getUriFromContactId(Long.parseLong(contactId));
		
							result.add(new ContactDTO(name, number, uri));
							addedPhones.add(number);
						}
					} while (phones.moveToNext());
				}
				phones.close();
			}catch(StaleDataException e){}
		} else {
			// Do nothing not all phones will be presented to the user
		}

		return result;
	}
	public static LinkedList<String> filterContacts(ContactAttributeEnum element, List<ContactDTO> contactList){
		Iterator<ContactDTO> ites = contactList.iterator();
		LinkedList<String> collected = new LinkedList<String>();
		while(ites.hasNext()){
			ContactDTO contact = ites.next();
			collected.add(element.getData(contact));
		}
		return collected;
	}
	public static List<ContactDTO> getAllContactObjects(Activity act) throws UnableToRetrieveDataException{
		return ContactUtilsSingleton.getContacts(act, ContactUtilsSingleton.getUriAllContacts());
	}
	public static LinkedList<ContactDTO> contactsContainsKeyValues(ContactAttributeEnum key, String value, List<ContactDTO> contacts){
		Iterator<ContactDTO> searchIte = contacts.iterator();
		LinkedList<ContactDTO> result = new LinkedList<ContactDTO>();
		while(searchIte.hasNext()){
			ContactDTO entry = searchIte.next();
			if(key.equals(ContactAttributeEnum.PHONE)){
				if(RotinesUtilsSingleton.comparePhones(entry.getPhone(), value)){
					result.add(entry);
				}
			}else{
				if(key.getData(entry).equals(value)) 
					result.add(entry);
			}
		}
		return result;
	}





	/**
	 * 
	 * @param cr
	 * @param uri Can be null
	 * @return Retrieves the contact photo or null if no photo is available. Or null if any photo is available.
	 */	
	public static Bitmap getContactPhoto(ContentResolver cr, Uri uri) {
		Bitmap photo = null;
		if(uri!=null){
			InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
			if (input != null) {
				photo = BitmapFactory.decodeStream(input);
				try {
					input.close();
				} catch (IOException e) {
					// TODO
				}
			}
		}
		return photo;
	}

	/**
	 * This will do a exact search on the specified phone number
	 * @param act
	 * @param phoneNumber
	 * @return
	 * @throws UnableToRetrieveDataException 
	 */
	public static ContactDTO getContactFromPhone(Context act, String phoneNumber) throws UnableToRetrieveDataException{
		Uri uri = getUriFromContactPhone(phoneNumber);
		ContactDTO result = getContactByUri(act, uri);
		return result;
	}

	public static Uri getContactUriForObserver(Uri contactUri){
		return contactUri==null?getUriAllContacts():contactUri;
	}

	public static Uri getUriAllContacts(){
		return ContactsContract.Contacts.CONTENT_URI;
	}

	public static Uri getUriFromContactId(String id){
		return  getUriFromContactId(Long.parseLong(id));
	}

	public static Uri getUriFromContactPhone(String phone){
		return Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
	}

	public static Uri getUriFromContactId(long id){
		return ContentUris.withAppendedId(getUriAllContacts(), id);
	}
	
	/**
	 * Presents the user a interface to pick the desired phone to contact from the many available to the same user.
	 * Then it uses the method {@link #getComponentAt(String, String, String) openContactConversation} to apply the selection.
	 * 
	 * @param phoneNumbers
	 * @param name
	 * @param contactId
	 */
	public static void choosePhoneFromContact(Context ctx, String title, LinkedList<String> phoneNumbers, final String name, final String contactId, final Handler setData){

		// The contact has more than one phone which one are we going to use?
		final AlertDialog.Builder choose = new AlertDialog.Builder(ctx);
		final String[] choices = phoneNumbers.toArray(new String[phoneNumbers.size()]);
		final int selected = 0;

		choose.setSingleChoiceItems(choices, selected, null);
		choose.setNegativeButton(R.string.cancel, null);
		choose.setPositiveButton(R.string.select, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// The which parameter models the action, in this case -1
				ListView lw = ((AlertDialog)dialog).getListView();
				String checkedItem = (String)lw.getAdapter().getItem(lw.getCheckedItemPosition());
				//openContactConversation(name, checkedItem, contactId);
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString(NAME_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER, name);
				data.putString(PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER, checkedItem);
				msg.setData(data);
				setData.sendMessage(msg);
			}
		});
		choose.setTitle(title);
		choose.show();
	}
	public static String NAME_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER = "nameChoosePhone";
	public static String PHONE_DISPLAY_RESULTS_CHOOSE_PHONE_HANDLER = "phoneChoosePhone";
}
