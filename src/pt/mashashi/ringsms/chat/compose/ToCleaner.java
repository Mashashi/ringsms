package pt.mashashi.ringsms.chat.compose;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;

class ToCleaner implements OnTouchListener{
	
	private boolean executed;
	
	public ToCleaner() {
		executed=false;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!executed){
			formatAsExecuted((EditText) v);
			executed=true;
		}
		return false;
	}
	public void setExecuted(EditText edit) {
		formatAsExecuted(edit);
		executed=true;
	}
	private void formatAsExecuted(EditText edit){
		edit.setText("");
		edit.setTextColor(Color.BLACK);
	}
	public boolean getExecuted(){
		return executed;
	}
}