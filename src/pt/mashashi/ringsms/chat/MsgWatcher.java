package pt.mashashi.ringsms.chat;

import pt.mashashi.ringsms.R;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

public class MsgWatcher implements TextWatcher{
	
	private final char[] ALLOWED_CHARS = {' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','x','y', 'w', 'z','.',',','?','\'','!','*','=','&','|','+','-','>','<','%','$','£','€','@','#','_' };
	
	private Activity act;
	private EditText target;
	
	public MsgWatcher(Activity act, EditText target){
		this.act = act;
		this.target = target;
	}
	
	public void afterTextChanged(Editable s) {}    
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}    
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		String content = s.toString();
		next_char:for(int i=0;i<content.length();i++){
			for(char cAllowed: ALLOWED_CHARS){
				if(cAllowed==content.charAt(i) )
					continue next_char;
			}
			Toast.makeText(act, String.format(act.getString(R.string.invalid_char), content.charAt(i)), Toast.LENGTH_LONG).show();
			String str = target.getText().toString();
			str = str.replace(content.charAt(i)+"", "");
			target.setText(str);
			target.setSelection(str.length());
		}
	}    
	
}
