package pt.mashashi.ringsms.chat.compose;

import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.chat.ContactDTO;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class ContactFilter extends Filter{
	
	private final List<ContactDTO> allLocal;
	private final ArrayAdapter<ContactDTO> listViewAdapter;
	
	private final static long MAXIMUM_TIME = 5;
	private final static long MINIMUM_DISPLAY_CONTACTS = 4;
	
	public ContactFilter(ArrayAdapter<ContactDTO> listViewAdapter, List<ContactDTO> all) {
		allLocal = new LinkedList<ContactDTO>(all);
		this.listViewAdapter = listViewAdapter;
	}
	@Override
    public String convertResultToString(Object resultValue) {
        String str = ((ContactDTO)(resultValue)).getPhone(); 
        return str;
    }
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        if(constraint != null) {
            LinkedList<ContactDTO> suggestions = new LinkedList<ContactDTO>();
            long start = System.currentTimeMillis();
            int displayContacts = 0;
            
            synchronized(allLocal){	//SYNC ALLLOCAL
	            for (ContactDTO contact : allLocal) {
	                if(contact.getPhone()!=null){
	                	if(contact.getPhone().contains(constraint.toString())){
		                    suggestions.add(contact);
		                    displayContacts++;
		                    // Check if the search is taking to long
		                    // Check here to reduce the overhead
		                    long end = System.currentTimeMillis();
		                    boolean maxContacts = displayContacts == MINIMUM_DISPLAY_CONTACTS;
		                    if((end-start>MAXIMUM_TIME && maxContacts)||maxContacts) 
		                    	break;
	                	}
	                }
	            }
            }
            
            FilterResults filterResults = new FilterResults();
            filterResults.values = suggestions;
            filterResults.count = suggestions.size();
            return filterResults;
        } else {
            return new FilterResults();
        }
    }
    @SuppressWarnings("unchecked")
	@Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
    	LinkedList<ContactDTO> filteredList = (LinkedList<ContactDTO>) results.values;
    	long start = System.currentTimeMillis();
    	int displayContacts = 0;
    	
        if(results != null && results.count > 0) {
            listViewAdapter.clear();
            for (ContactDTO c : filteredList) {
            	listViewAdapter.add(c);
            	displayContacts++;
            	// Check if the search is taking to long
                long end = System.currentTimeMillis();
                boolean maxContacts = displayContacts == MINIMUM_DISPLAY_CONTACTS;
                if((end-start>MAXIMUM_TIME && maxContacts)||maxContacts) 
                	break;
            }
            listViewAdapter.notifyDataSetChanged();
        }
    }
    public List<ContactDTO> getContacts(){
    	return allLocal;
    }
	public void newData(List<ContactDTO> allContacts) {
		synchronized(allLocal){ //SYNC ALLLOCAL
			allLocal.removeAll(allLocal);
			allLocal.addAll(allContacts);
		}
	}
    
}
