package org.d1scw0rld.wordmatex;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import org.d1scw0rld.wordmatex.R;

public class Settings extends Activity
{
   CheckBox notify;
   CheckBox wordlist;

   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.settings);
      wordlist = (CheckBox) findViewById(R.id.wordlist);
      notify = (CheckBox) findViewById(R.id.notify);
   }

   protected void onStart()
   {
      super.onStart();
      wordlist.setChecked(getPref("Wordlist", true));
      notify.setChecked(getPref("Notify", true));
      wordlist.setOnCheckedChangeListener(new OnCheckedChangeListener()
      {
         
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            putPref("Wordlist", isChecked);
            
         }
      });
      this.notify.setOnCheckedChangeListener(new OnCheckedChangeListener()
      {
         
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            putPref("Notify", isChecked);
            if(isChecked)
            {
               putPref("Update", MODE_PRIVATE);
            }
         }
      });
   }

   boolean getPref(String key, boolean defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getBoolean(key, defValue);
   }

   void putPref(String key, boolean value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit().putBoolean(key, value).commit();
   }

   void putPref(String key, int value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit().putInt(key, value).commit();
   }
}
