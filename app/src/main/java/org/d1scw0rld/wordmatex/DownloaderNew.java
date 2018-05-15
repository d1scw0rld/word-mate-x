package org.d1scw0rld.wordmatex;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import java.util.ArrayList;

public class DownloaderNew extends AppCompatActivity
{
   final static String ACCESS_TOKEN = "taEhH2ktguAAAAAAAAABtpz3e_ZHCYP3rZUU7otLbyQZ7OLAWqunkSQpXpw6xi2a";

   private TextView textView;

   private RecyclerView rvDictionaries;

   private ProgressDialog progressDialog;

   private ArrayList<Metadata> alMetadata = new ArrayList<>();

   private DictionariesAdapter adDictionaries;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.downloader_new);

      textView = findViewById(R.id.textView);

      adDictionaries = new DictionariesAdapter();
      adDictionaries.setOnItemClickListener(new OnItemClickListener()
      {
         @Override
         public void OnItemClick(View view, int pos)
         {
            view(pos);
         }
      });

      rvDictionaries = findViewById(R.id.rv_dictionaries);
      rvDictionaries.setItemAnimator(new DefaultItemAnimator());
      rvDictionaries.setLayoutManager(new LinearLayoutManager(this));
      rvDictionaries.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
      rvDictionaries.setAdapter(adDictionaries);
      new DropBoxTask().execute();
//      new DropBoxTask(this, new DropBoxTask.Callback()
//      {
//         @Override
//         public void onError(Exception e)
//         {
//
//         }
//
//         @Override
//         public void onFinish(ArrayList<Metadata> alMetadata)
//         {
//            DownloaderNew.this.alMetadata = alMetadata;
//            adDictionaries.notifyDataSetChanged();
//         }
//      }).execute();

   }

   private class DropBoxTask extends AsyncTask<Void, String, ArrayList<Metadata>>
   {
      DbxRequestConfig requestConfig;
      DbxClientV2 dbxClient;
      ProgressDialog progressDialog;

//      private Context context;
//
//      private Callback callback;

//      public interface Callback
//      {
//         void onError(Exception e);
//         void onFinish(ArrayList<Metadata> alMetadata);
//      }

//      public DropBoxTask(Context context, Callback callback)
//      {
//         this.context = context;
//         this.callback = callback;
//      }

      @Override
      protected void onPreExecute()
      {
         progressDialog = new ProgressDialog(DownloaderNew.this);
         progressDialog.setCancelable(false);
         progressDialog.setMessage("Wait");
         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progressDialog.show();

         super.onPreExecute();
         requestConfig = DbxRequestConfig.newBuilder("word-mate-x")
                                         .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                                         .build();
      }

      @Override
      protected ArrayList<Metadata> doInBackground(Void... voids)
      {
//         ArrayList<Metadata> alMetadata = new ArrayList<>();
         alMetadata.clear();

         dbxClient = new DbxClientV2(requestConfig, ACCESS_TOKEN);
         FullAccount account = null;
         try
         {
            account = dbxClient.users()
                               .getCurrentAccount();
//            tv.setText(account.getName()
//                              .getDisplayName());
            publishProgress(account.getName()
                                   .getDisplayName());

            ListFolderResult result = dbxClient.files()
                                               .listFolder("");
            while(true)
            {
               for(Metadata metadata : result.getEntries())
               {
//                  tv.append("\n" + metadata.getPathLower());
//                  publishProgress("\n" + metadata.getPathLower());
                  alMetadata.add(metadata);
               }

//               dbxClient.files().
//               result.getEntries()
//                     .get(0).

               if(!result.getHasMore())
               {
                  break;
               }

               result = dbxClient.files()
                                 .listFolderContinue(result.getCursor());
            }

         }
         catch(DbxException e)
         {
            e.printStackTrace();
//            callback.onError(e);
            return null;
         }
         return alMetadata;
      }

//      @Override
//      protected void onProgressUpdate(String... values)
//      {
//         super.onProgressUpdate(values);
//         tv.append("\n" + values[0]);
//      }

      @Override
      protected void onPostExecute(ArrayList<Metadata> metadata)
      {
         progressDialog.cancel();
         adDictionaries.notifyDataSetChanged();
//         callback.onFinish(metadata);
//         new DownloadFileTask(MainActivity.this, dbxClient).execute(metadata.get(0));
      }
   }

   class DictionariesAdapter extends RecyclerView.Adapter<DictionariesAdapter.ViewHolder>
   {
      private OnItemClickListener onItemClickListener = null;

//      private ArrayList<Metadata> alMetadata;

//      public DictionariesAdapter(ArrayList<Metadata> alMetadata)
//      {
//         this.alMetadata = alMetadata;
//      }

      public void setOnItemClickListener(OnItemClickListener onItemClickListener)
      {
         this.onItemClickListener = onItemClickListener;
      }


      @NonNull
      @Override
      public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
      {
         View v = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.downloader_list_item_new, parent, false);
//         .inflate(android.R.layout.simple_list_item_1, parent, false);

         return new ViewHolder(v);
      }

      @Override
      public void onBindViewHolder(@NonNull ViewHolder holder, int position)
      {
         Metadata metadata = alMetadata.get(position);
         holder.tvTitle.setText(metadata.getName());
         if(metadata instanceof FileMetadata)
         {
            holder.tvSize.setText(String.valueOf(((FileMetadata)metadata).getSize() / 1000) + "KB");
         }
         else
            holder.tvSize.setText(null);

      }

      @Override
      public int getItemCount()
      {
         return alMetadata.size();
      }

      public class ViewHolder extends RecyclerView.ViewHolder
      {
         TextView tvTitle,
                  tvSize;
         View view;

         public ViewHolder(View itemView)
         {
            super(itemView);
//            tvTitle = itemView.findViewById(android.R.id.text1);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSize = itemView.findViewById(R.id.tv_size);
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
   }

   interface OnItemClickListener
   {
      void OnItemClick(View view, int pos);
   }

   void view(int position)
   {
      if(position < alMetadata.size())
      {
         Metadata metadata = alMetadata.get(position);
         Intent i = new Intent(this, DownloadServiceNew.class);
         i.putExtra(DownloadServiceNew.XTR_ACTION, DownloadService.ACT_VIEW);
         i.putExtra(DownloadServiceNew.XTR_NAME, metadata.getName());
//         i.putExtra(DownloadServiceNew.XTR_FILE, WordMateX.FILES_PATH + metadata.getPathLower());
         i.putExtra(DownloadServiceNew.XTR_FILE, metadata.getPathLower());

         if(metadata instanceof FileMetadata)
         {
            i.putExtra(DownloadServiceNew.XTR_SIZE, ((FileMetadata) metadata).getSize());
            i.putExtra(DownloadServiceNew.XTR_ID, ((FileMetadata) metadata).getId().hashCode());
            i.putExtra(DownloadServiceNew.XTR_DATE, ((FileMetadata) metadata).getClientModified().getTime());
         }
         startService(i);
      }
   }
}

