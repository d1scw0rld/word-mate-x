package org.d1scw0rld.wordmatex;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.Integer.valueOf;

public class DownloadService extends Service
{
   final static int ACT_DOWNLOAD = 1,
         ACT_CANCEL = 2,
         ACT_VIEW = 3;

   final static int BUFFER_SIZE = 65536;

   final static String XTR_ID = "id",
         XTR_ACTION = "action",
         XTR_IS_DOWNLOADING = "is_downloading",
         XTR_DICT_INFO = "dict_info";

   final static String TAG = "DOWNLOAD_SERVICE";

   private final static String NTF_CHN = "dict_download_channel";

   private TreeMap<Integer, Task> tasks;

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

   public void onCreate()
   {
      Log.i(TAG, "Service created");
      tasks = new TreeMap<>();
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
      DictInfo dictInfo;
      int id = 0;

      switch(intent.getIntExtra(XTR_ACTION, 0))
      {
         case ACT_DOWNLOAD /* 1 */:
            dictInfo = intent.getParcelableExtra(XTR_DICT_INFO);
            id = dictInfo.getId();
            if(tasks.get(valueOf(dictInfo.getId())) == null)
            {
               t = new Task(dictInfo);
               tasks.put(t.getId(), t);
               Log.i(TAG, "Task add " + t.getName());
               showToast(getString(R.string.download) + ": " + t.getName());

               new RunnerThread(t).start();
            }
            break;

         case ACT_CANCEL /* 2 */:
            id = intent.getIntExtra(XTR_ID, 0);
            t = tasks.get(valueOf(id));
            if(t != null)
            {
               t.stop = true;
               tasks.remove(valueOf(id));
               Log.i(TAG, "Task cancel " + t.getName());
               Log.i(TAG, "Tasks " + tasks.size());
               showToast(getString(R.string.cancel_download)
                               + ": "
                               + t.getName());
            }
            break;

         case ACT_VIEW /* 3 */:
            dictInfo = intent.getParcelableExtra(XTR_DICT_INFO);
            id = dictInfo.getId();

            Intent i = new Intent(this, DownloadViewer.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(XTR_DICT_INFO, dictInfo);
            if(tasks.get(dictInfo.getId()) != null)
            {
               i.putExtra(XTR_IS_DOWNLOADING, true);
            }
            startActivity(i);

            break;
      }

      if(tasks.size() > 0)
      {
         NotificationCompat.Builder oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
         oNotificationBuilder.setContentTitle(getString(R.string.app_name));
         oNotificationBuilder.setSmallIcon(R.drawable.icon);
         Notification notification = oNotificationBuilder.build();

         startForeground(id, notification);
      }
      else
      {
         stopSelf();
      }
   }

   void done(int id)
   {
      if(tasks.size() > 0)
      {
         Log.i(TAG,
               "Remove " + id + " " + tasks.get(id)
                                           .getName());
      }

      tasks.remove(id);
      Log.i(TAG, "Tasks " + tasks.size());
      if(tasks.size() == 0)
      {
         Log.i(TAG, "Service stop");
         stopSelf();
      }
   }

   class Task extends DictInfo
   {
      boolean stop;

      Task(DictInfo dictInfo)
      {
         super(dictInfo);
      }
   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT)
           .show();
   }

   class RunnerThread extends Thread
   {
      class TaskCanceledException extends Exception
      {}

      Task task;

      NotificationHelper notificationHelper;

      File file;

      RunnerThread(Task t)
      {
         task = t;

         notificationHelper = new NotificationHelper(task);
      }

      public void run()
      {

         notificationHelper.onDownloadStart();

         File path = new File(WordMateX.FILES_PATH);

         file = new File(path, task.getFile());
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
            download(file);

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            sendBroadcast(intent);

            Log.e(TAG, "Task id: " + task.getId());


            if(file.getName()
                   .toLowerCase()
                   .endsWith(".zip"))
            {

               notificationHelper.onUnzipStart();

               try
               {
                  unzip(file, WordMateX.FILES_PATH + file.getName()
                                                         .substring(0,
                                                                    file.getName()
                                                                        .indexOf(".zip")));
               }

               catch(IOException e)
               {
                  e.printStackTrace();
                  notificationHelper.onFail(e.getMessage());
               }
               catch(TaskCanceledException e)
               {
                  notificationHelper.onCancel();
               }
               finally
               {
                  if(!file.delete())
                  {
                     throw new RuntimeException("Can not delete file " + file.getName());
                  }
               }
            }

            Log.i(TAG, "onFinished task id: " + task.getId());
            notificationHelper.onDone();

            intent = new Intent("dict-added");
            LocalBroadcastManager.getInstance(DownloadService.this).sendBroadcast(intent);
         }
         catch(IOException e)
         {
            e.printStackTrace();
            notificationHelper.onFail(e.getMessage());
         }
         catch(TaskCanceledException e)
         {
            notificationHelper.onCancel();
         }
         finally
         {
            handler.sendEmptyMessage(task.getId());
         }
      }

      private void download(File file) throws IOException, TaskCanceledException
      {

         OutputStream outputStream = null;
         InputStream inputStream = null;
         File tmpFile = null;

         notificationHelper.onProgress(0);
         try
         {
            URL url = new URL(task.getUrl());

            URLConnection connection = url.openConnection();

            inputStream = new BufferedInputStream(url.openStream(), BUFFER_SIZE);

            tmpFile = File.createTempFile(file.getName(), null, file.getParentFile());
            outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), BUFFER_SIZE);

            int lengthOfFile = connection.getContentLength();


            byte data[] = new byte[1024];

            long total = 0;

            int count,
                  progress = 0,
                  progressNew;

            while((count = inputStream.read(data)) != -1)
            {
               total += count;
               // writing data to file
               outputStream.write(data, 0, count);

               if(task.stop)
               {
                  throw new TaskCanceledException();
               }

               if(lengthOfFile < 0)
               {
                  continue;
               }

               // publishing the progress....
               // After this onProgressUpdate will be called
               progressNew = (int) (total * 100) / lengthOfFile;
               if(progress != progressNew)
               {
                  progress = progressNew;
                  notificationHelper.onProgress(progress);
               }
            }

            if(file.exists())
            {
               if(!file.delete())
               {
                  throw new RuntimeException("Can not delete file " + file.getName());
               }
            }
            if(!tmpFile.renameTo(file))
            {
               throw new RuntimeException("Can not rename file " + tmpFile.getName());
            }
         }
         finally
         {
            if(inputStream != null)
            {
               try
               {
                  inputStream.close();
               }
               catch(IOException e)
               {
                  e.printStackTrace();
               }
            }
            if(outputStream != null)
            {
               try
               {
                  outputStream.close();
               }
               catch(IOException e)
               {
                  e.printStackTrace();
               }
            }
            if(tmpFile != null && tmpFile.exists())
            {
               if(!tmpFile.delete())
               {
                  throw new RuntimeException("Can not delete file " + tmpFile.getName());
               }
            }

         }
      }

      private void unzip(File archive, String unzipAtLocation) throws IOException, TaskCanceledException
      {
         ZipFile zipFile = new ZipFile(archive);

         LinkedList<File> files = new LinkedList<>();

         try
         {
            for(Enumeration e = zipFile.entries(); e.hasMoreElements(); )
            {

               ZipEntry entry = (ZipEntry) e.nextElement();

               notificationHelper.onUnzipEntryStart(entry.getName());

               unzipEntry(zipFile, entry, unzipAtLocation, files);
            }

            Iterator<File> it = files.iterator();
            while(it.hasNext())
            {
               File f = it.next();
               if(f.exists())
               {

                  if(!f.delete())
                  {
                     throw new RuntimeException("Can not delete file " + f.getName());
                  }
               }
               if(!it.next()
                     .renameTo(f))
               {
                  throw new RuntimeException("Can not rename file " + f.getName());
               }
            }
         }
         catch(TaskCanceledException e)
         {
            for(File f : files)
            {
               if(f.getName()
                   .endsWith(".tmp"))
               {
                  if(!f.delete())
                  {
                     throw new RuntimeException("Can not delete file " + f.getName());
                  }
               }
            }

            throw e;
         }
         finally
         {
            zipFile.close();

         }
      }


      private void unzipEntry(ZipFile zipFile, ZipEntry zipEntry, String outputDir, LinkedList<File> files) throws IOException, TaskCanceledException
      {
         BufferedOutputStream outputStream = null;

         if(zipEntry.isDirectory())
         {
            createDir(new File(outputDir, zipEntry.getName()));
            return;
         }

         File outputFile = new File(outputDir, zipEntry.getName());
         if(!outputFile.getParentFile()
                       .exists())
         {
            createDir(outputFile.getParentFile());
         }

         Log.v(TAG, "Extracting: " + zipEntry);

         InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry), BUFFER_SIZE);
         try
         {
            File tmpFile = File.createTempFile(outputFile.getName(), null, outputFile.getParentFile());
            outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), BUFFER_SIZE);

            try
            {
               int size = (int) zipEntry.getSize();
               int interval = size / 100;
               if(interval < 1)
               {
                  interval = 1;
               }
               int percent = 0;
               int p = 0;
               int to = 0;
               while(p < size)
               {
                  to += interval;
                  if(to > size)
                  {
                     to = size;
                  }
                  while(p < to)
                  {
                     outputStream.write(inputStream.read());
                     p += 1;
                  }
                  if(task.stop)
                  {
                     throw new TaskCanceledException();
                  }
                  else if(percent != 100)
                  {
                     percent += 1;
                     notificationHelper.onProgress(percent);
                  }
               }
               files.add(outputFile);
               files.add(tmpFile);

            }
            finally
            {
               outputStream.close();
            }

         }
         finally
         {
            if(outputStream != null)
            {
               outputStream.close();
            }
            inputStream.close();
         }
      }

      private void createDir(File dir)
      {

         if(dir.exists())
         {
            return;
         }

         Log.v(TAG, "Creating dir " + dir.getName());

         if(!dir.mkdirs())
         {

            throw new RuntimeException("Can not create dir " + dir);
         }
      }
   }

   class NotificationHelper
   {
      private Context context;

      private Task task;

      private Intent intent;

      private PendingIntent pi;

      private NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

      private NotificationCompat.Builder oNotificationBuilder;

      NotificationHelper(Task task)
      {
         this(DownloadService.this, task);
      }

      NotificationHelper(Context context, Task task)
      {
         this.context = context;
         this.task = task;
         oNotificationBuilder = new NotificationCompat.Builder(context, NTF_CHN);
      }

      public void onDownloadStart()
      {
         intent = new Intent(context, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
         intent.putExtra(XTR_DICT_INFO, task);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         pi = PendingIntent.getService(context,
                                       0,
                                       intent,
                                       0);


         String title = getString(R.string.download) + ": " + task.getName();

         oNotificationBuilder.setContentTitle(title)
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_sys_download)
                             .setWhen(System.currentTimeMillis())
                             .setOngoing(true);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());

         Log.i(TAG, "onDownloadStart Task:" + task.getName());
      }

      public void onProgress(int progress)
      {

         oNotificationBuilder.setProgress(100, progress, false);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
         Log.i(TAG, "onProgress Task:" + task.getName() + " percent:" + progress);
      }

      public void onDone()
      {
         intent = new Intent(context, WordMateX.class);
         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
         intent.setAction(Long.toString(System.currentTimeMillis()));

         pi = PendingIntent.getActivity(context,
                                        0,
                                        intent,
                                        0);

         oNotificationBuilder.setContentTitle(task.getName())
                             .setContentText(getString(android.R.string.ok))
                             .setContentIntent(pi) // TODO ???
                             .setProgress(0, 0, false)
                             .setSmallIcon(android.R.drawable.stat_sys_download_done)
                             .setOngoing(false)
                             .setAutoCancel(true);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
         Log.i(TAG, "Done Task:" + task.getName());
      }

      public void onFail(String sError)
      {
         intent = new Intent(context, WordMateX.class);
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         intent.setAction(Long.toString(System.currentTimeMillis()));

         pi = PendingIntent.getActivity(context,
                                        0,
                                        intent,
                                        0);

         oNotificationBuilder.setContentTitle(task.getName())
                             .setContentText(sError)
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_notify_error)
                             .setWhen(System.currentTimeMillis())
                             .setAutoCancel(true);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
      }

      public void onUnzipStart()
      {
         intent = new Intent(context, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
         intent.putExtra(XTR_DICT_INFO, task);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         pi = PendingIntent.getService(context,
                                       0,
                                       intent,
                                       0);

         oNotificationBuilder.setTicker(task.getName())
                             .setContentTitle(getString(R.string.extract) + ": " + task.getName())
                             .setContentText(null)
                             .setSmallIcon(android.R.drawable.stat_sys_download)
                             .setContentIntent(pi);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
      }

      public void onUnzipEntryStart(String sEntry)
      {
         intent = new Intent(context, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
         intent.putExtra(XTR_DICT_INFO, task);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         pi = PendingIntent.getService(context,
                                       0,
                                       intent,
                                       0);

         oNotificationBuilder.setTicker(task.getName())
                             .setContentTitle(getString(R.string.extract) + ": " + task.getName())
                             .setContentText(sEntry)
                             .setSmallIcon(android.R.drawable.stat_sys_download)
                             .setContentIntent(pi);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
      }

      public void onCancel()
      {
         notificationManager.cancel(task.getId());
      }
   }
}
