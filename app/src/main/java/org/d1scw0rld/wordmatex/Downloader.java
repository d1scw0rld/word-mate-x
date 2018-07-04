package org.d1scw0rld.wordmatex;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
import android.view.View;
import android.view.ViewGroup;
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

public class Downloader extends AppCompatActivity
{
   final static String URL_DICTS_LIST = "https://www.dropbox.com/s/99p4p71uhf5f0n0/dicts.json?dl=1";

   private ArrayList<DictInfo> alDictsInfo = new ArrayList<>();

   private DictsInfoAdapter adDictsInfo;

   private CoordinatorLayout oCoordinatorLayout;

   private GetDictsInfoTask.Callback getDictsInfoCallback;

   private SwipeRefreshLayout swipeContainer;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.downloader);

      oCoordinatorLayout = findViewById(R.id.coordinator_layout);

      Toolbar toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      assert getSupportActionBar() != null;
      getSupportActionBar().setDisplayShowTitleEnabled(false);

      SearchView searchView = findViewById(R.id.search);
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
            adDictsInfo.filter(newText);
            return true;
         }
      });
      searchView.clearFocus();

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
      rvDictionaries.setAdapter(adDictsInfo);

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

      new GetDictsInfoTask(getDictsInfoCallback).execute();
      swipeContainer.setRefreshing(true);

   }

   private static class GetDictsInfoTask extends AsyncTask<Void, String, ArrayList<DictInfo>>
   {
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
         View view;

         ViewHolder(View itemView)
         {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
            view = itemView;
            view.setOnClickListener(new View.OnClickListener()
            {
               @Override
               public void onClick(View view)
               {
                  for(int i = 0; i < alDictsInfo.size(); i++)
                  {
                     if(alDictsInfo.get(i).equals(visibleItems.get(getLayoutPosition())))
                     {
                        onItemClickListener.OnItemClick(view, i);
                        return;
                     }
                  }
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
      if(position < alDictsInfo.size())
      {
         DictInfo dictInfo = alDictsInfo.get(position);

         Intent i = new Intent(this, DownloadService.class);
         i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_VIEW);
         i.putExtra(DownloadService.XTR_DICT_INFO, dictInfo);
         startService(i);
      }
   }
}

