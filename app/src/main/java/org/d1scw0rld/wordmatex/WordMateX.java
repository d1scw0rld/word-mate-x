package org.d1scw0rld.wordmatex;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.d1scw0rld.wordmatex.dictionary.Dict;

import java.io.IOException;

public class WordMateX extends AppCompatActivity implements DictLoader.IWordMate
{
   final static String FILES_PATH = Environment.getExternalStorageDirectory()
                                               .getAbsolutePath() + "/wordmate/";

   final static String PREF_FILE = "main";

   private final static String PREF_DICT = "dictionary",
         PREF_WORD                       = "word",
         PREF_VIEW                       = "view",
         PREF_WORD_LIST                  = "word_list";

   //   private final static String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
   private static String[] PERMISSIONS;

   static
   {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
         PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
      }
   }

   private final static int PERMISSION_ALL = 1;

   private boolean enableWordlist;

   private int currentDict,
         currentWord;

   private String sharedText = null;

   private ViewSwitcher switcher;

   private DictLoader dictLoader;

   private Dict[] dicts;

   private SearchView searchView = null;

   private AlertDialog dictDialog;

   private RecyclerView rvWordList;

   private View messageView;

   private TextView textView,
         wordView,
         definitionView,
         tvTitle;

   private ScrollView scroll;

   private Button downloaderButton;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      Toolbar toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      assert getSupportActionBar() != null;
      getSupportActionBar().setDisplayShowTitleEnabled(false);

      rvWordList = findViewById(R.id.rv_word_list);
      rvWordList.setItemAnimator(new DefaultItemAnimator());
      rvWordList.setLayoutManager(new LinearLayoutManager(this));
      rvWordList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

      switcher = findViewById(R.id.switcher);

      // Message ACT_VIEW
      messageView = findViewById(R.id.messageView);
      textView = findViewById(R.id.textView);
      wordView = findViewById(R.id.wordView);
      scroll = findViewById(R.id.scroll);
      /*
       *    Smooth scroll. Too slow.
       */
//      smoothScroller = new LinearSmoothScroller(this)
//      {
//         @Override protected int getVerticalSnapPreference()
//         {
//            return LinearSmoothScroller.SNAP_TO_START;
//         }
//      };
      definitionView = findViewById(R.id.definitionView);
      tvTitle = findViewById(R.id.tv_title);
      searchView = findViewById(R.id.search);
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
      {

         @Override
         public boolean onQueryTextSubmit(String query)
         {
            return false;
         }

         @Override
         public boolean onQueryTextChange(String newText)
         {
            if(newText.isEmpty())
            {
               onInput(newText);
            }
            else
            {
               if(!isWordlistView())
               {
                  displayContent(newText);
               }
               else
               {
                  displayWordList(newText);
               }
            }
            return true;
         }
      });
      searchView.setOnCloseListener(new SearchView.OnCloseListener()
      {
         @Override
         public boolean onClose()
         {
            return true;
         }
      });


      downloaderButton = findViewById(R.id.downloaderButton);
      downloaderButton.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            startActivity(new Intent(WordMateX.this, Downloader.class));
         }
      });

      dicts = new Dict[0];
      currentWord = -1;


      Intent intent = getIntent();
      String action = intent.getAction();
      String type = intent.getType();

      if(Intent.ACTION_SEND.equals(action) && type != null)
      {
         if("text/plain".equals(type))
         {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
         }
      }
   }


   @Override
   protected void onStart()
   {
      super.onStart();

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !hasPermissions(this, PERMISSIONS))
      {
         ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
      }


      switcher.setInAnimation(null);
      switcher.setOutAnimation(null);
      switcher.setDisplayedChild(getPref(PREF_VIEW, 0));
      enableWordlist = getPref(PREF_WORD_LIST, true);

      if(!enableWordlist)
      {
         invalidateOptionsMenu();
         switcher.setDisplayedChild(1);
         if(dicts.length > 0)
         {
            onInput();
         }
      }

      searchView.clearFocus();
      dictLoader = new DictLoader(this);
   }

   @Override
   public void onResume()
   {
      super.onResume();
      // This registers mMessageReceiver to receive messages.
      LocalBroadcastManager.getInstance(this)
                           .registerReceiver(mMessageReceiver,
                                             new IntentFilter("dict-added"));
   }

   @Override
   protected void onNewIntent(Intent intent)
   {
      super.onNewIntent(intent);
      String type = intent.getType();

      if(Intent.ACTION_SEND.equals(intent.getAction()) && type != null)
      {
         if("text/plain".equals(type))
         {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if(sharedText != null)
            {
               setInput(sharedText);
            }
         }
      }
   }

   @Override
   public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults)
   {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      if(requestCode == PERMISSION_ALL)
      {
         if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
         {
            dictLoader = new DictLoader(this);
         }
         else
         {
            finish();
         }
      }
   }

   @Override
   protected void onPause()
   {
      // Unregister since the activity is not visible
      LocalBroadcastManager.getInstance(this)
                           .unregisterReceiver(mMessageReceiver);
      super.onPause();
   }

   @Override
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
         putPref(PREF_DICT, currentDict);
         putPref(PREF_WORD,
                 searchView.getQuery()
                           .toString());
         putPref(PREF_VIEW, switcher.getDisplayedChild());
      }
   }

   @Override
   public void setTitle(CharSequence title)
   {
      tvTitle.setText(title);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_main, menu);

      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      if(dicts.length == 0)
      {
         menu.setGroupVisible(R.id.group_info, false);
         menu.setGroupVisible(R.id.group_words, false);
         searchView.setVisibility(View.GONE);
         menu.findItem(R.id.action_dictionary)
             .setVisible(false);

      }
      else
      {
         menu.setGroupVisible(R.id.group_info, true);
         searchView.setVisibility(View.VISIBLE);
         menu.findItem(R.id.action_dictionary)
             .setVisible(true);

         if(getRvWordList())
         {
            menu.setGroupVisible(R.id.group_words, false);
         }
         else
         {
            menu.setGroupVisible(R.id.group_words, true);
//            if(hasPrevious())
//            {
//               menu.findItem(R.id.action_prev_word)
//                   .setEnabled(true);
//            }
//            else
//            {
//               menu.findItem(R.id.action_prev_word)
//                   .setEnabled(false);
//            }
//            if(hasNext())
//            {
//               menu.findItem(R.id.action_next_word)
//                   .setEnabled(true);
//            }
//            else
//            {
//               menu.findItem(R.id.action_next_word)
//                   .setEnabled(false);
//            }
         }
      }
      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch(item.getItemId())
      {
         case R.id.action_dictionary:
            dictDialog.show();
            break;

         case R.id.action_about:
            showAbout();
            break;

         case R.id.action_settings:
            startActivity(new Intent(this, Settings.class));
            break;

         case R.id.action_dict_downloader:
            startActivity(new Intent(this, Downloader.class));
            break;

         case R.id.action_dict_info:
            if(dicts.length > 0)
            {
               showDictInfo();
            }
            break;

         case R.id.action_prev_word:
            if(dicts.length > 0)
            {
               displayPrevious();
            }
            break;

         case R.id.action_next_word:
            if(dicts.length > 0)
            {
               displayNext();
            }
            break;
      }
      return true;
   }

   @Override
   public void onBackPressed()
   {
      if(enableWordlist && isContentView())
      {
         displayWordList();
      }
      else
      {
         super.onBackPressed();
      }
   }

   @Override
   public Context getContext()
   {
      return this;
   }

   @Override
   public void showAlert(String title, int error_format)
   {
      showDialog(android.R.drawable.ic_dialog_alert, title, error_format);
   }

   @Override
   public void showError(String message)
   {
      showAlert(R.string.error, message);
   }

   @Override
   public void onLoad(Dict[] newDicts)
   {
      dictLoader = null;
      if(dicts.length == 0)
      {
         if(newDicts.length == 0)
         {
            messageView.setVisibility(View.VISIBLE);
            if(Environment.getExternalStorageState()
                          .startsWith("mounted"))
            {
               textView.setText(R.string.no_dict);
               downloaderButton.setVisibility(View.VISIBLE);
            }
            else
            {
               textView.setText(R.string.no_sdcard);
               downloaderButton.setVisibility(View.GONE);
            }
         }
         else
         {
            messageView.setVisibility(View.GONE);
            dicts = newDicts;
            init();
            if(sharedText != null)
            {
               setInput(sharedText);
               sharedText = null;
            }
            invalidateOptionsMenu();
         }
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
      dictDialog = new AlertDialog.Builder(this).setTitle(R.string.dict_dialog)
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
      setInput(getPref(PREF_WORD, null));
      setDict(getPref(PREF_DICT, 0));
   }

   void setDict(int n)
   {
      if(n >= dicts.length)
      {
         n = 0;
      }
      currentDict = n;
      setTitle(dicts[n].title);

      WordListAdapter wordListAdapterNew = new WordListAdapter(this, dicts[n]);
      wordListAdapterNew.setOnItemClickListener(new WordListAdapter.OnItemClickListener()
      {
         @Override
         public void OnItemClick(View view, int pos)
         {
            setInput(((TextView) view).getText()
                                      .toString());
            displayContent(pos);
         }
      });
      rvWordList.setAdapter(wordListAdapterNew);
      currentWord = -1;
      if(isContentView())
      {
         displayContent();
      }
      else
      {
         displayWordList();
      }
   }

   void restart()
   {
      finish();
      startActivity(getIntent());
   }

   boolean getPref(String key, boolean defValue)
   {
      return getSharedPreferences(PREF_FILE, MODE_PRIVATE).getBoolean(key, defValue);
//      return PreferenceManager.getDefaultSharedPreferences(this)
//                              .getBoolean(key, defValue);
   }

   int getPref(String key, int defValue)
   {
      return getSharedPreferences(PREF_FILE, MODE_PRIVATE).getInt(key, defValue);
   }

   String getPref(String key, String defValue)
   {
      return getSharedPreferences(PREF_FILE, MODE_PRIVATE).getString(key, defValue);
   }


   void putPref(String key, int value)
   {
      getSharedPreferences(PREF_FILE, MODE_PRIVATE).edit()
                                                   .putInt(key, value)
                                                   .apply();
   }

   void putPref(String key, String value)
   {
      getSharedPreferences(PREF_FILE, MODE_PRIVATE).edit()
                                                   .putString(key, value)
                                                   .apply();
   }

   void setInput(String word)
   {
      if(word == null)
      {
         word = "";
      }
      searchView.setQuery(word, false);
      searchView.clearFocus();
   }

   void onInput()
   {
      onInput(searchView.getQuery()
                        .toString());
   }

   void onInput(String word)
   {
      if(enableWordlist)
      {
         displayWordList(word);
      }
      else
      {
         displayContent(word);
      }
   }

   void displayContent()
   {
      displayContent(searchView.getQuery()
                               .toString());
   }

   void displayContent(int index)
   {
      showContentView();

      if(currentWord != index)
      {
         currentWord = index;
         rvWordList.scrollTo(0, 0);
         try
         {
            wordView.setText(dicts[currentDict].getWord(index));
            definitionView.setText(Html.fromHtml(dicts[currentDict].getDefinition(index)));
            scroll.scrollTo(0, 0);
         }
         catch(IOException e)
         {
            showError(e.getMessage());
         }
      }
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

   void displayWordList()
   {
      displayWordList(searchView.getQuery()
                                .toString());
   }

   void displayWordList(String word)
   {
      try
      {
         displayWordList(dicts[currentDict].query(word));
      }
      catch(IOException e)
      {
         showError(e.getMessage());
      }
   }

   void displayWordList(int index)
   {
      showWordlistView();
      ((LinearLayoutManager) rvWordList.getLayoutManager()).scrollToPositionWithOffset(index, 0);

      /*
       *    Smooth scroll. Too slow.
       */
//      smoothScroller.setTargetPosition(index);
//      rvWordList.getLayoutManager().startSmoothScroll(smoothScroller);

//      rvWordList.scrollTo(0, index);
   }

   void showContentView()
   {
      if(!isContentView())
      {
         switcher.setOutAnimation(this, R.anim.slide_left_1);
         switcher.setInAnimation(this, R.anim.slide_left_2);
         switcher.showNext();
         invalidateOptionsMenu();
      }
   }


   boolean isContentView()
   {
      return switcher.getDisplayedChild() == 1;
   }

   boolean isWordlistView()
   {
      return this.switcher.getDisplayedChild() == 0;
   }

   boolean getRvWordList()
   {
      return switcher.getDisplayedChild() == 0;
   }

   void showWordlistView()
   {
      if(!getRvWordList())
      {
         switcher.setOutAnimation(this, R.anim.slide_right_1);
         switcher.setInAnimation(this, R.anim.slide_right_2);
         switcher.showPrevious();
         invalidateOptionsMenu();
      }
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
         try
         {
            setInput(dicts[currentDict].getWord(currentWord - 1));
         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
      }
   }

   void displayNext()
   {
      if(hasNext())
      {
         try
         {
            setInput(dicts[currentDict].getWord(currentWord + 1));
         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
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
      {
         e.printStackTrace();
      }
      new AlertDialog.Builder(this).setIcon(R.drawable.icon)
                                   .setTitle(R.string.app_name)
                                   .setMessage(String.format(getString(R.string.about_msg),
                                                             versionName,
                                                             versionCode))

//                                   .setNegativeButton("Web", new DialogInterface.OnClickListener()
//                                   {
//                                      @Override
//                                      public void onClick(DialogInterface dialogInterface, int i)
//                                      {
//                                         startActivity(new Intent("android.intent.action.VIEW",
//                                                                  Uri.parse("http://www.wordmate.net")));
//                                      }
//                                   })
                                   .setNeutralButton("Email", new DialogInterface.OnClickListener()
                                   {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i)
                                      {
                                         startActivity(new Intent("android.intent.action.VIEW",
                                                                  Uri.parse("mailto: wordmatex@gmail.com")));
                                      }
                                   })
                                   .setPositiveButton(android.R.string.cancel, null)
                                   .show();
   }

   void showDictInfo()
   {
      new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info)
                                   .setTitle(dicts[currentDict].title)
                                   .setMessage(Html.fromHtml(String.valueOf(dicts[currentDict].info) + "<br>"))
                                   .setPositiveButton(android.R.string.cancel, null)
                                   .show();
   }

   void showDialog(int icon, int title, String message)
   {
      showDialog(icon, getString(title), message);
   }

   void showDialog(int icon, String title, int message)
   {
      showDialog(icon, title, getString(message));
   }

   void showDialog(int icon, String title, String message)
   {
      new AlertDialog.Builder(this).setIcon(icon)
                                   .setTitle(title)
                                   .setMessage(String.valueOf(message) + "\n")
                                   .setNeutralButton(android.R.string.cancel, null)
                                   .show();
   }

   void showAlert(int title, String message)
   {
      showDialog(android.R.drawable.ic_dialog_alert, title, message);
   }

   private static boolean hasPermissions(Context context, String... permissions)
   {
      if(context != null && permissions != null)
      {
         for(String permission : permissions)
         {
            if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
            {
               return false;
            }
         }
      }
      return true;
   }

   private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
   {
      @Override
      public void onReceive(Context context, Intent intent)
      {
         // Extract data included in the Intent
//         int yourInteger = intent.getIntExtra("message", -1/*default value*/);
         dictLoader = new DictLoader(WordMateX.this);
      }
   };

}
