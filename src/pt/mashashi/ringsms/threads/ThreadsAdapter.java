package pt.mashashi.ringsms.threads;

import java.util.LinkedList;
import java.util.List;

import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.R;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ThreadsAdapter extends ArrayAdapter<ThreadDAO>{

	private Context context;  
	private List<ThreadDAO> data;
	
	public ThreadsAdapter(final Context context, List<ThreadDAO> data, final TextView threadCount) {
		super(context, 0, data);
		this.context = context;
		this.data = data;
		registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				int count = ThreadsAdapter.this.getCount();
				int strResource = R.string.total_threads;
				if(ThreadsAdapter.this.data.size()==1)
					strResource = R.string.total_thread;
				
				String countMessage = String.format(context.getString(strResource), Integer.toString(count));
				threadCount.setText(countMessage);
			}
		});
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		LogHolder holder = null;
		ThreadDAO thread = data.get(position);

		if(row == null){

			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(R.layout.thread, parent, false);
			holder = new LogHolder();

			holder.name = (TextView) row.findViewById(R.id.name);
			holder.photo = (ImageView) row.findViewById(R.id.photo); 
			holder.lastMsg = (TextView) row.findViewById(R.id.last_message);
			holder.timeStampLastMsg = (TextView) row.findViewById(R.id.last_message_time);
			holder.msgCount = (TextView) row.findViewById(R.id.count_thread_msgs);
			
			row.setTag(holder);

		}else{
			holder = (LogHolder)row.getTag();
		}

		holder.name.setText(thread.getName());
		
        if(thread.getPhoto()!=null){
        	holder.photo.setImageBitmap(thread.getPhoto());
        }
        
        
        holder.lastMsg.setText(thread.getLastMsg()==null?"":thread.getLastMsg());
        String dateTimeStr = thread.getTimeStampLastMsg()!=null?RotinesUtilsSingleton.formatTimeStamp(getContext(), thread.getTimeStampLastMsg()):"";
        holder.timeStampLastMsg.setText(dateTimeStr);
        holder.msgCount.setText("("+Integer.toString(thread.getThreadMessageCount())+")");
        
        
        int defaultColor = Color.WHITE;
        
        if(	thread.getTimeStampLastMsg()!=null && 
        	( thread.getLastAccess()==null||thread.getLastAccess().compareTo(thread.getTimeStampLastMsg())<0) ){
        		defaultColor=Color.parseColor("#B4CDCD");
        }
        row.setBackgroundColor(defaultColor);
        return row;
	}
	
	@Override
	public void add(ThreadDAO msg) {
		data.add(msg);
		notifyDataSetChanged();
	}
	@Override
	public void remove(ThreadDAO log) {
		data.remove(log);
		notifyDataSetChanged();
	}
	@Override
	public ThreadDAO getItem(int position) {
		return data.get(position);
	}
	@Override
	public int getCount() {
		return data.size();
	}
	
	public void newData(List<ThreadDAO> threads){
		data.removeAll(data);
		data.addAll(threads);
		notifyDataSetChanged();
	}
	
	public List<String> getAllPhoneNumbers(){
		List<String> phones = new LinkedList<String>();
		for(ThreadDAO thread : data){
			phones.add(thread.getNumber());
		}
		return phones;
	}
	
	static class LogHolder{
		TextView name;
		ImageView photo;
		TextView lastMsg;
		TextView timeStampLastMsg;
		TextView msgCount;
	}

}
