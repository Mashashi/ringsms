package pt.mashashi.ringsms.threads;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import pt.mashashi.ringsms.R;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

public class AboutDialog extends Dialog{

	private Context ctx = null;

	public AboutDialog(Context ctx) {
		super(ctx);
		this.ctx = ctx;
	}
	
	private class TextHtmlLoader extends AsyncTask<Void, Void, Spanned>{
		private Context ctx;
		private TextView textView;
		private int strResourceId;
		public TextHtmlLoader(Context ctx, TextView textView, int strResourceId) {
			this.ctx = ctx;
			this.textView = textView;
			this.strResourceId = strResourceId;
		}
		@Override
		protected Spanned doInBackground(Void... params) {
			return Html.fromHtml(ctx.getString(strResourceId));
		}
		@Override
		protected void onPostExecute(Spanned result) {
			textView.setText(result);
		};
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);

		TextView tv = (TextView)findViewById(R.id.help);
		new TextHtmlLoader(this.getContext(), tv, R.string.help).execute();
		
		{
			String title = getContext().getString(R.string.info);
			setTitle(title);
			TextView textView = (TextView)findViewById(android.R.id.title);
			textView.setTextSize(22);
			textView.setTypeface(null, Typeface.BOLD);
			textView.setTextColor(Color.parseColor("#cccccc"));
		}

		tv = (TextView)findViewById(R.id.legal_text);
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.legal)));
		tv = (TextView)findViewById(R.id.info_text);
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
		tv.setLinkTextColor(Color.WHITE);
		
		
		
		tv = (TextView)findViewById(R.id.info_mail);
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.mail)));
		Linkify.addLinks(tv, Linkify.ALL);


	}

	public String readRawTextFile(int id) {
		InputStream inputStream = ctx.getResources().openRawResource(id);

		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;


		StringBuilder text = new StringBuilder();
		try {
			while (( line = buf.readLine()) != null)
				text.append(line);
		} catch (IOException e) {
			return null;
		}

		return text.toString();
	}

}