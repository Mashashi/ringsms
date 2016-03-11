package pt.mashashi.ringsms.licensing;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.Policy;

import pt.mashashi.ringsms.R;
import pt.mashashi.ringsms.threads.ThreadsActivity;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class LicensingActivity extends Activity{
	
	private static String LISENCING_PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkF7a6QWYlwd08UzFWDsTlf1cF6I/ruxp45/H/MApwtk7Vx0aY6C95KPvW480TGaPfU4wqg/U3n5BC47ikDtAtNnWXwZj05bSAAjAGg+6PSl10lqHOANItu7rSbeONuuYUG8k/EueZk9vkD/omWhc/RSz8G06t2tKaIMWF/AVG4Qy2skKSmn+SBPRhPE8qfZ30LsX8XwoYSkXZdsa6463USsvxowY5DgKTEh2LE9SNruIq832FVbnZD+1tOVJw94SrIkuj6j7yxJ6Tqk8Jj2YwbKe2eD+3KBC4KU+/M92Ww3GEPpVWpJFEJrVscal4GaESirY1joMXfDzVx1dEkWG3wIDAQAB";
	private LicenseChecker mChecker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.licensing);
		setTitle(R.string.ringsms_activation);
		
		activate();
		
		Button activationButton = (Button) this.findViewById(R.id.try_licensing);
		activationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activate();
			}
		});
		
		/*
		startActivity(new Intent(this, ThreadsActivity.class));
		finish();
		*/
	}
	private void activate(){
		final Activity ctx = LicensingActivity.this;
		final ProgressDialog pd = ProgressDialog.show(ctx, ctx.getString(R.string.activating_ringsms), ctx.getString(R.string.this_shouldnt_take_long));
		
		class CheckAsync extends AsyncTask<Void, Void, Integer>{
			public CheckAsync() {}
			@Override
			protected Integer doInBackground(Void... arg0) {
				if(mChecker==null){
					mChecker = new LicenseChecker(ctx, new MyPolicy(ctx), LISENCING_PUBLIC_KEY_BASE64);
				}
				MyLicenseCheckerCallBack checkerCallBack = new MyLicenseCheckerCallBack(this);
				synchronized(this){
					mChecker.checkAccess(checkerCallBack);
					try {
						while(checkerCallBack.getReason()==null){
							this.wait();
						}
					} catch (InterruptedException e) {}
				}
				return  checkerCallBack.getReason();
			}
			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
				if(result.equals(Policy.LICENSED)){
					pd.dismiss();
					startActivity(new Intent(ctx, ThreadsActivity.class));
					ctx.finish();
				}else{
					pd.dismiss();
					Toast.makeText(ctx, R.string.failed_activation, Toast.LENGTH_LONG).show();
				}
			}
		};
		new CheckAsync().execute();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mChecker!=null){
			mChecker.onDestroy();
		}
	}
}
