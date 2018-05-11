package org.d1scw0rld.wordmatex;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class DownloadViewer extends Activity
{
   Button button;
   int downloads;
   TextView downloadsView;
   String file;
   int id;
   String info;
   TextView infoView;
   int size;
   TextView sizeView;
   String title;
   TextView titleView;
   String user;
   TextView userView;

   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.download_viewer);
      titleView = (TextView) findViewById(R.id.title);
      userView = (TextView) findViewById(R.id.user);
      sizeView = (TextView) findViewById(R.id.size);
      downloadsView = (TextView) findViewById(R.id.downloads);
      infoView = (TextView) findViewById(R.id.info);
      button = (Button) findViewById(R.id.button);
   }

   protected void onStart()
   {
      super.onStart();
      Intent i = getIntent();
      id = i.getIntExtra("id", 0);
      size = i.getIntExtra("size", 0);
      downloads = i.getIntExtra("downloads", 0);
      title = i.getStringExtra("title");
      info = i.getStringExtra("info");
      user = i.getStringExtra("user");
      file = i.getStringExtra("file");
      titleView.setText(this.title);
      userView.setText(this.user);
      sizeView.setText(new StringBuilder(String.valueOf(Integer.toString(size / 1000))).append("KB").toString());
      downloadsView.setText(new StringBuilder(String.valueOf(Integer.toString(downloads))).append(" ")
                                                                                          .append(getString(R.string.downloads))
                                                                                          .toString());
      infoView.setText(Html.fromHtml(this.info));
      if(i.getBooleanExtra("isDownloading", false))
      {
         button.setText(R.string.cancel_download);
         button.setOnClickListener(new OnClickListener()
         {
            @Override
            public void onClick(View v)
            {
               cancel();
            }
         });
         return;
      }
      button.setText(R.string.download);
      button.setOnClickListener(new OnClickListener()
      {
         
         @Override
         public void onClick(View v)
         {
            if(!Environment.getExternalStorageState().startsWith("mounted"))
            {
               new Builder(DownloadViewer.this).setIcon(android.R.drawable.ic_dialog_alert)
                                               .setTitle(R.string.downloader)
                                               .setMessage(R.string.no_sdcard)
                                               .setNeutralButton(android.R.string.cancel, null)
                                               .show();
            }
            else if(new File(DownloadViewer.this.file).exists())
            {
               new Builder(DownloadViewer.this).setIcon(android.R.drawable.ic_dialog_alert)
                                               .setTitle(R.string.downloader)
                                               .setMessage(R.string.overwrite)
                                               .setPositiveButton(R.string.yes,
                                               new DialogInterface.OnClickListener()
                                               {
                                                  @Override
                                                  public void onClick(DialogInterface dialog, int which)
                                                  {
                                                     download();
                                                                        
                                                  }
                                               })
                                               .setNegativeButton(R.string.no, null)
                                               .show();
            }
            else
            {
               download();
            }
            
         }
      });
   }

   protected void onStop()
   {
      super.onStop();
      finish();
   }

   void download()
   {
      Intent i = new Intent(this, DownloadService.class);
      i.putExtra("action", 1);
      i.putExtra("id", id);
      i.putExtra("size", size);
      i.putExtra("downloads", downloads);
      i.putExtra("title", title);
      i.putExtra("info", info);
      i.putExtra("user", user);
      i.putExtra("file", file);
      startService(i);
      finish();
   }

   void cancel()
   {
      Intent i = new Intent(this, DownloadService.class);
      i.putExtra("action", 2);
      i.putExtra("id", id);
      startService(i);
      finish();
   }

   void showToast(int id)
   {
      Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
   }
}
