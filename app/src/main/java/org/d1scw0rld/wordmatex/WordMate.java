package org.d1scw0rld.wordmatex;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.d1scw0rld.wordmatex.dictionary.Dict;

import java.io.IOException;

public class WordMate extends Activity
{
   static final String path = "/sdcard/wordmate/";
   ImageButton clearButton;
   int currentDict;
   int currentWord;
   TextView definitionView;
   ImageButton dictButton;
   AlertDialog dictDialog;
   DictLoader dictLoader;
   Dict[] dicts;
   Button downloaderButton;
   boolean enableWordlist;
   ImageButton goButton;
   EditText inputField;
   View messageView;
   ScrollView scroll;
   ViewSwitcher switcher;
   TextView textView;
   TextView wordView;
   ListView wordlistView;

//class C00207 implements DialogInterface.OnClickListener
//   {
//      C00207()
//      {}
//   
//      public void onClick(DialogInterface dialog, int which)
//      {
//         WordMate.this.startActivity(new Intent("android.intent.action.VIEW",
//                                                Uri.parse("http://www.wordmate.net")));
//      }
//   }

//   class C00141 implements OnClickListener
//   {
//      C00141()
//      {}
//
//      public void onClick(View v)
//      {
//         WordMate.this.startActivity(new Intent(WordMate.this,
//                                                Downloader.class));
//      }
//   }

//   class C00152 implements DialogInterface.OnClickListener
//   {
//      C00152()
//      {}
//
//      public void onClick(DialogInterface dialog, int which)
//      {
//         WordMate.this.setDict(which);
//      }
//   }
//
//   class C00163 implements OnItemClickListener
//   {
//      C00163()
//      {}
//
//      public void onItemClick(AdapterView<?> adapterView,
//                              View ACT_VIEW,
//                              int position,
//                              long id)
//      {
//         WordMate.this.setInput(((TextView) ACT_VIEW).getText().toString());
//         WordMate.this.displayContent(position);
//      }
//   }
//
//   class C00174 implements OnClickListener
//   {
//      C00174()
//      {}
//
//      public void onClick(View v)
//      {
//         WordMate.this.dictDialog.show();
//      }
//   }
//
//   class C00185 implements OnClickListener
//   {
//      C00185()
//      {}
//
//      public void onClick(View v)
//      {
//         if(WordMate.this.getRvWordList())
//         {
//            WordMate.this.displayContent();
//         }
//         else
//         {
//            WordMate.this.displayWordlist();
//         }
//      }
//   }
//
//   class C00196 implements OnClickListener
//   {
//      C00196()
//      {}
//
//      public void onClick(View v)
//      {
//         WordMate.this.setInput(null);
//         WordMate.this.onInput(null);
//      }
//   }

   DialogInterface.OnClickListener oclPositiveButton = new DialogInterface.OnClickListener()
   {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
         startActivity(new Intent("android.intent.action.VIEW",
                                  Uri.parse("http://www.wordmate.net")));
      }
   };
   
//   class C00218 implements DialogInterface.OnClickListener
//   {
//      C00218()
//      {}
//
//      public void onClick(DialogInterface dialog, int which)
//      {
//         WordMate.this.startActivity(new Intent("android.intent.action.VIEW",
//                                                Uri.parse("mailto: hongbo@wordmate.net")));
//      }
//   }
   
   DialogInterface.OnClickListener oclNeutralButton = new DialogInterface.OnClickListener()
   {
      
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
         startActivity(new Intent("android.intent.action.VIEW",
                                  Uri.parse("mailto: hongbo@wordmate.net")));
      }
   };

   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      dictButton = (ImageButton) findViewById(R.id.dictButton);
      inputField = (EditText) findViewById(R.id.inputField);
      goButton = (ImageButton) findViewById(R.id.goButton);
      clearButton = (ImageButton) findViewById(R.id.clearButton);
      switcher = (ViewSwitcher) findViewById(R.id.switcher);
      scroll = (ScrollView) findViewById(R.id.scroll);
      wordView = (TextView) findViewById(R.id.wordView);
      definitionView = (TextView) findViewById(R.id.definitionView);
      wordlistView = (ListView) findViewById(R.id.rv_word_list);
      messageView = findViewById(R.id.messageView);
      textView = (TextView) findViewById(R.id.textView);
      
      downloaderButton = (Button) findViewById(R.id.downloaderButton);
      downloaderButton.setOnClickListener(new OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            startActivity(new Intent(WordMate.this, Downloader.class));                                                }
      });
      dicts = new Dict[0];
      currentWord = -1;
   }

   protected void onStart()
   {
      super.onStart();
      dictLoader = new DictLoader(this);
      switcher.setInAnimation(null);
      switcher.setOutAnimation(null);
      switcher.setDisplayedChild(getPref("View", 0));
      enableWordlist = getPref("Wordlist", true);
      if(enableWordlist)
      {
         goButton.setVisibility(View.VISIBLE);
      }
      else
      {
         switcher.setDisplayedChild(1);
         goButton.setVisibility(View.GONE);
         if(dicts.length > 0)
         {
            onInput();
         }
      }
      inputField.requestFocus();
      inputField.selectAll();
//      UpdateManager updateManager = new UpdateManager(this);
   }

   protected void onStop()
   {
      super.onStop();
      if(dictLoader != null)
      {
         dictLoader.stop();
         dictLoader = null;
      }
      if(dicts.length > 0)
      {
         putPref("Dict", this.currentDict);
         putPref("Word", this.inputField.getText().toString());
         putPref("View", this.switcher.getDisplayedChild());
      }
   }

   void restart()
   {
      finish();
      startActivity(getIntent());
   }

   void onLoad(Dict[] newDicts)
   {
      dictLoader = null;
      if(dicts.length == 0)
      {
         if(newDicts.length == 0)
         {
            messageView.setVisibility(View.VISIBLE);
            if(Environment.getExternalStorageState().startsWith("mounted"))
            {
               textView.setText(R.string.no_dict);
               downloaderButton.setVisibility(View.VISIBLE);
               return;
            }
            textView.setText(R.string.no_sdcard);
            downloaderButton.setVisibility(View.GONE);
            return;
         }
         dicts = newDicts;
         init();
      }
      else if(dicts.length == newDicts.length)
      {
         int i = 0;
         while(i < dicts.length)
         {
            if(dicts[i].equals(newDicts[i]))
            {
               i++;
            }
            else
            {
               restart();
               return;
            }
         }
      }
      else
      {
         restart();
      }
   }

   void init()
   {
      String[] titles = new String[dicts.length];
      for(int i = 0; i < dicts.length; i++)
      {
         titles[i] = dicts[i].title;
      }
      dictDialog = new Builder(this).setTitle(R.string.dict_dialog)
                                    .setItems(titles, new DialogInterface.OnClickListener()
                                    {
                                       @Override
                                       public void onClick(DialogInterface dialog,
                                                           int which)
                                       {
                                          setDict(which);
                                       }
                                       
                                    })
                                    .create();
      wordlistView.setOnItemClickListener(new OnItemClickListener()
      {
         @Override
         public void onItemClick(AdapterView<?> adapterView,
                                 View view,
                                 int position,
                                 long id)
         {
           setInput(((TextView) view).getText().toString());
           displayContent(position);
         }
      });
      dictButton.setOnClickListener(new OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            dictDialog.show();
         }
      });
      goButton.setOnClickListener(new OnClickListener()
      {
         
         @Override
         public void onClick(View v)
         {
            if(WordMate.this.isWordlistView())
            {
               displayContent();
            }
            else
            {
               displayWordlist();
            }
         }
      });
      clearButton.setOnClickListener(new OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            setInput(null);
            onInput(null);
         }
      });
      setInput(getPref("Word", null));
      setDict(getPref("Dict", 0));
      inputField.addTextChangedListener(new InputWatcher(this));
   }

   void setDict(int n)
   {
      if(n >= dicts.length)
      {
         n = 0;
      }
      currentDict = n;
      setTitle(dicts[n].title);
      wordlistView.setAdapter(new WordlistAdapter(this));
      currentWord = -1;
      if(isContentView())
      {
         displayContent();
      }
      else
      {
         displayWordlist();
      }
   }

   void setInput(String word)
   {
      inputField.requestFocus();
      inputField.setText(word);
      inputField.selectAll();
   }

   void onInput()
   {
      onInput(inputField.getText().toString());
   }

   void onInput(String word)
   {
      if(enableWordlist)
      {
         displayWordlist(word);
      }
      else
      {
         displayContent(word);
      }
   }

   void displayContent()
   {
      displayContent(inputField.getText().toString());
   }

   void displayContent(String word)
   {
      try
      {
         displayContent(dicts[currentDict].query(word));
      }
      catch(IOException e)
      {
         showError(e.getMessage());
      }
   }

   void displayContent(int index)
   {
      showContentView();
      if(currentWord != index)
      {
         currentWord = index;
         scroll.scrollTo(0, 0);
         try
         {
            wordView.setText(dicts[currentDict].getWord(index));
            definitionView.setText(Html.fromHtml(dicts[currentDict].getDefinition(index)));
         }
         catch(IOException e)
         {
            showError(e.getMessage());
         }
      }
   }

   void displayWordlist()
   {
      displayWordlist(inputField.getText().toString());
   }

   void displayWordlist(String word)
   {
      try
      {
         displayWordlist(dicts[currentDict].query(word));
      }
      catch(IOException e)
      {
         showError(e.getMessage());
      }
   }

   void displayWordlist(int index)
   {
      showWordlistView();
      wordlistView.setSelection(index);
   }

   void showContentView()
   {
      if(!isContentView())
      {
         switcher.setOutAnimation(this, R.anim.slide_left_1);
         switcher.setInAnimation(this, R.anim.slide_left_2);
         switcher.showNext();
      }
   }

   void showWordlistView()
   {
      if(!isWordlistView())
      {
         switcher.setOutAnimation(this, R.anim.slide_right_1);
         switcher.setInAnimation(this, R.anim.slide_right_2);
         switcher.showPrevious();
      }
   }

   boolean isContentView()
   {
      return switcher.getDisplayedChild() == 1;
   }

   boolean isWordlistView()
   {
      return switcher.getDisplayedChild() == 0;
   }

   boolean hasPrevious()
   {
      return currentWord > 0;
   }

   boolean hasNext()
   {
      return currentWord + 1 < dicts[currentDict].wordCount;
   }

   void displayPrevious()
   {
      if(hasPrevious())
      {
         displayContent(currentWord - 1);
      }
   }

   void displayNext()
   {
      if(hasNext())
      {
         displayContent(currentWord + 1);
      }
   }

   void showAbout()
   {
      String versionName = "0";
      String versionCode = "0";
      try
      {
         versionName = getPackageManager().getPackageInfo(getPackageName(),
                                                          0).versionName;
         versionCode =
                     Integer.toString(getPackageManager().getPackageInfo(getPackageName(),
                                                                         0).versionCode);
      }
      catch(Exception e)
      {}
      new Builder(this).setIcon(R.drawable.icon)
                       .setTitle(R.string.app_name)
                       .setMessage("Version : $versionName\nBuild : $versionCode\n\nCopyright 2009 Hongbo\nAll rights reserved\n\nhttp://www.wordmate.net\nhongbo@wordmate.net\n".replace("$versionName",
                                                                                                                                                                                          versionName)
                                                                                                                                                                                 .replace("$versionCode",
                                                                                                                                                                                          versionCode))
                       .setPositiveButton("Web", oclPositiveButton)
                       .setNeutralButton("Email", oclNeutralButton)
                       .setNegativeButton(android.R.string.cancel, null)
                       .show();
   }

   void showDictInfo()
   {
      new Builder(this).setIcon(android.R.drawable.ic_dialog_info)
                       .setTitle(dicts[currentDict].title)
                       .setMessage(Html.fromHtml(String.valueOf(dicts[currentDict].info) + "<br>"))
                       .setNeutralButton(android.R.string.cancel, null)
                       .show();
   }

   void showError(String message)
   {
      showAlert(R.string.error, message);
   }

   void showAlert(int title, String message)
   {
      showDialog(android.R.drawable.ic_dialog_alert, title, message);
   }

   void showAlert(String title, int message)
   {
      showDialog(android.R.drawable.ic_dialog_alert, title, message);
   }

//   void showAlert(int name, int message)
//   {
//      showDialog(android.R.drawable.ic_dialog_alert, name, message);
//   }
//
//   void showAlert(String name, String message)
//   {
//      showDialog(android.R.drawable.ic_dialog_alert, name, message);
//   }

   void showDialog(int icon, int title, String message)
   {
      showDialog(icon, getString(title), message);
   }

   void showDialog(int icon, String title, int message)
   {
      showDialog(icon, title, getString(message));
   }

//   void showDialog(int icon, int name, int message)
//   {
//      showDialog(icon, getString(name), getString(message));
//   }
//
   void showDialog(int icon, String title, String message)
   {
      new Builder(this).setIcon(icon)
                       .setTitle(title)
                       .setMessage(String.valueOf(message) + "\n")
                       .setNeutralButton(android.R.string.cancel, null)
                       .show();
   }

//   void showToast(int id)
//   {
//      Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
//   }
//
//   void showToast(String s)
//   {
//      Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
//   }

   int getPref(String key, int defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getInt(key, defValue);
   }

   boolean getPref(String key, boolean defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getBoolean(key, defValue);
   }

   String getPref(String key, String defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getString(key, defValue);
   }

   void putPref(String key, int value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit().putInt(key, value).commit();
   }

   void putPref(String key, boolean value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit().putBoolean(key, value).commit();
   }

   void putPref(String key, String value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit().putString(key, value).commit();
   }

   public boolean onSearchRequested()
   {
      if(dicts.length == 0)
      {
         return super.onSearchRequested();
      }
      if(this.inputField.isFocused())
      {
         displayContent();
      }
      else
      {
         inputField.requestFocus();
         inputField.selectAll();
      }
      return super.onSearchRequested();
   }

   public boolean dispatchKeyEvent(KeyEvent event)
   {
      if(dicts.length == 0)
      {
         return super.dispatchKeyEvent(event);
      }
      if(event.getKeyCode() != 66 || !inputField.isFocused())
      {
         return super.dispatchKeyEvent(event);
      }
      if(isWordlistView())
      {
         inputField.selectAll();
         displayContent();
      }
      return true;
   }

   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      if(dicts.length == 0)
      {
         return super.onKeyDown(keyCode, event);
      }
      if(keyCode != 4 || !isContentView() || !enableWordlist)
      {
         return super.onKeyDown(keyCode, event);
      }
      displayWordlist();
      return true;
   }

   public boolean onCreateOptionsMenu(Menu menu)
   {
      menu.add(0, 1, 0, R.string.about)
          .setIcon(R.drawable.icon)
          .setAlphabeticShortcut('a');
      menu.add(0, 2, 0, R.string.settings)
          .setIcon(android.R.drawable.ic_menu_preferences)
          .setAlphabeticShortcut('s');
      menu.add(0, 3, 0, R.string.downloader)
          .setIcon(android.R.drawable.ic_menu_save)
          .setAlphabeticShortcut('d');
      menu.add(1, 4, 0, R.string.dict_info)
          .setIcon(android.R.drawable.ic_menu_info_details)
          .setAlphabeticShortcut('i');
      menu.add(2, 5, 0, R.string.prev)
          .setIcon(android.R.drawable.ic_media_rew)
          .setAlphabeticShortcut('n');
      menu.add(2, 6, 0, R.string.next)
          .setIcon(android.R.drawable.ic_media_ff)
          .setAlphabeticShortcut('m');
      return super.onCreateOptionsMenu(menu);
   }

   public boolean onPrepareOptionsMenu(Menu menu)
   {
      if(this.dicts.length == 0)
      {
         menu.setGroupVisible(1, false);
         menu.setGroupVisible(2, false);
      }
      else
      {
         menu.setGroupVisible(1, true);
         if(isWordlistView())
         {
            menu.setGroupVisible(2, false);
         }
         else
         {
            menu.setGroupVisible(2, true);
            if(hasPrevious())
            {
               menu.findItem(5).setEnabled(true);
            }
            else
            {
               menu.findItem(5).setEnabled(false);
            }
            if(hasNext())
            {
               menu.findItem(6).setEnabled(true);
            }
            else
            {
               menu.findItem(6).setEnabled(false);
            }
         }
      }
      return super.onPrepareOptionsMenu(menu);
   }

   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId())
      {
         case 1:
            showAbout();
         break;
         case 2:
            startActivity(new Intent(this, Settings.class));
         break;
         case 3:
            startActivity(new Intent(this, Downloader.class));
         break;
         case 4:
            if(dicts.length > 0)
            {
               showDictInfo();
               break;
            }
         break;
         case 5:
            if(dicts.length > 0)
            {
               displayPrevious();
               break;
            }
         break;
         case 6:
            if(dicts.length > 0)
            {
               displayNext();
               break;
            }
         break;
      }
      return super.onOptionsItemSelected(item);
   }
}
