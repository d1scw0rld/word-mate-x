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

import java.io.File;
import java.util.Date;

public class DownloadViewerNew extends Activity
{
   private int id;

   private long size;

   private String file,
         name;

   private Date date;

   private TextView tvSize,
         tvName,
         tvDate;

   private Button button;

   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.download_viewer_new);
      tvName = findViewById(R.id.tv_name);
      tvSize = findViewById(R.id.tv_size);
      tvDate = findViewById(R.id.tv_date);
      button = findViewById(R.id.button);
   }

   protected void onStart()
   {
      super.onStart();
      Intent i = getIntent();
      id = i.getIntExtra(DownloadServiceNew.XTR_ID, 0);
      size = i.getLongExtra(DownloadServiceNew.XTR_SIZE, 0);
      name = i.getStringExtra(DownloadServiceNew.XTR_NAME);
      file = i.getStringExtra(DownloadServiceNew.XTR_FILE);
      date = new Date(i.getLongExtra(DownloadServiceNew.XTR_DATE, -1));
      tvName.setText(name);
      tvSize.setText(String.format("%dKB", size / 1000));
      tvDate.setText(Html.fromHtml(date.toString())); // TODO Format it correctly

      if(i.getBooleanExtra(DownloadServiceNew.XTR_IS_DOWNLOADING, false))
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
               new Builder(DownloadViewerNew.this).setIcon(android.R.drawable.ic_dialog_alert)
                                                  .setTitle(R.string.downloader)
                                                  .setMessage(R.string.no_sdcard)
                                                  .setNeutralButton(android.R.string.cancel, null)
                                                  .show();
            }
            else if(new File(DownloadViewerNew.this.file).exists())
            {
               new Builder(DownloadViewerNew.this).setIcon(android.R.drawable.ic_dialog_alert)
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
      Intent i = new Intent(this, DownloadServiceNew.class);
      i.putExtra(DownloadServiceNew.XTR_ACTION, DownloadServiceNew.ACT_DOWNLOAD);
      i.putExtra(DownloadServiceNew.XTR_ID, id);
      i.putExtra(DownloadServiceNew.XTR_SIZE, size);
      i.putExtra(DownloadServiceNew.XTR_NAME, name);
      i.putExtra(DownloadServiceNew.XTR_DATE, date);
      i.putExtra(DownloadServiceNew.XTR_FILE, file);
      startService(i);
      finish();
   }

   void cancel()
   {
      Intent i = new Intent(this, DownloadService.class);
      i.putExtra(DownloadServiceNew.XTR_ACTION, DownloadService.ACT_CANCEL);
      i.putExtra(DownloadServiceNew.XTR_ID, id);
      startService(i);
      finish();
   }
}
