package pt.mashashi.ringsms.talk;

import pt.mashashi.ringsms.database.MsgDirectionEnum;

public interface OnNewRefreshMessageListener {
	void onNewMessage(String phone, MsgDirectionEnum direction);
	void onNewMessageNotificationPlaced(String phone);
}
