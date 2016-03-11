package pt.mashashi.ringsms.codemap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.MultiAutoCompleteTextView;

public class MyMultiAutoCompleteTextView extends MultiAutoCompleteTextView{
	private final static long minInterval=500;
	private long lastTouch=System.currentTimeMillis()-2*minInterval;
			
	public MyMultiAutoCompleteTextView(Context context) {
		super(context);
	}
	public MyMultiAutoCompleteTextView(Context context, AttributeSet attributes) {
		super(context,attributes);
	}
	boolean showKeyboard = false;
	
	// Deactivate cursor positioning
	@Override
    public void onSelectionChanged(int start, int end) {
        CharSequence text = getText();
        if (text != null) {
            if (start != text.length() || end != text.length()) {
                setSelection(text.length(), text.length());
                return;
            }
        }
        super.onSelectionChanged(start, end);
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		boolean result = super.onTouchEvent(event);
		this.showDropDown();
		if(event.getAction() ==MotionEvent.ACTION_DOWN){
			long timeout = lastTouch+minInterval;
			long now = System.currentTimeMillis();
			if(timeout<now){
				if(!showKeyboard){
					InputMethodManager in = (InputMethodManager) super.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(this.getApplicationWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
				}
			}else{
				showKeyboard=!showKeyboard;
			}
			lastTouch=System.currentTimeMillis();
		}else{
			if(!showKeyboard){
				InputMethodManager in = (InputMethodManager) super.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(this.getApplicationWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
			}
		}
		this.setSelection(this.getText().length());
		return result;
	}
	
}
