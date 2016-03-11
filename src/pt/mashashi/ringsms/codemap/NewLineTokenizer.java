package pt.mashashi.ringsms.codemap;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

public class NewLineTokenizer implements Tokenizer{

	public final static char SEPARATOR = '\n';
	
	public final static String SEPARATOR_STR = Character.toString(SEPARATOR);
	
	public NewLineTokenizer() {}

	@Override
	public int findTokenEnd(CharSequence text, int cursor) {
		int i = cursor;
		while (i > 0 && text.charAt(i - 1) != SEPARATOR) {
			i--;
		}
		while (i < cursor && text.charAt(i) == SEPARATOR) {
			i++;
		}
		return i;
	}

	@Override
	public int findTokenStart(CharSequence text, int cursor) {
		int i = cursor;
		int len = text.length();

		while (i < len) {
			if (text.charAt(i) == SEPARATOR) {
				return i;
			} else {
				i++;
			}
		}

		return len;
	}

	@Override
	public CharSequence terminateToken(CharSequence text) {
		int i = text.length();

		while (i > 0 && text.charAt(i - 1) == SEPARATOR) {
			i--;
		}

		if (i > 0 && text.charAt(i - 1) == SEPARATOR) { return text; } 
		else  if (text instanceof Spanned) {
			SpannableString sp = new SpannableString(text + Character.toString(SEPARATOR));
			TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
			return sp;
		}else { 
			return text + Character.toString(SEPARATOR); 
		}
		
	}

}
