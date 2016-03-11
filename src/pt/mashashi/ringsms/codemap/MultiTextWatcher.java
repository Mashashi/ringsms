package pt.mashashi.ringsms.codemap;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import pt.mashashi.ringsms.MyLog;
import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.threads.ThreadsActivity;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

public class MultiTextWatcher implements TextWatcher{

	public static class DeleteTokenAction implements OnClickListener {
		private MultiAutoCompleteTextView tokenContainer;
		private boolean delete;
		public DeleteTokenAction(MultiAutoCompleteTextView tokenContainer) {
			this.tokenContainer = tokenContainer;
			delete=false;
		}
		public boolean isDelete(){
			boolean localDelete=delete;
			delete=false;
			return localDelete;
		}
		@Override
		public void onClick(View v) {
			synchronized (EditText.class) {
				String currentText = tokenContainer.getText().toString();
				if(currentText.length()!=0){
					delete=true;
					tokenContainer.setText(currentText.substring(0, currentText.length()-1));
				}
			}
		}
	}

	private Context ctx;
	private MultiAutoCompleteTextView multiEditorParam;
	private Collection<String> codes;
	DeleteTokenAction deleteTokenAction;

	public MultiTextWatcher(Context ctx,
			MultiAutoCompleteTextView multiEditorParam,
			Collection<String> codes, DeleteTokenAction deleteTokenAction) {
		this.ctx = ctx;
		this.multiEditorParam = multiEditorParam;
		this.codes = codes;
		this.deleteTokenAction = deleteTokenAction;
		allowedStrings = new LinkedList<String>();
		allowedStrings.addAll(Collections2.transform(codes, new Function<String, String>() {
			@Override public String apply(String arg) {
				if(arg.length()==0){
					throw new IllegalArgumentException();
				}
				return arg+NewLineTokenizer.SEPARATOR_STR; 
			}
		}));
	}


	private List<String> allowedStrings;

	private String beforeChanged="";
	@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
	@Override public void beforeTextChanged(CharSequence s, int start, int count,int after) {
		beforeChanged = s.toString(); 
	}
	
	public synchronized boolean wasEdit(){
		
		
		if(beforeChanged.length()>0){
			int last = beforeChanged.lastIndexOf("\n");
			if(last!=-1 || beforeChanged.charAt(beforeChanged.length()-1)!=NewLineTokenizer.SEPARATOR){
				return true;
			}
		}
		return false;
	}
	boolean ignoreBeforeThisIsSetResgular=false;
	@Override public synchronized void afterTextChanged(Editable s) {

		MyLog.d(ThreadsActivity.DEBUG_TAG, "This is:"+this.hashCode());

		boolean clickOnDropDown = false;
		String content = s.toString();
		String lastEntryIncomplete = null;

		if(!deleteTokenAction.isDelete()){

			int countContained=0;
			Set<String> matches = new HashSet<String>();

			if(content.length()!=0){

				if(!ignoreBeforeThisIsSetResgular){
					boolean isEdit = content.charAt(content.length()-1)!=NewLineTokenizer.SEPARATOR;
					if(!isEdit && beforeChanged.length()>0){
						// Check if this is a backspace
						char lastCharBefore = beforeChanged.charAt(beforeChanged.length()-1);
						isEdit = isEdit || lastCharBefore!=NewLineTokenizer.SEPARATOR;
						// Check if the last character was the new line
						char lastCharActual = content.charAt(content.length()-1);
						isEdit = isEdit || (lastCharBefore==NewLineTokenizer.SEPARATOR && lastCharActual==NewLineTokenizer.SEPARATOR);
						isEdit = isEdit && content.length()<beforeChanged.length(); // Otherwise a item in the drop down was selected
					}
					if(isEdit){

						int start = 0;
						int indexOfNewLine = content.lastIndexOf(NewLineTokenizer.SEPARATOR_STR);
						if(start!=-1){ start=indexOfNewLine+1; }

						lastEntryIncomplete = content.substring(start,content.length());
						Collection<String> starters =null;
						{
							final String lastEntry = lastEntryIncomplete;
							starters = Collections2.filter(allowedStrings, new Predicate<String>(){
								@Override public boolean apply(String arg) { return arg.startsWith(lastEntry); }
							});
						}
						if(starters.size()==0){ lastEntryIncomplete = null; }

					}else {

						if(wasEdit()){
							clickOnDropDown = true;

							int start=0; int end=0;
							int last = beforeChanged.lastIndexOf(NewLineTokenizer.SEPARATOR_STR);
							if(last!=-1){ end=last+1; }
							String bufferContent = content.substring(start, end); // The before correct part

							int toDelete = beforeChanged.length()-bufferContent.length();
							content=content.substring(end+toDelete,content.length());
							content=bufferContent+content;
						}

					}
				}
			}





			StringBuffer contentTracker = new StringBuffer(content);
			do{
				for(final String allowedString: allowedStrings){
					int firstIndex = contentTracker.indexOf(allowedString);
					if(firstIndex!=-1){
						matches.add(allowedString);
						contentTracker=contentTracker.delete(firstIndex, firstIndex+allowedString.length());
						countContained+=allowedString.length();
					} else {
						matches.remove(allowedString);
					}
				}
			}while(matches.size()!=0);

			if(lastEntryIncomplete!=null){ 
				countContained+=lastEntryIncomplete.length(); 
			}

			if(countContained!=content.length()){

				Toast.makeText(ctx, R.string.invalid_string, Toast.LENGTH_LONG).show();
				multiEditorParam.setText(beforeChanged);

			} else {
				// Set of conditions that triger the refresh of the list
				if(clickOnDropDown){
					lastEntryIncomplete="";
					ignoreBeforeThisIsSetResgular=true;
					multiEditorParam.setText(content); // Put the new valid text									
				}

				if(ignoreBeforeThisIsSetResgular){	
					ignoreBeforeThisIsSetResgular =false;
					lastEntryIncomplete="";
				}

				if(content.length()==0){
					lastEntryIncomplete="";
				}

			}

			if(lastEntryIncomplete!=null){


				final String lastEntry = lastEntryIncomplete;
				Collection<String> equalCodes = Collections2.filter(codes, new Predicate<String>(){
					@Override public boolean apply(String arg) { return arg.equals(lastEntry); }
				});
				if(equalCodes.size()==1){
					ignoreBeforeThisIsSetResgular=true;
					multiEditorParam.setText(content+NewLineTokenizer.SEPARATOR);
				}else{
					@SuppressWarnings("unchecked")
					ArrayAdapter<String> adapter = ((ArrayAdapter<String>)multiEditorParam.getAdapter());
					adapter.getFilter().filter(lastEntry);
				}

			}
		}else{

			int lastEntryOf = content.lastIndexOf(NewLineTokenizer.SEPARATOR_STR);
			if(lastEntryOf!=-1){

				String deletedLastEntry = content.substring(0, lastEntryOf)+NewLineTokenizer.SEPARATOR_STR;
				ignoreBeforeThisIsSetResgular=true;
				multiEditorParam.setText(deletedLastEntry);

			}else if(content.length()!=0){
				multiEditorParam.setText("");
			}
		}
		multiEditorParam.setSelection(multiEditorParam.getText().toString().length());
	}

}
