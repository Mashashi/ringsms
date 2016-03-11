package pt.mashashi.ringsms.chat;

import java.util.Date;

import pt.mashashi.ringsms.database.MsgDirectionEnum;
import pt.mashashi.ringsms.database.MsgStatusEnum;

public class MsgDAO {
	
	private String number;
	private String text;
	private MsgDirectionEnum direction;
	private Date timeStamp;
	private MsgStatusEnum status;
	
	public MsgDAO(String number, String text, MsgDirectionEnum direction, Date timeStamp, MsgStatusEnum status){
		this.number = number;
		this.text = text;
		this.direction = direction;
		this.timeStamp = timeStamp;
		this.status = status;
	}
	public String getNumber() {
		return number;
	}
	public String getText() {
		return text;
	}
	public MsgDirectionEnum getDirection() {
		return direction;
	}
	public Date getTimeStamp() {
		return timeStamp;
	}
	public MsgStatusEnum getStatus() {
		return status;
	}
}