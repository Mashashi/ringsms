package pt.mashashi.ringsms.chat;

import java.util.List;

import pt.mashashi.ringsms.RotinesUtilsSingleton;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.MsgStatusEnum;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MsgsAdapter extends BaseAdapter{

	private Context ctx;  
	private List<MsgDAO> data;

	public enum MsgLayouts {MESSAGE_US_LAYOUT, MESSAGE_THEY_LAYOUT, MESSAGE_US_CANCELLED, MESSAGE_US_BROKEN_CHANNEL, MESSAGE_THEY_ERROR, MESSAGE_THEY_URGENT, MESSAGE_US_URGENT}

	public MsgsAdapter(Context context, List<MsgDAO> data) {
		this.ctx = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		MessageHolder holder = null;
		MsgDAO message = data.get(position);
		boolean outgoingMessage = message.getDirection().equals(MsgDirectionEnum.OUTGOING);
		MsgStatusEnum status = message.getStatus();
		
		if(row == null){

			LayoutInflater inflater = ((Activity)ctx).getLayoutInflater();
			
			Integer layout = null;
			
			if(outgoingMessage){
				switch(status){
				case SUCCESS:
				{
					layout = R.layout.message_us;
					break;
				}
				case SEND_CANCELLED:
				{
					layout = R.layout.message_us_cancelled;
					break;
				}
				case CHANNEL_BROKEN:
				{
					layout = R.layout.message_us_broken_channel;
					break;
				}
				case URGENT_SENT:
				{
					layout = R.layout.message_us_urgent;
					break;
				}
				default: throw new IllegalArgumentException();
				}
			}else{
				switch(status){
				case SUCCESS:
				{
					layout = R.layout.message_they;
					break;
				}
				case RECEIVED_WITH_ERROR:
				{
					layout = R.layout.message_they_error;
					break;
				}
				case URGENT_RECEIVED:
				{
					layout = R.layout.message_they_urgent;
					break;
				}
				default: throw new IllegalArgumentException();
				}
			}
			
			
			
			if(layout==null)
				throw new IllegalArgumentException();
			
			row = inflater.inflate(layout, parent, false);
			
			holder = new MessageHolder();
			holder.text = (TextView)row.findViewById(R.id.message_txt);
			holder.date = (TextView)row.findViewById(R.id.message_date);

			row.setTag(holder);

		}else{
			holder = (MessageHolder)row.getTag();
		}

		holder.text.setText(message.getText());

		String dateTimeStr = RotinesUtilsSingleton.formatTimeStamp(ctx, message.getTimeStamp());
		holder.date.setText(dateTimeStr);

		return row;

	}

	public void addItem(MsgDAO msg) {
		data.add(msg);
		notifyDataSetChanged();
	}
	@Override
	public int getCount() {
		return data.size();
	}
	@Override
	public MsgDAO getItem(int position) {
		return data.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public boolean isEnabled(int position) {
		return false;
	}
	@Override
	public int getItemViewType(int position) {
		MsgDAO message = data.get(position);
		boolean outgoingMessage = message.getDirection().equals(MsgDirectionEnum.OUTGOING);
		MsgStatusEnum status = message.getStatus();
		
		MsgLayouts msgLayout = null;
		
		if(outgoingMessage){
			switch(status){
				case SUCCESS:
				{
					msgLayout = MsgLayouts.MESSAGE_US_LAYOUT;
					break;
				}
				case SEND_CANCELLED:
				{
					msgLayout = MsgLayouts.MESSAGE_US_CANCELLED;
					break;
				}
				case CHANNEL_BROKEN:
				{
					msgLayout = MsgLayouts.MESSAGE_US_BROKEN_CHANNEL;
					break;
				}
				case URGENT_SENT:
				{
					msgLayout = MsgLayouts.MESSAGE_US_URGENT;
					break;
				}
				default: throw new IllegalArgumentException();
			}
		}else{
			switch(status){
			case SUCCESS:
			{
				msgLayout = MsgLayouts.MESSAGE_THEY_LAYOUT;
				break;
			}
			case RECEIVED_WITH_ERROR:
			{
				msgLayout = MsgLayouts.MESSAGE_THEY_ERROR;
				break;
			}
			case URGENT_RECEIVED:
			{
				msgLayout = MsgLayouts.MESSAGE_THEY_URGENT;
				break;
			}
			default: throw new IllegalArgumentException();
			}
		}
		
		return  msgLayout.ordinal();
	}
	@Override
	public int getViewTypeCount() {
		return MsgLayouts.values().length;
	}
	public void newData(List<MsgDAO> newData){
		data.removeAll(data);
		data.addAll(newData);
		notifyDataSetChanged();
	}
	private static class MessageHolder{
		TextView text;
		TextView date;
	}

}