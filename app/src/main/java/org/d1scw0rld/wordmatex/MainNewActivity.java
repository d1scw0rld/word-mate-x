package org.d1scw0rld.wordmatex;

import android.Manifest;
import android.support.v7.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;

import org.d1scw0rld.wordmatex.dictionary.Dict;

public class MainNewActivity extends AppCompatActivity implements DictLoaderNew.IWordMate
{
   private final static String PREF_DICT = "dictionary",
         PREF_WORD = "word",
         PREF_VIEW = "view",
         PREF_WORD_LIST = "word_list";


//   private final static String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
   private static String[] PERMISSIONS;
   static {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
         PERMISSIONS = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                                     Manifest.permission.WRITE_EXTERNAL_STORAGE};
   }

   private final static int PERMISSION_ALL = 1;

   private boolean enableWordlist;

   private int currentDict,
         currentWord;

   private ViewSwitcher switcher;

   private DictLoaderNew dictLoader;

   private Dict[] dicts;

   private SearchView searchView;

   private AlertDialog dictDialog;

   private RecyclerView wordlistView;

   private View messageView;

   private TextView textView,
         wordView,
         definitionView,
         tvTitle;

   private Button downloaderButton;

   private Toolbar toolbar;

//   private RecyclerView.SmoothScroller smoothScroller;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main_new);

      toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      assert getSupportActionBar() != null;
      getSupportActionBar().setDisplayShowTitleEnabled(false);
//      toolbar.setTitle(getTitle());

//      getSupportFragmentManager().popBackStack();

      wordlistView = findViewById(R.id.wordlistView);
      wordlistView.setItemAnimator(new DefaultItemAnimator());
      wordlistView.setLayoutManager(new LinearLayoutManager(this));
      wordlistView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

      switcher = findViewById(R.id.switcher);

      // Meesage view
      messageView = findViewById(R.id.messageView);
      textView = findViewById(R.id.textView);
      wordView = findViewById(R.id.wordView);
      definitionView = findViewById(R.id.definitionView);
      tvTitle = findViewById(R.id.tv_title);

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

      downloaderButton = findViewById(R.id.downloaderButton);
      downloaderButton.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            startActivity(new Intent(MainNewActivity.this, Downloader.class));
         }
      });


//      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//            && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
//      {
//         ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
////         dialog.dismiss();
//      }

//      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//            && !hasPermissions(this, PERMISSIONS))
//      {
//         ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
//      }

      dicts = new Dict[0];
      currentWord = -1;
   }

   @Override
   public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults)
   {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      if(requestCode == PERMISSION_ALL)
      {
         if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
         {
            dictLoader = new DictLoaderNew(this);
         }
         else
         {
            finish();
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
      else
      {

//         dictLoader = new DictLoaderNew(this);
      }

      switcher.setInAnimation(null);
      switcher.setOutAnimation(null);
      switcher.setDisplayedChild(getPref(PREF_VIEW, 0));
      enableWordlist = getPref(PREF_WORD_LIST, true);

      /*
       * Moved in onCreateOptionsMenu
       */

//      if(enableWordlist)
//      {
////         goButton.setVisibility(View.VISIBLE);
//         searchView.setVisibility(View.VISIBLE);
//      }
//
//      else
//      {
//         switcher.setDisplayedChild(1);
////         goButton.setVisibility(View.GONE);
//         searchView.setVisibility(View.GONE);
//         if(dicts.length > 0)
//         {
//            onInput();
//         }
//      }
//      inputField.requestFocus();
//      inputField.selectAll();
//      UpdateManager updateManager = new UpdateManager(this);
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

   //      searchView.getLocationOnScreen(location);

   //   @Override
   @Override
   public void setTitle(CharSequence title)
   {
      tvTitle.setText(title);
   }

   //   }
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_main, menu);

      final MenuItem searchItem = menu.findItem(R.id.action_search);
//      final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
//      final SearchView searchView = (SearchView) searchItem.getActionView();
      searchView = (SearchView) searchItem.getActionView();

//      searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

      SearchManager searchManager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
      assert searchManager != null;
      searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));


//      searchView.setIconifiedByDefault(false);
////      searchView.setIconified(false);
//      searchItem.expandActionView();


      toolbar.getViewTreeObserver()
             .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
             {
                @Override
                public void onGlobalLayout()
                {
                   if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                   {
                      toolbar.getViewTreeObserver()
                             .removeOnGlobalLayoutListener(this);
                   }
                   View menuItem = findViewById(R.id.action_dictionary);
                   if(menuItem != null)
                   {
                      int[] location = new int[2];
                      menuItem.getLocationOnScreen(location);
                      int x = location[0]; //x coordinate
//                      int y = location[1]; //y coordinate
//                      int w = menuItem.getWidth();

                      ActionMenuView.LayoutParams params =
                            new ActionMenuView.LayoutParams(ActionMenuView.LayoutParams.WRAP_CONTENT, ActionMenuView.LayoutParams.MATCH_PARENT);
//               Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.MATCH_PARENT);
                      params.width = x;

                     /*
                      *    I variant - icon outside the input field
                      */
                      searchView.setIconifiedByDefault(false);
                      searchItem.expandActionView();
                      searchView.setOnCloseListener(new SearchView.OnCloseListener()
                      {
                         @Override
                         public boolean onClose()
                         {
                            return true;
                         }
                      });
                      searchView.setLayoutParams(params);

                      /*
                       *   II variant - icon in the field as hint
                       */

//                      searchView.setIconified(false);
//                      searchView.requestFocus();
//                      searchView.setOnCloseListener(new SearchView.OnCloseListener()
//                      {
//                         @Override
//                         public boolean onClose()
//                         {
//                            return true;
//                         }
//                      });
//
//                      searchView.setLayoutParams(params);
//
//                      AutoCompleteTextView autoComplete = (AutoCompleteTextView) searchView.findViewById(R.id.search_src_text);
//                      Class<?> clazz = null;
//                      try
//                      {
//                         clazz = SearchView.SearchAutoComplete.class;
////                         clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");
//                         SpannableStringBuilder stopHint = new SpannableStringBuilder("  ");
//                         stopHint.append("Hint");
//                         // Add the icon as an spannable
//                         Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_24dp);
//                         Method textSizeMethod = clazz.getMethod("getTextSize");
//                         Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
//                         int textSize = (int) (rawTextSize * 1.25);
//                         searchIcon.setBounds(0, 0, textSize, textSize);
//                         stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                         // Set the new hint text
//                         Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
//                         setHintMethod.invoke(autoComplete, stopHint);
//                      }
//                      catch(Exception e)
//                      {
//                         // Set default hint
//                         searchView.setQueryHint("Search");
//                         e.printStackTrace();
//                      }
                   }
                }
             });

//      Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.MATCH_PARENT);
//      params.width=800;
//      searchView.setLayoutParams(params);
//      menu.findItem(R.id.action_search).expandActionView();

//      searchView.setIconified(false);
//      AutoCompleteTextView autoComplete = (AutoCompleteTextView) searchView.findViewById(R.id.search_src_text);
//      Class<?> clazz = null;
//      try
//      {
//         clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");
//         SpannableStringBuilder stopHint = new SpannableStringBuilder("  ");
//         stopHint.append("Hint");
//         // Add the icon as an spannable
//         Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_24dp);
//         Method textSizeMethod = clazz.getMethod("getTextSize");
//         Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
//         int textSize = (int) (rawTextSize * 1.25);
//         searchIcon.setBounds(0, 0, textSize, textSize);
//         stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//         // Set the new hint text
//         Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
//         setHintMethod.invoke(autoComplete, stopHint);
//      }
//      catch(Exception e)
//      {
//         // Set default hint
//         searchView.setQueryHint("Search");
//         e.printStackTrace();
//      }

      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
      {

         @Override
         public boolean onQueryTextSubmit(String arg0)
         {
            return false;
         }

         @Override
         public boolean onQueryTextChange(String arg0)
         {
            onInput(arg0);
            return true;
         }
      });

//      if(enableWordlist)
//      {
////         goButton.setVisibility(View.VISIBLE);
////         searchView.setVisibility(View.VISIBLE);
//      }

      dictLoader = new DictLoaderNew(this);
      if(!enableWordlist)
      {
         switcher.setDisplayedChild(1);
//         goButton.setVisibility(View.GONE);
//         searchView.setVisibility(View.GONE);
         if(dicts.length > 0)
         {
            onInput();
         }
      }
      return super.onCreateOptionsMenu(menu);
//      return true;
   }


   //   public boolean onPrepareOptionsMenu(Menu menu)
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      if(this.dicts.length == 0)
      {
         menu.setGroupVisible(R.id.group_info, false);
         menu.setGroupVisible(R.id.group_words, false);
      }
      else
      {
         menu.setGroupVisible(R.id.group_info, true);
         if(isWordlistView())
         {
            menu.setGroupVisible(R.id.group_words, false);
         }
         else
         {
            menu.setGroupVisible(R.id.group_words, true);
            if(hasPrevious())
            {
               menu.findItem(R.id.action_prev_word)
                   .setEnabled(true);
            }
            else
            {
               menu.findItem(R.id.action_prev_word)
                   .setEnabled(false);
            }
            if(hasNext())
            {
               menu.findItem(R.id.action_next_word)
                   .setEnabled(true);
            }
            else
            {
               menu.findItem(R.id.action_next_word)
                   .setEnabled(false);
            }
         }
      }
      return super.onPrepareOptionsMenu(menu);
   }

   //   @Nullable
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

   //   }
   @Override
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

   @Override
   public Context getContext()
   {
      return this;
   }
   //   View getMenuItemView(Toolbar toolbar, @IdRes int menuItemId) throws IllegalAccessException, NoSuchFieldException

   @Override
   public void showAlert(String title, int error_format)
   {
      showDialog(android.R.drawable.ic_dialog_alert, title, error_format);
   }
   //   {
   @Override
   public void showError(String message)
   {
      showAlert(R.string.error, message);
   }

   //      Field mMenuView = Toolbar.class.getDeclaredField("mMenuView");
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
               return;
            }
            textView.setText(R.string.no_sdcard);
            downloaderButton.setVisibility(View.GONE);
            return;
         }
         else
            messageView.setVisibility(View.GONE);
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

   //      mMenuView.setAccessible(true);
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
//      wordlistView.setOnItemClickListener(new AdapterView.OnItemClickListener()
//      {
//         @Override
//         public void onItemClick(AdapterView<?> adapterView,
//                                 View view,
//                                 int position,
//                                 long id)
//         {
//            setInput(((TextView) view).getText().toString());
//            displayContent(position);
//         }
//      });

      /*
       * Moved in adapter
       */
//      wordlistView.setOnItemClickListener(new WordListAdapterNew.OnItemClickListener()
//      {
//         @Override
//         public void OnItemClick(View view, int pos)
//         {
//            setInput(((TextView) view).getText().toString());
//            displayContent(pos);
//         }
//      });


//      dictButton.setOnClickListener(new View.OnClickListener()
//      {
//         @Override
//         public void onClick(View v)
//         {
//            dictDialog.show();
//         }
//      });

//      goButton.setOnClickListener(new View.OnClickListener()
//      {
//
//         @Override
//         public void onClick(View v)
//         {
//            if(WordMate.this.isWordlistView())
//            {
//               displayContent();
//            }
//            else
//            {
//               displayWordlist();
//            }
//         }
//      });
//      clearButton.setOnClickListener(new View.OnClickListener()
//      {
//         @Override
//         public void onClick(View v)
//         {
//            setInput(null);
//            onInput(null);
//         }
//      });
      setInput(getPref(PREF_WORD, null));
      setDict(getPref(PREF_DICT, 0));
//      inputField.addTextChangedListener(new InputWatcher(this));
   }
   //      Field mChildren = menuView.getClass()  //android.support.v7.internal.view.menu.ActionMenuView
   //                                .getSuperclass() //android.support.v7.widget.LinearLayoutCompat
   //                                .getSuperclass() //android.view.ViewGroup
   //                                .getDeclaredField("mChildren");
   //      mChildren.setAccessible(true);
   //      View[] children = (View[]) mChildren.get(menuView);
   //      for(View child : children)
   //      {
   //         if(child.getId() == menuItemId)
   //         {
   //            return child;
   //         }
   //      }
   //      return null;
   //   {
   //      MenuItem   searchItem = menu.findItem(R.id.action_search);
   //      SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
   ////      int        searchImgId        = android.support.v7.appcompat.R.id.search_button; // I used the explicit layout ID of searchview's ImageView
   ////      ImageView  v                  = (ImageView) mSearchView.findViewById(searchImgId);
   ////      v.setImageResource(R.drawable.search_btn);
   //      int[] location = new int[2];
   //      int x = location[0]; //x coordinate
   //      int y = location[1]; //y coordinate
   //      int w = searchView.getWidth();
   //
   //      return super.onPrepareOptionsMenu(menu);

   //      Object menuView = mMenuView.get(toolbar);

   void setDict(int n)
   {
      if(n >= dicts.length)
      {
         n = 0;
      }
      currentDict = n;
      setTitle(dicts[n].title);

      WordListAdapterNew wordListAdapterNew = new WordListAdapterNew(dicts[n], this);
      wordListAdapterNew.setOnItemClickListener(new WordListAdapterNew.OnItemClickListener()
      {
         @Override
         public void OnItemClick(View view, int pos)
         {
            setInput(((TextView) view).getText()
                                      .toString());
            displayContent(pos);
         }
      });
      wordlistView.setAdapter(wordListAdapterNew);
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

   void restart()
   {
      finish();
      startActivity(getIntent());
   }

   boolean getPref(String key, boolean defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getBoolean(key, defValue);
   }

   int getPref(String key, int defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getInt(key, defValue);
   }

   String getPref(String key, String defValue)
   {
      return getSharedPreferences("Main", MODE_PRIVATE).getString(key, defValue);
   }


   void putPref(String key, int value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit()
                                                .putInt(key, value)
                                                .apply();
   }

   void putPref(String key, String value)
   {
      getSharedPreferences("Main", MODE_PRIVATE).edit()
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
      searchView.requestFocus();

//      inputField.requestFocus();
//      inputField.setText(word);
//      inputField.selectAll();
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
         displayWordlist(word);
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
//         scroll.scrollTo(0, 0);
         wordlistView.scrollTo(0, 0);
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

   void displayWordlist()
   {
      displayWordlist(searchView.getQuery()
                                .toString());
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
//      wordlistView.getLayoutManager()
//                  .scrollToPosition(index);
      ((LinearLayoutManager) wordlistView.getLayoutManager()).scrollToPositionWithOffset(index, 0);

      /*
       *    Smooth scroll. Too slow.
       */
//      smoothScroller.setTargetPosition(index);
//      wordlistView.getLayoutManager().startSmoothScroll(smoothScroller);

//      wordlistView.scrollTo(0, index);
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


   boolean isContentView()
   {
      return switcher.getDisplayedChild() == 1;
   }

   boolean isWordlistView()
   {
      return switcher.getDisplayedChild() == 0;
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
      {
      }
      new AlertDialog.Builder(this).setIcon(R.drawable.icon)
                                   .setTitle(R.string.app_name)
                                   .setMessage(String.format(getString(R.string.about_msg),
                                                             versionName,
                                                             versionCode))
//                                         "Version : $versionName\nBuild : $versionCode\n\nCopyright 2009 Hongbo\nAll rights reserved\n\nhttp://www.wordmate.net\nhongbo@wordmate.net\n".replace(
//                                               "$versionName",
//                                               versionName)
//                                                                                                                                                                                       .replace(
//                                                                                                                                                                                             "$versionCode",
//                                                                                                                                                                                             versionCode))
                                   .setNegativeButton("Web", new DialogInterface.OnClickListener()
                                   {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i)
                                      {
                                         startActivity(new Intent("android.intent.action.VIEW",
                                                                  Uri.parse("http://www.wordmate.net")));
                                      }
                                   })
                                   .setNeutralButton("Email", new DialogInterface.OnClickListener()
                                   {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i)
                                      {
                                         startActivity(new Intent("android.intent.action.VIEW",
                                                                  Uri.parse("mailto: hongbo@wordmate.net")));
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
}
