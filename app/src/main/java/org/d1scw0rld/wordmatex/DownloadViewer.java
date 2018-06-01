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
//import java.util.Date;

public class DownloadViewer extends AppCompatActivity
{
//   private int id;

//   private long size;

//   private String file,
//         name;
//
//   private Date date;

   private DictInfo dictInfo;

   private TextView tvSize,
         tvName,
//         tvDate,
         tvInfo,
         tvWords;

   private Button button;

   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.download_viewer);

      tvName = findViewById(R.id.tv_name);
      tvInfo = findViewById(R.id.tv_info);
      tvWords = findViewById(R.id.tv_words);
      tvSize = findViewById(R.id.tv_size);
//      tvDate = findViewById(R.id.tv_date);
      button = findViewById(R.id.button);
   }

   protected void onStart()
   {
      super.onStart();
      Intent i = getIntent();
      dictInfo = i.getParcelableExtra(DownloadService.XTR_DICT_INFO);
//      id = i.getIntExtra(DownloadService.XTR_ID, 0);
//      size = i.getLongExtra(DownloadService.XTR_SIZE, 0);
//      name = i.getStringExtra(DownloadService.XTR_NAME);
//      file = i.getStringExtra(DownloadService.XTR_FILE);
//      date = new Date(i.getLongExtra(DownloadService.XTR_DATE, -1));

      tvName.setText(dictInfo.getName());
      tvInfo.setText(Html.fromHtml(dictInfo.getInfo()));
      tvWords.setText(String.valueOf(dictInfo.getWords()));
      tvSize.setText(String.format(getString(R.string.format_size), dictInfo.getSize() / 1000));
//      Calendar calendar = Calendar.getInstance();
//      calendar.setTime(date);
//      String sDateModified = String.format(getString(R.string.format_date),
//                                           calendar.get(Calendar.DAY_OF_MONTH),
//                                           calendar.get(Calendar.MONTH),
//                                           calendar.get(Calendar.YEAR),
//                                           calendar.get(Calendar.HOUR_OF_DAY),
//                                           calendar.get(Calendar.MINUTE));
//
//      tvDate.setText(sDateModified);
      if(i.getBooleanExtra(DownloadService.XTR_IS_DOWNLOADING, false))
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
            if(!Environment.getExternalStorageState()
                           .startsWith("mounted"))
            {
               new Builder(DownloadViewer.this).setIcon(android.R.drawable.ic_dialog_alert)
                                               .setTitle(R.string.downloader)
                                               .setMessage(R.string.no_sdcard)
                                               .setNeutralButton(android.R.string.cancel, null)
                                               .show();
            }
//            else if(new File(WordMateX.FILES_PATH + DownloadViewer.this.file.substring(1)).exists())
            else if(new File(WordMateX.FILES_PATH + DownloadViewer.this.dictInfo.getFile().substring(1)).exists())
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
      i.putExtra(DownloadService.XTR_DICT_INFO, dictInfo);
      i.putExtra(DownloadService.XTR_ID, dictInfo.getId());
//      i.putExtra(DownloadService.XTR_ID, id);
//      i.putExtra(DownloadService.XTR_SIZE, size);
//      i.putExtra(DownloadService.XTR_NAME, name);
//      i.putExtra(DownloadService.XTR_DATE, date);
//      i.putExtra(DownloadService.XTR_FILE, file);
      startService(i);
      finish();
   }

   void cancel()
   {
      Intent i = new Intent(this, DownloadService.class);
      i.putExtra(DownloadService.XTR_ACTION, DownloadService.ACT_CANCEL);
      i.putExtra(DownloadService.XTR_ID, dictInfo.getId());
//      i.putExtra(DownloadService.XTR_ID, id);
      startService(i);
      finish();
   }
}
