package org.d1scw0rld.wordmatex;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.TreeMap;

import static java.lang.Integer.valueOf;

public class DownloadService extends Service
{
   final static int ACT_DOWNLOAD = 1,
         ACT_CANCEL              = 2,
         ACT_VIEW                = 3;

   final static String XTR_NAME = "name",
         XTR_DATE               = "date",
         XTR_SIZE               = "size",
         XTR_ID                 = "id",
         XTR_ACTION             = "action",
         XTR_FILE               = "file",
         XTR_IS_DOWNLOADING     = "is_downloading";

   final static String TAG = "DOWNLOAD_SERVICE";

   private final static String NTF_CHN = "dict_download_channel";


   private NotificationManager nm;

   private TreeMap<Integer, Task> tasks;

   private DbxClientV2 dbxClient;

   static class IncomingHandler extends Handler
   {
      private final WeakReference<DownloadService> mService;

      IncomingHandler(DownloadService service)
      {
         mService = new WeakReference<>(service);
      }

      @Override
      public void handleMessage(Message msg)
      {
         DownloadService service = mService.get();
         if(service != null)
         {
            Log.i(TAG, "msg " + msg.what);
            service.done(msg.what);
         }
      }
   }

   IncomingHandler handler;

//   Handler handler = new Handler()
//   {
//
//      @Override
//      public void handleMessage(Message msg)
//      {
//         done(msg.what);
//      }
//   };

   public void onCreate()
   {
      Log.i(TAG, "Service created");
      tasks = new TreeMap<>();
      nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("word-mate-x")
                                                       .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                                                       .build();
      dbxClient = new DbxClientV2(requestConfig, Downloader.ACCESS_TOKEN);

      handler = new IncomingHandler(this);
   }

   public IBinder onBind(Intent intent)
   {
      return null;
   }

   public void onStart(Intent intent, int startId)
   {
      Log.i(TAG, "Service start");

      Task t;

      int id = intent.getIntExtra(XTR_ID, 0);
      switch(intent.getIntExtra(XTR_ACTION, 0))
      {
         case ACT_DOWNLOAD /* 1 */:
            if(tasks.get(valueOf(id)) == null)
            {
               t = new Task();
               t.id = id;
               t.size = intent.getLongExtra(XTR_SIZE, 0);
               t.name = intent.getStringExtra(XTR_NAME);
               t.file = intent.getStringExtra(XTR_FILE);
               t.date = new Date(intent.getLongExtra(XTR_DATE, -1));
               tasks.put(id, t);
               Log.i(TAG, "Task add " + t.name);

               showToast(getString(R.string.download) + ": " + t.name);

               new RunnerNew(t).start();
            }
            break;

         case ACT_CANCEL /* 2 */:
            t = tasks.get(valueOf(id));
            if(t != null)
            {
               t.stop = true;
               nm.cancel(id);
               tasks.remove(valueOf(id));
               Log.i(TAG, "Task cancel " + t.name);
               Log.i(TAG, "Tasks " + tasks.size());
               showToast(getString(R.string.cancel_download)
                               + ": "
                               + t.name);
            }
            break;

         case ACT_VIEW /* 3 */:

////            View alertLayout = getLayoutInflater().inflate(R.layout.download_viewer, null);
//            View alertLayout = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.download_viewer, null);
//
////            AlertDialog.Builder alert = new AlertDialog.Builder(this);
//            AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));
//            alert.setTitle("Info");
//            // this is set the view from XML inside AlertDialog
////            alert.setView(alertLayout);
//            // disallow cancel of AlertDialog on click of back button and outside touch
//            alert.setCancelable(false);
////            dialog.show();
//            AlertDialog dialog = alert.create();
//            dialog.show();

            Intent i = new Intent(this, DownloadViewer.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(XTR_ID, id);
            i.putExtra(XTR_SIZE, intent.getLongExtra(XTR_SIZE, 0));
            i.putExtra(XTR_NAME, intent.getStringExtra(XTR_NAME));
            i.putExtra(XTR_DATE, intent.getLongExtra(XTR_DATE, -1));
            i.putExtra(XTR_FILE, intent.getStringExtra(XTR_FILE));
            if(tasks.get(id) != null)
            {
               i.putExtra(XTR_IS_DOWNLOADING, true);
            }
            startActivity(i);

            break;
      }

      if(tasks.size() > 0)
      {
//         setForeground(true);
         NotificationCompat.Builder oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
         oNotificationBuilder.setContentTitle(getString(R.string.app_name));
         oNotificationBuilder.setSmallIcon(R.drawable.icon);
         Notification notification = oNotificationBuilder.build();

//         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//         {
//            notification = oNotificationBuilder.build();
//         }
//         else
//            notification = oNotificationBuilder.getNotification();

         startForeground(id, notification);

      }
      else
      {
         stopSelf();
      }
   }

   void done(int id)
   {
      if(tasks.size()>0)
         Log.i(TAG, "Remove " + id + " " + tasks.get(id).name);

      tasks.remove(id);
      Log.i(TAG, "Tasks " + tasks.size());
      if(tasks.size() == 0)
      {
         Log.i(TAG, "Service stop");
         stopSelf();
      }
   }

   class Task
   {
      boolean stop;

      int id;

      long size;

      String file,
            name;

      Date date;

      Task()
      {
      }
   }

   class RunnerNew extends Thread
   {
      Task task;


      RunnerNew(Task t)
      {
         task = t;
      }

      public void run()
      {
//         Exception e;

         Intent intent = new Intent(DownloadService.this, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
         intent.putExtra(XTR_ID, task.id);
         intent.putExtra(XTR_SIZE, task.size);
         intent.putExtra(XTR_NAME, task.name);
         intent.putExtra(XTR_FILE, task.file);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         PendingIntent pi = PendingIntent.getService(DownloadService.this,
                                                     0,
                                                     intent,
                                                     0);

         String title = getString(R.string.download) + ": " + task.name;
//         notification.setLatestEventInfo(DownloadService.this, name, "", pi);
//         Notification notification = new Notification(android.R.drawable.stat_sys_download,
//                                                      task.name,
//                                                      System.currentTimeMillis());
//         notification.flags = Notification.FLAG_ONGOING_EVENT;

         NotificationCompat.Builder oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
         oNotificationBuilder.setContentTitle(title)
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_sys_download)
                             .setTicker(task.name)
                             .setWhen(System.currentTimeMillis());
         Notification notification = oNotificationBuilder.build();
         notification.flags = Notification.FLAG_ONGOING_EVENT;

//         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//         {
//            notification = oNotificationBuilder.build();
//         }
//         else
//            notification = oNotificationBuilder.getNotification();

         nm.notify(task.id, notification);

         File path = new File(WordMateX.FILES_PATH);
         File file = new File(path, task.file);
         // Make sure the Downloads directory exists.
         if(!path.exists())
         {
            if(!path.mkdirs())
            {
               throw new RuntimeException("Unable to create directory: " + path);
            }
         }
         else if(!path.isDirectory())
         {
            throw new IllegalStateException("Download path is not a directory: " + path);
         }

         try
         {
            OutputStream outputStream = new FileOutputStream(file);

            dbxClient.files()
                     .download(task.file)
                     //                      .download(metadata.getPathLower(), metadata.getRev())
                     .download(outputStream);
            // Tell android about the file
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            DownloadService.this.sendBroadcast(intent);

            intent = new Intent(DownloadService.this, WordMateX.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            pi = PendingIntent.getActivity(DownloadService.this,
                                           0,
                                           intent,
                                           0);

            oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
            oNotificationBuilder.setContentTitle(task.name)
                                .setContentText(getString(android.R.string.ok))
                                .setContentIntent(pi)
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setWhen(System.currentTimeMillis());
            notification = oNotificationBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notification = oNotificationBuilder.build();

            nm.cancel(task.id);
            nm.notify(task.id, notification);
            handler.sendEmptyMessage(task.id);

         }
         catch(DbxException | IOException e)
         {
            e.printStackTrace();

            if(!task.stop)
            {
//               notification = new Notification(android.R.drawable.stat_notify_error,
//                                               task.name,
//                                               System.currentTimeMillis());
//               notification.flags = Notification.FLAG_AUTO_CANCEL;
               oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
               oNotificationBuilder.setContentTitle(title)
                                   .setContentText(e.getMessage())
                                   .setContentIntent(pi);
               notification = oNotificationBuilder.build();
               notification.flags = Notification.FLAG_AUTO_CANCEL;

               nm.cancel(task.id);
               nm.notify(task.id, notification);
               handler.sendEmptyMessage(task.id);
            }
         }
      }
   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT)
           .show();
   }
}
