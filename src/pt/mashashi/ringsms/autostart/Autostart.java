package pt.mashashi.ringsms.autostart;

import pt.mashashi.ringsms.interfazze.InterfazzeStarterService;
import pt.mashashi.ringsms.threads.GeneralPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Autostart extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		try{ 
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			boolean autoStart = sp.getBoolean(GeneralPreferences.AUTOSTART, GeneralPreferences.PREFERENCE_DEFAULT_AUTOSTART);
			if(autoStart){
				Intent registerPhoneLister = new Intent(context, PhoneStateListenerStarterService.class);
				context.startService(registerPhoneLister);
			}
			
			String interfazze_password = sp.getString(GeneralPreferences.INTERFAZZE_PASSWORD, "");
			if(interfazze_password.length()!=0){
				Intent registerPhoneLister = new Intent(context, InterfazzeStarterService.class);
				context.startService(registerPhoneLister);
			}
		}catch(Exception e){
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

}
