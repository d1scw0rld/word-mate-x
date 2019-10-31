package org.d1scw0rld.wordmatex;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Settings extends AppCompatActivity
{
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      getSupportFragmentManager().beginTransaction()
                                 .replace(android.R.id.content, new WordMatePreferenceFragment())
                                 .commit();
   }

   public static class WordMatePreferenceFragment extends PreferenceFragmentCompat
   {


      @Override
      public View onCreateView(LayoutInflater inflater,
                               ViewGroup container,
                               Bundle savedInstanceState)
      {
         return super.onCreateView(inflater, container, savedInstanceState);
      }

      @Override
      public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
      {
         getPreferenceManager().setSharedPreferencesName(WordMateX.PREF_FILE);
         addPreferencesFromResource(R.xml.preference_screen);
      }

      @Override
      public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
      {
         super.onViewCreated(view, savedInstanceState);
      }
   }
}
