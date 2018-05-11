package org.d1scw0rld.wordmatex;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import java.util.ArrayList;

public class DownloaderNew extends AppCompatActivity
{
   private final static String ACCESS_TOKEN = "taEhH2ktguAAAAAAAAABtpz3e_ZHCYP3rZUU7otLbyQZ7OLAWqunkSQpXpw6xi2a";

   private TextView textView;

   private RecyclerView rvDictionaries;

   private ProgressDialog progressDialog;

   int a;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.downloader_new);

      textView = findViewById(R.id.textView);
      rvDictionaries = findViewById(R.id.rv_dictionaries);


   }

   private class atDropBox extends AsyncTask<Void, String, ArrayList<Metadata>>
   {
      DbxRequestConfig requestConfig;
      DbxClientV2 dbxClient;

      @Override
      protected void onPreExecute()
      {
         progressDialog = new ProgressDialog(DownloaderNew.this);
         progressDialog.setCancelable(false);
         progressDialog.setMessage("Wait");
         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progressDialog.show();

         super.onPreExecute();
         requestConfig = DbxRequestConfig.newBuilder("box-drop-123")
                                         .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                                         .build();
      }

      @Override
      protected ArrayList<Metadata> doInBackground(Void... voids)
      {
         ArrayList<Metadata> alMetadata = new ArrayList<>();

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
//         metadata.get(0)
//         new DownloadFileTask(MainActivity.this, dbxClient).execute(metadata.get(0));
      }
   }

}
