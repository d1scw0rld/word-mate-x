package org.d1scw0rld.wordmatex;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
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

   private NotificationManager nm;

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

   Callback callback;

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
      handler = new IncomingHandler(this);
      callback = new Callback()
      {
         private Intent intent;
         private Notification notification;
         private PendingIntent pi;
         private NotificationCompat.Builder oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);

         @Override
         public void onDownloadStart(Task task)
         {
            intent = new Intent(DownloadService.this, DownloadService.class);
            intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
            intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            pi = PendingIntent.getService(DownloadService.this,
                                          0,
                                          intent,
                                          0);


            String title = getString(R.string.download) + ": " + task.getName();


            oNotificationBuilder.setContentTitle(title)
                                .setContentIntent(pi)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setTicker(task.getName())
                                .setWhen(System.currentTimeMillis());

            notification = oNotificationBuilder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;

            nm.notify(task.getId(), notification);
         }

         @Override
         public void onProgress(Task task, int progress)
         {
            intent = new Intent(DownloadService.this, DownloadService.class);
            intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
            intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            pi = PendingIntent.getService(DownloadService.this,
                                          0,
                                          intent,
                                          0);


            oNotificationBuilder.setContentIntent(pi)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setTicker(task.getName())
                                .setProgress(100, progress, false);

            notification = oNotificationBuilder.build();

            nm.notify(task.getId(), notification);
         }

         @Override
         public void onDone(Task task, File file)
         {
            intent = new Intent(DownloadService.this, WordMateX.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Long.toString(System.currentTimeMillis()));

            pi = PendingIntent.getActivity(DownloadService.this,
                                           0,
                                           intent,
                                           0);

            oNotificationBuilder.setContentTitle(task.getName())
                                .setTicker(task.getName())
                                .setContentText(getString(android.R.string.ok))
                                .setContentIntent(null)
                                .setProgress(0, 0, false)
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setWhen(System.currentTimeMillis());

            notification = oNotificationBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;

            nm.cancel(task.getId());
            nm.notify(task.getId(), notification);
//
//            handler.sendEmptyMessage(id);

            // Tell android about the file
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            sendBroadcast(intent);
         }

         @Override
         public void onFail(Task task, String sError)
         {
            intent = new Intent(DownloadService.this, WordMateX.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Long.toString(System.currentTimeMillis()));

            pi = PendingIntent.getActivity(DownloadService.this,
                                           0,
                                           intent,
                                           0);

            oNotificationBuilder.setContentTitle(task.getName())
                                .setTicker(task.getName())
                                .setContentText(sError)
                                .setContentIntent(pi)
                                .setSmallIcon(android.R.drawable.stat_notify_error)
                                .setWhen(System.currentTimeMillis());

            notification = oNotificationBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;

            nm.cancel(task.getId());
            nm.notify(task.getId(), notification);
         }

         @Override
         public void onUnzipStart(Task task)
         {
            intent = new Intent(DownloadService.this, DownloadService.class);
            intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
            intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            pi = PendingIntent.getService(DownloadService.this,
                                          0,
                                          intent,
                                          0);

            oNotificationBuilder.setTicker(task.getName())
                                .setContentTitle(getString(R.string.extract) + ": " + task.getName())
                                .setContentText(null)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setContentIntent(pi);

            notification = oNotificationBuilder.build();

            nm.notify(task.getId(), notification);
         }

         @Override
         public void onUnzipEntryStart(Task task, String sEntry)
         {
            intent = new Intent(DownloadService.this, DownloadService.class);
            intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
            intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            pi = PendingIntent.getService(DownloadService.this,
                                          0,
                                          intent,
                                          0);

            oNotificationBuilder.setTicker(task.getName())
                                .setContentTitle(getString(R.string.extract) + ": " + task.getName())
                                .setContentText(sEntry)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setContentIntent(pi);

            notification = oNotificationBuilder.build();

            nm.notify(task.getId(), notification);
         }

//                  @Override
//                  public void onUnzipFailed(int id, String sError)
//                  {
//                     oNotificationBuilder.setContentText(sError);
//                     notification = oNotificationBuilder.build();
//                     notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//                     nm.cancel(id);
//                     nm.notify(id, notification);
//                  }
//
//                  @Override
//                  public void onUnzipDone()
//                  {
//
//                  }

         @Override
         public void onFinished(int id)
         {
//            nm.notify(id, notification);
//            nm.cancel(id);
            handler.sendEmptyMessage(id);
         }
      };
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

//      int id = intent.getIntExtra(XTR_ID, 0);
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

//               new RunnerNew(t).start();
               new Runner(t, callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            break;

         case ACT_CANCEL /* 2 */:
            id = intent.getIntExtra(XTR_ID, 0);
            t = tasks.get(valueOf(id));
            if(t != null)
            {
               t.stop = true;
               nm.cancel(id);
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
//         setForeground(true);
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


   //   class RunnerNew extends Thread
//   {
//      Task task;
//
//
//      RunnerNew(Task t)
//      {
//         task = t;
//      }
//
//      public void run()
//      {
////         Exception e;
//
//         Intent intent = new Intent(DownloadService.this, DownloadService.class);
//         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
//         intent.putExtra(XTR_ID, task.id);
//         intent.putExtra(XTR_SIZE, task.size);
//         intent.putExtra(XTR_NAME, task.name);
//         intent.putExtra(XTR_FILE, task.file);
//         intent.setAction(Long.toString(System.currentTimeMillis()));
//         PendingIntent pi = PendingIntent.getService(DownloadService.this,
//                                                     0,
//                                                     intent,
//                                                     0);
//
//         String title = getString(R.string.download) + ": " + task.name;
////         notification.setLatestEventInfo(DownloadService.this, name, "", pi);
////         Notification notification = new Notification(android.R.drawable.stat_sys_download,
////                                                      task.name,
////                                                      System.currentTimeMillis());
////         notification.flags = Notification.FLAG_ONGOING_EVENT;
//
//         NotificationCompat.Builder oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
//         oNotificationBuilder.setContentTitle(title)
//                             .setContentIntent(pi)
//                             .setSmallIcon(android.R.drawable.stat_sys_download)
//                             .setTicker(task.name)
//                             .setWhen(System.currentTimeMillis());
//         Notification notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_ONGOING_EVENT;
//
////         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
////         {
////            notification = oNotificationBuilder.build();
////         }
////         else
////            notification = oNotificationBuilder.getNotification();
//
//         nm.notify(task.id, notification);
//
//         File path = new File(WordMateX.FILES_PATH);
//         File file = new File(path, task.file);
//         // Make sure the Downloads directory exists.
//         if(!path.exists())
//         {
//            if(!path.mkdirs())
//            {
//               throw new RuntimeException("Unable to create directory: " + path);
//            }
//         }
//         else if(!path.isDirectory())
//         {
//            throw new IllegalStateException("Download path is not a directory: " + path);
//         }
//
//         try
//         {
//            OutputStream outputStream = new FileOutputStream(file);
//
//            dbxClient.files()
//                     .download(task.file)
//                     //                      .download(metadata.getPathLower(), metadata.getRev())
//                     .download(outputStream);
//            // Tell android about the file
//            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            intent.setData(Uri.fromFile(file));
//            DownloadService.this.sendBroadcast(intent);
//
//            intent = new Intent(DownloadService.this, WordMateX.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setAction(Long.toString(System.currentTimeMillis()));
//            pi = PendingIntent.getActivity(DownloadService.this,
//                                           0,
//                                           intent,
//                                           0);
//
//            oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
//            oNotificationBuilder.setContentTitle(task.name)
//                                .setContentText(getString(android.R.string.ok))
//                                .setContentIntent(pi)
//                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
//                                .setWhen(System.currentTimeMillis());
//            notification = oNotificationBuilder.build();
//            notification.flags = Notification.FLAG_AUTO_CANCEL;
//            notification = oNotificationBuilder.build();
//
//            nm.cancel(task.id);
//            nm.notify(task.id, notification);
//            handler.sendEmptyMessage(task.id);
//
//         }
//         catch(DbxException | IOException e)
//         {
//            e.printStackTrace();
//
//            if(!task.stop)
//            {
////               notification = new Notification(android.R.drawable.stat_notify_error,
////                                               task.name,
////                                               System.currentTimeMillis());
////               notification.flags = Notification.FLAG_AUTO_CANCEL;
//               oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
//               oNotificationBuilder.setContentTitle(title)
//                                   .setContentText(e.getMessage())
//                                   .setContentIntent(pi);
//               notification = oNotificationBuilder.build();
//               notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//               nm.cancel(task.id);
//               nm.notify(task.id, notification);
//               handler.sendEmptyMessage(task.id);
//            }
//         }
//      }
//   }
//
   interface Callback
   {
      void onDownloadStart(Task task);

      void onProgress(Task task, int progress);

      void onDone(Task task, File file);

      void onFail(Task task, String sError);

      void onUnzipStart(Task task);

      void onUnzipEntryStart(Task task, String sEntry);

//         void onUnzipFailed(int id, String sError);
//
//         void onUnzipDone();

      void onFinished(int id);
   }

   class Runner extends AsyncTask<Void, Integer, Void>
   {
      private final static int STG_UNZIP_START = 1,
                               STG_UNZIP_ENTRY = 2;
      private Task task;

      private Intent intent;

      private NotificationCompat.Builder oNotificationBuilder;

      private Notification notification;

      private PendingIntent pi;

      private Callback callback;

      NotificationHelper notificationHelper;
      File file;

      Runner(Task task, Callback callback)
      {
         this.task = task;
         this.callback = callback;
         notificationHelper = new NotificationHelper(task);

//         oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
      }

      @Override
      protected void onPreExecute()
      {
         super.onPreExecute();

//         downloadStarted();
//         callback.onDownloadStart(task);
         notificationHelper.onDownloadStart();
      }

      @Override
      protected Void doInBackground(Void... voids)
      {
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

            Log.e(TAG, "Task id: " + task.getId());
//            callback.onDone(task, file);


            if(file.getName()
                   .toLowerCase()
                   .endsWith(".zip"))
            {

//            unzipStarted();
//               callback.onUnzipStart(task);
               notificationHelper.onUnzipStart();

               try
               {
                  unzip(file, WordMateX.FILES_PATH + file.getName()
                                                         .substring(0,
                                                                    file.getName()
                                                                        .indexOf(".zip")));
//                  callback.onDone(task, file);
//                  notificationHelper.onDone(file);

               }
               catch(Exception e)
               {
                  e.printStackTrace();
//                  callback.onFail(task, e.getMessage());
                  notificationHelper.onFail(e.getMessage());
               }
               finally
               {
                  if(!file.delete())
                  {
                     throw new RuntimeException("Can not delete file " + file.getName());
                  }
               }
            }
         }
         catch(IOException e)
         {
            e.printStackTrace();
//            downloadFailed(e.getMessage());
//            callback.onFail(task, e.getMessage());
            notificationHelper.onFail(e.getMessage());
         }


//         nm.cancel(task.getId());
//         nm.notify(task.getId(), notification);
//         handler.sendEmptyMessage(task.getId());

         return null;
      }

      @Override
      protected void onProgressUpdate(Integer... values)
      {
//         oNotificationBuilder.setProgress(100, values[0], false);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//         callback.onProgress(task, values[0]);
         notificationHelper.onProgress(values[0]);
      }

      @Override
      protected void onPostExecute(Void aVoid)
      {
         super.onPostExecute(aVoid);
         Log.i(TAG, "onFinished task id: " + task.getId());
//         callback.onFinished(task.getId());
         notificationHelper.onDone(file);
         notificationHelper.onFinished(task.getId());

      }

//      private void downloadStarted()
//      {
//         intent = new Intent(DownloadService.this, DownloadService.class);
//         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
////         intent.putExtra(XTR_ID, task.getId());
//         intent.putExtra(XTR_DICT_INFO, task);
//         intent.setAction(Long.toString(System.currentTimeMillis()));
//         PendingIntent pi = PendingIntent.getService(DownloadService.this,
//                                                     0,
//                                                     intent,
//                                                     0);
//
//
//         String title = getString(R.string.download) + ": " + task.getName();
//
//
//         oNotificationBuilder.setContentTitle(title)
//                             .setContentIntent(pi)
//                             .setSmallIcon(android.R.drawable.stat_sys_download)
//                             .setTicker(task.getName())
//                             .setWhen(System.currentTimeMillis());
//
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_ONGOING_EVENT;
//
//         nm.notify(task.getId(), notification);
//      }
//
//      private void downloadFailed(String sError)
//      {
//         oNotificationBuilder.setContentText(sError);
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//         nm.cancel(task.getId());
//         nm.notify(task.getId(), notification);
//      }
//
//      private void downloadDone()
//      {
//         intent = new Intent(DownloadService.this, WordMateX.class);
//         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//         intent.setAction(Long.toString(System.currentTimeMillis()));
//         pi = PendingIntent.getActivity(DownloadService.this,
//                                        0,
//                                        intent,
//                                        0);
//
//         oNotificationBuilder.setContentTitle(task.getName())
//                             .setContentText(getString(android.R.string.ok))
//                             .setContentIntent(pi)
//                             .setSmallIcon(android.R.drawable.stat_sys_download_done)
//                             .setWhen(System.currentTimeMillis());
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//         nm.notify(task.getId(), notification);
//      }
//
//      private void unzipStarted()
//      {
//         oNotificationBuilder.setContentTitle(getString(R.string.extract) + ": " + task.getName())
//                             .setContentText(null)
//                             .setSmallIcon(android.R.drawable.stat_sys_download);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }
//
//      private void unzipEntryStart(String sEntry)
//      {
//         oNotificationBuilder.setContentText(sEntry);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }


//      private void sendProgress(int progress)
//      {
//         oNotificationBuilder.setProgress(100, progress, false);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }


      private void download(File file) throws IOException
      {

         OutputStream outputStream = null;
         InputStream inputStream = null;
         File tmpFile = null;

         publishProgress(0);
         try
         {
            URL url = new URL(task.getUrl());

            URLConnection connection = url.openConnection();

            inputStream = new BufferedInputStream(url.openStream(), BUFFER_SIZE);

//            outputStream = new FileOutputStream(file);

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
                  inputStream.close();
                  outputStream.close();
                  if(!tmpFile.delete())
                  {
                     Log.i(TAG, "Cannot delete the temp file");
                     throw new RuntimeException("Can not delete file " + tmpFile.getName());
                  }
                  Log.i(TAG, "Temp file deleted");
                  return;
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
                  publishProgress(progress);
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
            assert tmpFile != null;
            if(tmpFile.exists())
            {
               if(!tmpFile.delete())
               {
                  throw new RuntimeException("Can not delete file " + tmpFile.getName());
               }
            }

         }
      }

      private void unzip(File archive, String unzipAtLocation) throws Exception
      {
         ZipFile zipFile = new ZipFile(archive);

//         unzipStarted();
         LinkedList<File> files = new LinkedList<>();

         try
         {
            for(Enumeration e = zipFile.entries(); e.hasMoreElements(); )
            {

               ZipEntry entry = (ZipEntry) e.nextElement();

//               unzipEntryStart(entry.getName());
//               callback.onUnzipEntryStart(task, entry.getName());
               notificationHelper.onUnzipEntryStart(entry.getName());

               unzipEntry(zipFile, entry, unzipAtLocation, files);

               if(task.stop)
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
               }
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
         finally
         {
            zipFile.close();

         }
      }


      private void unzipEntry(ZipFile zipFile, ZipEntry zipEntry, String outputDir, LinkedList<File> files) throws IOException
      {

//         unzipEntryStart(zipEntry.getName());
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
                     inputStream.close();
                     outputStream.close();
                     zipFile.close();
                     if(!tmpFile.delete())
                     {
                        throw new RuntimeException("Can not delete file " + tmpFile.getName());
                     }
                     return;
                  }
                  else if(percent != 100)
                  {
                     percent += 1;
                     publishProgress(percent);
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

//      public void notifyDownloadStart()
//      {
//         intent = new Intent(DownloadService.this, DownloadService.class);
//         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
////         intent.putExtra(XTR_ID, task.getId());
//         intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
//         intent.setAction(Long.toString(System.currentTimeMillis()));
//         PendingIntent pi = PendingIntent.getService(DownloadService.this,
//                                                     0,
//                                                     intent,
//                                                     0);
//
//
//         String title = getString(R.string.download) + ": " + task.getName();
//
//
//         oNotificationBuilder.setContentTitle(title)
//                             .setContentIntent(pi)
//                             .setSmallIcon(android.R.drawable.stat_sys_download)
//                             .setTicker(task.getName())
//                             .setWhen(System.currentTimeMillis());
//
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_ONGOING_EVENT;
//
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyProgress(int progress)
//      {
//         oNotificationBuilder.setProgress(100, progress, false);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyDownloadDone(File file)
//      {
//         oNotificationBuilder.setContentText(getString(android.R.string.ok))
//                             .setContentIntent(null)
//                             .setProgress(0, 0, false)
//                             .setSmallIcon(android.R.drawable.stat_sys_download_done);
//
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_AUTO_CANCEL;
//
////            nm.notify(id, notification);
////            nm.cancel(id);
//         nm.notify(task.getId(), notification);
////
////            handler.sendEmptyMessage(id);
//
//         // Tell android about the file
//         intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//         intent.setData(Uri.fromFile(file));
//         sendBroadcast(intent);
//      }
//
//      public void notifyDownloadFailed(String sError)
//      {
//         oNotificationBuilder.setContentText(sError);
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//         nm.cancel(task.getId());
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyUnzipStart()
//      {
//         oNotificationBuilder.setContentTitle(getString(R.string.extract) + ": " + task.getName())
//                             .setContentText(null)
//                             .setSmallIcon(android.R.drawable.stat_sys_download);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyUnzipEntryStart(String sEntry)
//      {
//         oNotificationBuilder.setContentText(sEntry);
//
//         notification = oNotificationBuilder.build();
//
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyUnzipFailed(String sError)
//      {
//         oNotificationBuilder.setContentText(sError);
//         notification = oNotificationBuilder.build();
//         notification.flags = Notification.FLAG_AUTO_CANCEL;
//
//         nm.cancel(task.getId());
//         nm.notify(task.getId(), notification);
//      }
//
//      public void notifyUnzipDone()
//      {
//
//      }
//
//      public void notifyFinished()
//      {
////            nm.notify(id, notification);
//         nm.cancel(task.getId());
//         handler.sendEmptyMessage(task.getId());
//      }
   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT)
           .show();
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
         intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         pi = PendingIntent.getService(context,
                                       0,
                                       intent,
                                       0);


         String title = getString(R.string.download) + ": " + task.getName();

         oNotificationBuilder.setContentTitle(title)
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_sys_download)
//                             .setTicker(task.getName())
                             .setWhen(System.currentTimeMillis())
                             .setOngoing(true);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());

         Log.i(TAG, "onDownloadStart Task:"+task.getName());
      }

      public void onProgress(int progress)
      {

         oNotificationBuilder.setProgress(100, progress, false);

         notificationManager.notify(task.getId(), oNotificationBuilder.build());
         Log.i(TAG, "onProgress Task:" + task.getName() + " percent:" + progress);
      }

      public void onDone(File file)
      {
         intent = new Intent(context, WordMateX.class);
//         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
         intent.setAction(Long.toString(System.currentTimeMillis()));

         pi = PendingIntent.getActivity(context,
                                        0,
                                        intent,
                                        0);

         oNotificationBuilder.setContentTitle(task.getName())
//                             .setTicker(task.getName())
                             .setContentText(getString(android.R.string.ok))
                             .setContentIntent(pi) // TODO ???
                             .setProgress(0, 0, false)
                             .setSmallIcon(android.R.drawable.stat_sys_download_done)
//                             .setWhen(System.currentTimeMillis())
                             .setOngoing(false)
                             .setAutoCancel(true);

//         notificationManager.cancel(task.getId());
         notificationManager.notify(task.getId(), oNotificationBuilder.build());
         Log.i(TAG, "Done Task:" + task.getName());
         // Tell android about the file
         intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
         intent.setData(Uri.fromFile(file));
         sendBroadcast(intent);
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
//                             .setTicker(task.getName())
                             .setContentText(sError)
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_notify_error)
                             .setWhen(System.currentTimeMillis())
                             .setAutoCancel(true);

//         nm.cancel(task.getId());
         notificationManager.notify(task.getId(), oNotificationBuilder.build());
      }

      public void onUnzipStart()
      {
         intent = new Intent(context, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
         intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
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
         intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
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

      public void onFinished(int id)
      {
         handler.sendEmptyMessage(id);
      }
   }
}
