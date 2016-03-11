package pt.mashashi.ringsms.chat.compose;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.chat.ContactDTO;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class ContactSugestionAdapter extends ArrayAdapter<ContactDTO>{
	
	private ContactFilter nameFilter;
	private List<ContactDTO> all;
	private List<ContactDTO> suggestions;
	
	public ContactSugestionAdapter(Context context, int textViewResourceId, LinkedList<ContactDTO> data) {
		super(context, textViewResourceId, data);
		all = Collections.unmodifiableList(data);
		suggestions = new LinkedList<ContactDTO>();
		nameFilter = new ContactFilter(this, all);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		ContactHolder holder = null;
		ContactDTO contact = suggestions.get(position);

		if(row == null){

			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();

			row = inflater.inflate(R.layout.contact_suggestion, parent, false);

			holder = new ContactHolder();
			holder.name = (TextView)row.findViewById(R.id.name);
			holder.phone = (TextView)row.findViewById(R.id.number);

			row.setTag(holder);

		}else{
			holder = (ContactHolder)row.getTag();
		}

		holder.name.setText(contact.getName());
		holder.phone.setText(contact.getPhone());

		return row;

	}
	@Override
	public void clear() {
		suggestions.clear();
		super.clear();
	}
	@Override
	public void add(ContactDTO object) {
		suggestions.add(object);
		super.add(object);
	}
	@Override
	public int getCount() {
		return suggestions.size();
	}
	@Override
	public ContactDTO getItem(int position) {
		return suggestions.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public Filter getFilter() {
		return nameFilter;
	}
	
	private static class ContactHolder{
		TextView name;
		TextView phone;
	}

	public void newData(List<ContactDTO> allContacts) {
		synchronized (all) {
			nameFilter.newData(allContacts);
			all=allContacts;
			suggestions.clear();
			notifyDataSetChanged();
		}
	}
	public List<ContactDTO> getAll() {
		synchronized (all) {
			return all;
		}
	}
}
