package pt.mashashi.ringsms.chat;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ContactDTO implements Parcelable{
	
	private String name;
	private String phone;
	private Uri uri;
	
	public enum ContactAttributeEnum { URI, NAME, PHONE; 
		
		public String getData(ContactDTO contact){
			switch (this) {
				case URI:
					return contact.getUri().toString();
				case NAME:
					return contact.getName();
				case PHONE:
					return contact.getPhone();
				default:
					break;
			}
			return null;
		}
	}
	
	public static Parcelable.Creator<ContactDTO> CREATOR = new Parcelable.Creator<ContactDTO>() {
		@Override public ContactDTO createFromParcel(Parcel arg) {
			return new ContactDTO(arg.readString(), arg.readString(), Uri.parse(arg.readString()));
		}
		@Override public ContactDTO[] newArray(int size) {
			return new ContactDTO[size];
		}
	};
	
	public ContactDTO(String name, String phone, Uri uri) {
		this.name = name;
		this.phone = phone;
		this.uri = uri;
	}
	public String getName() {
		return name;
	}
	public String getPhone() {
		return phone;
	}
	public Uri getUri() {
		return uri;
	}
	@Override 
	public int describeContents() { 
		return 0; 
	}
	@Override
	public void writeToParcel(Parcel target, int argumentIndex) {
		switch (argumentIndex) {
		case 0:
			target.writeString(name);
			break;
		case 1:
			target.writeString(phone);
			break;
		case 2:
			target.writeString(uri.toString());
			break;
		default:
			break;
		}
	}
	@Override
	public String toString() {
		return "("+name+","+phone+","+uri.toString()+")";
	}
}
