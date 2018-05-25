package org.d1scw0rld.wordmatex;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;

public class DownloadViewerNew extends AppCompatActivity
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
//      boolean b= requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
//      getSupportActionBar().hide(); //<< this
      setContentView(R.layout.download_viewer_new);

//      setTitle(null);

//      int textViewId = getResources().getIdentifier("android:id/ac", null, null);
//      TextView tv = (TextView) d.findViewById(textViewId);
//      tv.setTextColor(getResources().getColor(R.color.my_color));
//      getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.drawable.title_bar));
//      assert getSupportActionBar() != null;
//      getSupportActionBar().setDisplayShowTitleEnabled(false);

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
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      String sDateModified = String.format(getString(R.string.format_date),
                                           calendar.get(Calendar.DAY_OF_MONTH),
                                           calendar.get(Calendar.MONTH),
                                           calendar.get(Calendar.YEAR),
                                           calendar.get(Calendar.HOUR_OF_DAY),
                                           calendar.get(Calendar.MINUTE));
      tvName.setText(name);
      tvSize.setText(String.format(getString(R.string.format_size), size / 1000));
//      tvDate.setText(Html.fromHtml(date.toString())); // TODO Format it correctly
      tvDate.setText(sDateModified); // TODO Format it correctly

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
            else if(new File(WordMateX.FILES_PATH + DownloadViewerNew.this.file.substring(1)).exists())
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
      Intent i = new Intent(this, DownloadServiceNew.class);
      i.putExtra(DownloadServiceNew.XTR_ACTION, DownloadServiceNew.ACT_CANCEL);
      i.putExtra(DownloadServiceNew.XTR_ID, id);
      startService(i);
      finish();
   }
}
