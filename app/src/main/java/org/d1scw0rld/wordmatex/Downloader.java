package org.d1scw0rld.wordmatex;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

//import com.dropbox.core.DbxException;
//import com.dropbox.core.DbxRequestConfig;
//import com.dropbox.core.http.OkHttp3Requestor;
//import com.dropbox.core.v2.DbxClientV2;
//import com.dropbox.core.v2.files.ListFolderResult;
//import com.dropbox.core.v2.files.Metadata;
//import com.dropbox.core.v2.users.FullAccount;

public class Downloader extends AppCompatActivity
{
//   final static String ACCESS_TOKEN = "taEhH2ktguAAAAAAAAABtpz3e_ZHCYP3rZUU7otLbyQZ7OLAWqunkSQpXpw6xi2a";

   final static String URL_DICTS_LIST = "https://www.dropbox.com/s/99p4p71uhf5f0n0/dicts.json?dl=1";

//   private ArrayList<Metadata> alMetadata = new ArrayList<>();

   private ArrayList<DictInfo> alDictsInfo = new ArrayList<>();

//   private DictionariesAdapter adDictionaries;

   private DictsInfoAdapter adDictsInfo;

   private Toolbar toolbar;

   private MenuItem searchMenuItem;

   private SearchView searchView;

   private CoordinatorLayout oCoordinatorLayout;

//   private DropBoxTask.Callback callback;

   private GetDictsInfoTask.Callback getDictsInfoCallback;

//   private ProgressBar oProgressBar;

   private SwipeRefreshLayout swipeContainer;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.downloader);

      oCoordinatorLayout = findViewById(R.id.coordinator_layout);

      toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      assert getSupportActionBar() != null;
      getSupportActionBar().setDisplayShowTitleEnabled(false);


//      ActionBar actionBar = getSupportActionBar();
//      if (actionBar != null) {
//         actionBar.hide();
//      }
//
//      SearchView searchView = findViewById(R.id.search);
//      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
//      {
//         @Override
//         public boolean onQueryTextSubmit(String query)
//         {
//            return false;
//         }
//
//         @Override
//         public boolean onQueryTextChange(String newText)
//         {
////            adDictionaries.filter(newText);
//            adDictsInfo.filter(newText);
//
//            return true;
//         }
//      });
//      oProgressBar = findViewById(R.id.progress_bar);

      swipeContainer = findViewById(R.id.swipe_container);
      // Setup refresh listener which triggers new data loading
      swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
      {
         @Override
         public void onRefresh()
         {
            // Your code to refresh the list here.
            // Make sure you call swipeContainer.setRefreshing(false)
            // once the network request has completed successfully.
//            new DropBoxTask(callback).execute();
            new GetDictsInfoTask(getDictsInfoCallback).execute();
         }
      });
      swipeContainer.setColorSchemeResources(R.color.accent);

//      adDictionaries = new DictionariesAdapter();
//      adDictionaries.setOnItemClickListener(new OnItemClickListener()
//      {
//         @Override
//         public void OnItemClick(View view, int pos)
//         {
//            view(pos);
//         }
//      });

      adDictsInfo = new DictsInfoAdapter();
      adDictsInfo.setOnItemClickListener(new OnItemClickListener()
      {
         @Override
         public void OnItemClick(View view, int pos)
         {
            view(pos);
         }
      });

      RecyclerView rvDictionaries = findViewById(R.id.rv_dictionaries);
      rvDictionaries.setItemAnimator(new DefaultItemAnimator());
      rvDictionaries.setLayoutManager(new LinearLayoutManager(this));
      rvDictionaries.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
//      rvDictionaries.setAdapter(adDictionaries);
      rvDictionaries.setAdapter(adDictsInfo);

//      callback = new DropBoxTask.Callback()
//      {
//         @Override
//         public void onError(Exception e)
//         {
////            oProgressBar.setVisibility(View.GONE);
//
//            e.printStackTrace();
//            Snackbar snackbar = Snackbar
//                  .make(oCoordinatorLayout, R.string.connection_error, Snackbar.LENGTH_LONG)
//                  .setAction(R.string.retry, new View.OnClickListener()
//                  {
//                     @Override
//                     public void onClick(View view)
//                     {
////                        new DropBoxTask(Downloader.this, callback).execute();
//                        new DropBoxTask(callback).execute();
//                     }
//                  });
//
//            snackbar.show();
//         }
//
//         @Override
//         public void onFinish(ArrayList<Metadata> alMetadata)
//         {
////            oProgressBar.setVisibility(View.GONE);
//            swipeContainer.setRefreshing(false);
//
//            Downloader.this.alMetadata = alMetadata;
////            adDictionaries.notifyDataSetChanged();
//            adDictionaries.notifyAdapterDataSetChanged();
//         }
//      };

      getDictsInfoCallback = new GetDictsInfoTask.Callback()
      {
         @Override
         public void onError(Exception e)
         {
            e.printStackTrace();
            Snackbar snackbar = Snackbar
                  .make(oCoordinatorLayout, R.string.connection_error, Snackbar.LENGTH_LONG)
                  .setAction(R.string.retry, new View.OnClickListener()
                  {
                     @Override
                     public void onClick(View view)
                     {
                        new GetDictsInfoTask(getDictsInfoCallback).execute();
                     }
                  });
            snackbar.show();
         }

         @Override
         public void onFinish(ArrayList<DictInfo> alDictsInfo)
         {
            swipeContainer.setRefreshing(false);

            Downloader.this.alDictsInfo = alDictsInfo;
            adDictsInfo.notifyAdapterDataSetChanged();
         }
      };

//      new DropBoxTask(callback).execute();
      new GetDictsInfoTask(getDictsInfoCallback).execute();
      swipeContainer.setRefreshing(true);

   }

   //   private static class DropBoxTask extends AsyncTask<Void, String, ArrayList<Metadata>>
//   {
//      DbxRequestConfig requestConfig;
//      DbxClientV2      dbxClient;
////      ProgressDialog   progressDialog;
//
////      private Context context;
//
////      private WeakReference<Downloader> appReference;
//
//      private Callback callback;
//
//      public interface Callback
//      {
//         void onError(Exception e);
//
//         void onFinish(ArrayList<Metadata> alMetadata);
//      }
//
//      //      DropBoxTask(Downloader activity, Callback callback)
//      DropBoxTask(Callback callback)
//      {
////         this.context = context;
////         this.appReference = new WeakReference<>(activity);
//         this.callback = callback;
//      }
//
//      @Override
//      protected void onPreExecute()
//      {
////         progressDialog = new ProgressDialog(appReference.get());
////         progressDialog.setCancelable(false);
////         progressDialog.setMessage(appReference.get().getString(R.string.loading));
////         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
////         progressDialog.show();
//
//         super.onPreExecute();
//         requestConfig = DbxRequestConfig.newBuilder("word-mate-x")
//                                         .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
//                                         .build();
//      }
//
//      @Override
//      protected ArrayList<Metadata> doInBackground(Void... voids)
//      {
//         ArrayList<Metadata> alMetadata = new ArrayList<>();
////         alMetadata.clear();
//
//         dbxClient = new DbxClientV2(requestConfig, ACCESS_TOKEN);
//         try
//         {
//            FullAccount account = dbxClient.users()
//                                           .getCurrentAccount();
////            tv.setText(account.getName()
////                              .getDisplayName());
//            publishProgress(account.getName()
//                                   .getDisplayName());
//
//            ListFolderResult result = dbxClient.files()
//                                               .listFolder("");
//            while(true)
//            {
//               //                  tv.append("\n" + metadata.getPathLower());
//               //                  publishProgress("\n" + metadata.getPathLower());
//               alMetadata.addAll(result.getEntries());
//
////               dbxClient.files().
////               result.getEntries()
////                     .get(0).
//
//               if(!result.getHasMore())
//               {
//                  break;
//               }
//
//               result = dbxClient.files()
//                                 .listFolderContinue(result.getCursor());
//            }
//
//         }
//         catch(DbxException e)
//         {
//            callback.onError(e);
////            e.printStackTrace();
////            Snackbar snackbar = Snackbar
////                  .make(oCoordinatorLayout, R.string.connection_error, Snackbar.LENGTH_LONG)
////                  .setAction(R.string.retry, new View.OnClickListener()
////                  {
////                     @Override
////                     public void onClick(View view)
////                     {
////                        Snackbar snackbar1 = Snackbar.make(oCoordinatorLayout, "Message is restored!", Snackbar.LENGTH_SHORT);
////                        snackbar1.show();
////                     }
////                  });
////
////            snackbar.show();
//            return null;
//         }
//         return alMetadata;
//      }
//
//      @Override
//      protected void onPostExecute(ArrayList<Metadata> metadata)
//      {
////         progressDialog.cancel();
////         adDictionaries.notifyDataSetChanged();
//         callback.onFinish(metadata);
//      }
//   }
//
   private static class GetDictsInfoTask extends AsyncTask<Void, String, ArrayList<DictInfo>>
   {
      //      private ArrayList<DictInfo> alDictInfos = null;
      private ArrayList<DictInfo> alDictInfos = new ArrayList<>();

      private Callback callback;

      public interface Callback
      {
         void onError(Exception e);

         void onFinish(ArrayList<DictInfo> alDictsInfo);
      }

      GetDictsInfoTask(Callback callback)
      {
         this.callback = callback;
      }

      @Override
      protected ArrayList<DictInfo> doInBackground(Void... voids)
      {
         InputStream inputStream = null;
         try
         {
            URLConnection connection = new URL(URL_DICTS_LIST).openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            inputStream = connection.getInputStream();

            // convert inputstream to string
            if(inputStream != null)
            {
               BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
               String line;
               StringBuilder result = new StringBuilder();
               while((line = bufferedReader.readLine()) != null)
               {
                  result.append(line);
               }

               JSONObject joRoot = new JSONObject(result.toString());

               JSONObject joTemp;

//               alDictInfos = new ArrayList<>();

               JSONArray jaDicts = joRoot.getJSONArray("dicts");
               for(int i = 0; i < jaDicts.length(); i++)
               {
                  joTemp = jaDicts.getJSONObject(i);
                  alDictInfos.add(new DictInfo(joTemp));
               }
            }
         }
         catch(JSONException e)
         {
            e.printStackTrace();
         }

         catch(Exception e)
         {
//            Log.d("InputStream", e.getLocalizedMessage());
            callback.onError(e);
         }
         finally
         {
            try
            {
               if(inputStream != null)
               {
                  inputStream.close();
               }
            }
            catch(IOException e)
            {
               e.printStackTrace();
            }
         }

         return alDictInfos;
      }

      @Override
      protected void onPostExecute(ArrayList<DictInfo> alDictsInfo)
      {
         callback.onFinish(alDictsInfo);
      }
   }


//   class DictionariesAdapter extends RecyclerView.Adapter<DictionariesAdapter.ViewHolder>
//   {
//      private String sFilter = "";
//
//      private ArrayList<Metadata> visibleItems;
//
//      private OnItemClickListener onItemClickListener = null;
//
//      DictionariesAdapter()
//      {
//         visibleItems = new ArrayList<>();
//         visibleItems.addAll(alMetadata);
//      }
//
//      void setOnItemClickListener(OnItemClickListener onItemClickListener)
//      {
//         this.onItemClickListener = onItemClickListener;
//      }
//
//
//      @NonNull
//      @Override
//      public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
//      {
//         View v = LayoutInflater.from(parent.getContext())
////                                .inflate(R.layout.downloader_list_item, parent, false);
//                                .inflate(android.R.layout.simple_list_item_1, parent, false);
//
//         return new ViewHolder(v);
//      }
//
//      @Override
//      public void onBindViewHolder(@NonNull ViewHolder holder, int position)
//      {
//         Metadata metadata = visibleItems.get(position);
//
//         String sText = metadata.getName()
//                                .substring(0, metadata.getName()
//                                                      .indexOf(".dwm"));
//         Spannable spContent = new SpannableString(sText);
//         int iFilteredStart = sText.toLowerCase(Locale.getDefault())
//                                   .indexOf(sFilter);
//         int iFilterEnd;
//         if(iFilteredStart < 0)
//         {
//            iFilteredStart = 0;
//            iFilterEnd = 0;
//         }
//         else
//         {
//            iFilterEnd = iFilteredStart + sFilter.length();
//         }
//         spContent.setSpan(new ForegroundColorSpan(ContextCompat.getColor(Downloader.this, R.color.accent)),
//                           iFilteredStart,
//                           iFilterEnd,
//                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//         holder.tvTitle.setText(spContent);
//
////         Metadata metadata = alMetadata.get(position);
////         holder.tvTitle.setText(metadata.getName()
////                                        .substring(0,
////                                                   metadata.getName()
////                                                           .indexOf(".dwm")));
//
////         if(metadata instanceof FileMetadata)
////         {
////            holder.tvSize.setText(String.format(getString(R.string.format_size), ((FileMetadata) metadata).getSize() / 1000));
////         }
////         else
////         {
////            holder.tvSize.setText(null);
////         }
//
//      }
//
//      @Override
//      public int getItemCount()
//      {
////         return alMetadata.size();
//         return visibleItems.size();
//      }
//
//      public void notifyAdapterDataSetChanged()
//      {
//         visibleItems.clear();
//         visibleItems.addAll(alMetadata);
//         notifyDataSetChanged();
//      }
//
//      class ViewHolder extends RecyclerView.ViewHolder
//      {
//         TextView tvTitle;
//         //               tvSize;
//         View     view;
//
//         ViewHolder(View itemView)
//         {
//            super(itemView);
//            tvTitle = itemView.findViewById(android.R.id.text1);
////            tvTitle = itemView.findViewById(R.id.tv_title);
////            tvSize = itemView.findViewById(R.id.tv_size);
//            view = itemView;
//            view.setOnClickListener(new View.OnClickListener()
//            {
//               @Override
//               public void onClick(View view)
//               {
//                  onItemClickListener.OnItemClick(view, getLayoutPosition());
//               }
//            });
//         }
//      }
//
//
//      public void filter(String charText)
//      {
//         sFilter = charText.toLowerCase(Locale.getDefault());
//         visibleItems.clear();
//         if(sFilter.length() == 0)
//         {
//            visibleItems.addAll(alMetadata);
//         }
//         else
//         {
//            for(Metadata metadata : alMetadata)
//            {
//               if(metadata.getName()
//                          .toLowerCase(Locale.getDefault())
//                          .contains(sFilter))
//               {
//                  visibleItems.add(metadata);
//               }
//            }
//         }
//         notifyDataSetChanged();
//      }
//   }

   class DictsInfoAdapter extends RecyclerView.Adapter<DictsInfoAdapter.ViewHolder>
   {
      private String sFilter = "";

      private ArrayList<DictInfo> visibleItems;

      private OnItemClickListener onItemClickListener = null;

      DictsInfoAdapter()
      {
         visibleItems = new ArrayList<>();
         visibleItems.addAll(alDictsInfo);
      }

      void setOnItemClickListener(OnItemClickListener onItemClickListener)
      {
         this.onItemClickListener = onItemClickListener;
      }

      @NonNull
      @Override
      public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
      {
         View v = LayoutInflater.from(parent.getContext())
//                                .inflate(R.layout.downloader_list_item, parent, false);
                                .inflate(android.R.layout.simple_list_item_1, parent, false);

         return new ViewHolder(v);
      }

      @Override
      public void onBindViewHolder(@NonNull ViewHolder holder, int position)
      {
         DictInfo oDictInfo = visibleItems.get(position);

         String sText = oDictInfo.getName();

         Spannable spContent = new SpannableString(sText);
         int iFilteredStart = sText.toLowerCase(Locale.getDefault())
                                   .indexOf(sFilter);
         int iFilterEnd;
         if(iFilteredStart < 0)
         {
            iFilteredStart = 0;
            iFilterEnd = 0;
         }
         else
         {
            iFilterEnd = iFilteredStart + sFilter.length();
         }
         spContent.setSpan(new ForegroundColorSpan(ContextCompat.getColor(Downloader.this, R.color.accent)),
                           iFilteredStart,
                           iFilterEnd,
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
         holder.tvTitle.setText(spContent);
      }

      @Override
      public int getItemCount()
      {
//         return alMetadata.size();
         return visibleItems.size();
      }

      public void notifyAdapterDataSetChanged()
      {
         visibleItems.clear();
         visibleItems.addAll(alDictsInfo);
         notifyDataSetChanged();
      }

      class ViewHolder extends RecyclerView.ViewHolder
      {
         TextView tvTitle;
         //               tvSize;
         View view;

         ViewHolder(View itemView)
         {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
//            tvTitle = itemView.findViewById(R.id.tv_title);
//            tvSize = itemView.findViewById(R.id.tv_size);
            view = itemView;
            view.setOnClickListener(new View.OnClickListener()
            {
               @Override
               public void onClick(View view)
               {
                  onItemClickListener.OnItemClick(view, getLayoutPosition());
               }
            });
         }
      }

      public void filter(String charText)
      {
         sFilter = charText.toLowerCase(Locale.getDefault());
         visibleItems.clear();
         if(sFilter.length() == 0)
         {
            visibleItems.addAll(alDictsInfo);
         }
         else
         {
            for(DictInfo dictInfo : alDictsInfo)
            {
               if(dictInfo.getName()
                          .toLowerCase(Locale.getDefault())
                          .contains(sFilter))
               {
                  visibleItems.add(dictInfo);
               }
            }
         }
         notifyDataSetChanged();
      }
   }


   interface OnItemClickListener
   {
      void OnItemClick(View view, int pos);
   }

   void view(int position)
   {
//      if(position < alMetadata.size())
//      {
//         Metadata metadata = alMetadata.get(position);
//         Intent i = new Intent(this, DownloadService.class);
//         i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_VIEW);
//         i.putExtra(DownloadService.XTR_NAME, metadata.getName());
////         i.putExtra(DownloadService.XTR_FILE, metadata.getPathLower());
//         i.putExtra(DownloadService.XTR_FILE, metadata.getPathDisplay());
//
//         if(metadata instanceof FileMetadata)
//         {
//            i.putExtra(DownloadService.XTR_SIZE, ((FileMetadata) metadata).getSize());
//            i.putExtra(DownloadService.XTR_ID,
//                       ((FileMetadata) metadata).getId()
//                                                .hashCode());
//            i.putExtra(DownloadService.XTR_DATE,
//                       ((FileMetadata) metadata).getClientModified()
//                                                .getTime());
//         }
//         startService(i);
//      }

      if(position < alDictsInfo.size())
      {
         DictInfo dictInfo = alDictsInfo.get(position);

         Intent i = new Intent(this, DownloadService.class);
         i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_VIEW);
         i.putExtra(DownloadService.XTR_DICT_INFO, dictInfo);
         startService(i);
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.search_menu, menu);

      SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
      searchMenuItem = menu.findItem(R.id.search);
      searchView = (SearchView) searchMenuItem.getActionView();

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

                   ActionMenuView.LayoutParams params =
                         new ActionMenuView.LayoutParams(ActionMenuView.LayoutParams.WRAP_CONTENT, ActionMenuView.LayoutParams.MATCH_PARENT);
                   params.width = toolbar.getWidth();

                   searchView.setIconifiedByDefault(false);
                   searchMenuItem.expandActionView();
                   searchView.setOnCloseListener(new SearchView.OnCloseListener()
                   {
                      @Override
                      public boolean onClose()
                      {
                         return true;
                      }
                   });
                   searchView.setLayoutParams(params);
                }
             });

      searchView.setIconifiedByDefault(false);
      searchMenuItem.expandActionView();
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
//            adDictionaries.filter(newText);
            adDictsInfo.filter(newText);

            return true;
         }
      });


      assert searchManager != null;
      searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

      return true;
   }

}

