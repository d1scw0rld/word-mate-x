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
      titleView = (TextView) findViewById(R.id.tv_name);
      userView = (TextView) findViewById(R.id.user);
      sizeView = (TextView) findViewById(R.id.size);
      downloadsView = (TextView) findViewById(R.id.downloads);
      infoView = (TextView) findViewById(R.id.tv_info);
      button = (Button) findViewById(R.id.button);
   }

   protected void onStart()
   {
      super.onStart();
      Intent i = getIntent();
      id = i.getIntExtra(DownloadService.XTR_ID, 0);
      size = i.getIntExtra(DownloadService.XTR_SIZE, 0);
      downloads = i.getIntExtra(DownloadService.XTR_DOWNLOADS, 0);
      title = i.getStringExtra(DownloadService.XTR_TITLE);
      info = i.getStringExtra(DownloadService.XTR_INFO);
      user = i.getStringExtra(DownloadService.XTR_USER);
      file = i.getStringExtra(DownloadService.XTR_FILE);
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
      i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_DOWNLOAD);
      i.putExtra(DownloadService.XTR_ID, id);
      i.putExtra(DownloadService.XTR_SIZE, size);
      i.putExtra(DownloadService.XTR_DOWNLOADS, downloads);
      i.putExtra(DownloadService.XTR_TITLE, title);
      i.putExtra(DownloadService.XTR_INFO, info);
      i.putExtra(DownloadService.XTR_USER, user);
      i.putExtra(DownloadService.XTR_FILE, file);
      startService(i);
      finish();
   }

   void cancel()
   {
      Intent i = new Intent(this, DownloadService.class);
      i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_CANCEL);
      i.putExtra(DownloadService.XTR_ID, id);
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
