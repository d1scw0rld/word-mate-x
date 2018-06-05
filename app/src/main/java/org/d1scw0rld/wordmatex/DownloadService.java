package org.d1scw0rld.wordmatex;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.Integer.valueOf;

public class DownloadService extends Service
{
   final static int ACT_DOWNLOAD = 1,
         ACT_CANCEL              = 2,
         ACT_VIEW                = 3;

   final static int BUFFER_SIZE = 65536;

   final static String XTR_NAME = "name",
         XTR_DATE               = "date",
         XTR_SIZE               = "size",
         XTR_ID                 = "id",
         XTR_ACTION             = "action",
         XTR_FILE               = "file",
         XTR_IS_DOWNLOADING     = "is_downloading",
         XTR_DICT_INFO          = "dict_info";

   final static String TAG = "DOWNLOAD_SERVICE";

   private final static String NTF_CHN = "dict_download_channel";


   private NotificationManager nm;

   private TreeMap<Integer, Task> tasks;

   private TreeMap<Integer, DictInfo> tmDictsInfo;

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
      tmDictsInfo = new TreeMap<>();
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
//               t = new Task();
//               t.id = id;
//               t.size = intent.getLongExtra(XTR_SIZE, 0);
//               t.name = intent.getStringExtra(XTR_NAME);
//               t.file = intent.getStringExtra(XTR_FILE);
//               t.date = new Date(intent.getLongExtra(XTR_DATE, -1));
//               tasks.put(id, t);
//               Log.i(TAG, "Task add " + t.name);
//               showToast(getString(R.string.download) + ": " + t.name);

               tmDictsInfo.put(dictInfo.getId(), dictInfo);
               Log.i(TAG, "Task add " + dictInfo.getName());
               showToast(getString(R.string.download) + ": " + dictInfo.getName());

               t = new Task(dictInfo);
               tasks.put(t.getId(), t);
               Log.i(TAG, "Task add " + t.getName());
               showToast(getString(R.string.download) + ": " + t.getName());

//               new RunnerNew(t).start();
               new Runner(t).execute();
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
            dictInfo = intent.getParcelableExtra(XTR_DICT_INFO);
            id = dictInfo.getId();

            Intent i = new Intent(this, DownloadViewer.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(XTR_DICT_INFO, dictInfo);
//            i.putExtra(XTR_DICT_INFO, intent.getParcelableExtra(XTR_DICT_INFO));
//            i.putExtra(XTR_ID, id);
//            i.putExtra(XTR_SIZE, intent.getLongExtra(XTR_SIZE, 0));
//            i.putExtra(XTR_NAME, intent.getStringExtra(XTR_NAME));
//            i.putExtra(XTR_DATE, intent.getLongExtra(XTR_DATE, -1));
//            i.putExtra(XTR_FILE, intent.getStringExtra(XTR_FILE));
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
      if(tasks.size() > 0)
      {
//         Log.i(TAG, "Remove " + id + " " + tasks.get(id).name);
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

//      int id;
//
//      long size;
//
//      String file,
//            name;
//
//      Date date;

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

   class Runner extends AsyncTask<Void, Integer, Void>
   {
      private Task task;

      private Intent intent;

      private NotificationCompat.Builder oNotificationBuilder;

      private Notification notification;

      private PendingIntent pi;

      Runner(Task task)
      {
         this.task = task;

         oNotificationBuilder = new NotificationCompat.Builder(DownloadService.this, NTF_CHN);
      }

      @Override
      protected void onPreExecute()
      {
         super.onPreExecute();

         downloadStarted();
      }

      @Override
      protected Void doInBackground(Void... voids)
      {
         File path = new File(WordMateX.FILES_PATH);

         File file = new File(path, task.getFile());
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

//
//         OutputStream outputStream = null;
//         InputStream inputStream = null;
//         try
//         {
//            URL url = new URL(task.getUrl());
//            URLConnection connection = url.openConnection();
//
//            inputStream = new BufferedInputStream(url.openStream(), 8192);
//
//            outputStream = new FileOutputStream(file);
////               outputStream.write(bufferedReader.read());
//
//            int lengthOfFile = connection.getContentLength();
//
////            List values = connection.getHeaderFields().get("content-Length");
////            if (values != null && !values.isEmpty())
////            {
////
////               // getHeaderFields() returns a Map with key=(String) header
////               // name, value = List of String values for that header field.
////               // just use the first value here.
////               String sLength = (String) values.get(0);
////
////               if(sLength != null)
////               {
////                  lengthOfFile = Integer.valueOf(sLength);
////               }
////            }
//
//            byte data[] = new byte[1024];
//
//            long total = 0;
//
//            int count,
//                  progress = 0, progressNew = 0;
//
//            while((count = inputStream.read(data)) != -1)
//            {
//               total += count;
//               // writing data to file
//               outputStream.write(data, 0, count);
//
//               if(lengthOfFile < 0)
//               {
//                  continue;
//               }
//
//               // publishing the progress....
//               // After this onProgressUpdate will be called
//               progressNew = (int) (total * 100) / lengthOfFile;
//               if(progress != progressNew)
//               {
//                  progress = progressNew;
//                  publishProgress(progress);
//               }
//            }
//
//            // Tell android about the file
//            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            intent.setData(Uri.fromFile(file));
//            DownloadService.this.sendBroadcast(intent);
         try
         {
            download(file);
         }
         catch(IOException e)
         {
            e.printStackTrace();

         }
         finally
         {
            downloadDone();
         }


//            if(file.getName()
//                   .toLowerCase()
//                   .endsWith(".zip"))
//            {
//               unzipStarted();
//
//               unzip(task.getFile(),
//                     WordMateX.FILES_PATH + task.getFile()
//                                                .substring(0,
//                                                           task.getFile()
//                                                               .indexOf(".zip")));
//            }

            if(file.getName()
                   .toLowerCase()
                   .endsWith(".zip"))
            {

               unzipStarted();

               try
               {
                  unzip(file, WordMateX.FILES_PATH + file.getName()
                                                         .substring(0,
                                                                    file.getName()
                                                                        .indexOf(".zip")));
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
               finally
               {
                  file.delete();
               }


//               LinkedList<File> files = new LinkedList<>();
//               ZipFile zipFile = new ZipFile(file);
//               File dir = new File(WordMateX.FILES_PATH + file.getName()
//                                                              .substring(0,
//                                                                         file.getName()
//                                                                             .indexOf(".zip")));
//               if(!dir.exists())
//               {
//                  if(!dir.mkdirs())
//                  {
//                     throw new RuntimeException("Unable to create directory: " + path);
//                  }
//               }
//
//               Enumeration<? extends ZipEntry> entries = zipFile.entries();
//               File entryFile;
//               while(entries.hasMoreElements())
//               {
//                  ZipEntry zipEntry = entries.nextElement();
//                  if(!zipEntry.isDirectory())
//                  {
//                     entryFile = new File(dir, zipEntry.getName());
//                     unzipEntryStart(zipEntry.getName());
//
//                     inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry), BUFFER_SIZE);
//                     File tmpFile = File.createTempFile(entryFile.getName(), null, dir);
//                     outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), BUFFER_SIZE);
//
//                     int size = (int) zipEntry.getSize();
//                     int interval = size / 100;
//                     if(interval < 1)
//                     {
//                        interval = 1;
//                     }
//                     int percent = 0;
//                     int p = 0;
//                     int to = 0;
//                     while(p < size)
//                     {
//                        to += interval;
//                        if(to > size)
//                        {
//                           to = size;
//                        }
//                        while(p < to)
//                        {
//                           outputStream.write(inputStream.read());
//                           p += 1;
//                        }
//                        if(task.stop)
//                        {
//                           inputStream.close();
//                           outputStream.close();
//                           zipFile.close();
//                           tmpFile.delete();
//                           return null;
//                        }
//                        else if(percent != 100)
//                        {
//                           percent += 1;
//                           publishProgress(percent);
//                        }
//                     }
//                     inputStream.close();
//                     outputStream.close();
//                     files.add(entryFile);
//                     files.add(tmpFile);
//                  }
//               }
//               zipFile.close();
//               file.delete();

//               Iterator<File> it = files.iterator();
//               while(it.hasNext())
//               {
//                  File f = it.next();
//                  if(f.exists())
//                  {
//                     f.delete();
//                  }
//                  it.next()
//                    .renameTo(f);
//               }
            }


            nm.cancel(task.getId());
            nm.notify(task.getId(), notification);
            handler.sendEmptyMessage(task.getId());
//         }

//         catch(DbxException |
//      IOException e)

//         catch(IOException e)
//         {
//            e.printStackTrace();
//
//            if(!task.stop)
//            {
//
//
//               downloadFailed(e.getMessage());
//
//               handler.sendEmptyMessage(task.getId());
//            }
//         }
//         catch(Exception e)
//         {
//            e.printStackTrace();
//         }
//         finally
//         {
//            if(inputStream != null)
//            {
//               try
//               {
//                  inputStream.close();
//               }
//               catch(IOException e)
//               {
//                  e.printStackTrace();
//               }
//            }
//            if(outputStream != null)
//            {
//               try
//               {
//                  outputStream.close();
//               }
//               catch(IOException e)
//               {
//                  e.printStackTrace();
//               }
//            }
//         }

         return null;
      }

      @Override
      protected void onProgressUpdate(Integer... values)
      {
         oNotificationBuilder.setProgress(100, values[0], false);

         notification = oNotificationBuilder.build();

         nm.notify(task.getId(), notification);
      }

      @Override
      protected void onPostExecute(Void aVoid)
      {
         super.onPostExecute(aVoid);
      }

      private void downloadStarted()
      {
         intent = new Intent(DownloadService.this, DownloadService.class);
         intent.putExtra(XTR_ACTION, DownloadService.ACT_VIEW);
//         intent.putExtra(XTR_ID, task.getId());
         intent.putExtra(XTR_DICT_INFO, (DictInfo) task);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         PendingIntent pi = PendingIntent.getService(DownloadService.this,
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

      private void downloadFailed(String sError)
      {
         oNotificationBuilder.setContentText(sError);
         notification = oNotificationBuilder.build();
         notification.flags = Notification.FLAG_AUTO_CANCEL;

         nm.cancel(task.getId());
         nm.notify(task.getId(), notification);
      }

      private void downloadDone()
      {
         intent = new Intent(DownloadService.this, WordMateX.class);
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         intent.setAction(Long.toString(System.currentTimeMillis()));
         pi = PendingIntent.getActivity(DownloadService.this,
                                        0,
                                        intent,
                                        0);

         oNotificationBuilder.setContentTitle(task.getName())
                             .setContentText(getString(android.R.string.ok))
                             .setContentIntent(pi)
                             .setSmallIcon(android.R.drawable.stat_sys_download_done)
                             .setWhen(System.currentTimeMillis());
         notification = oNotificationBuilder.build();
         notification.flags = Notification.FLAG_AUTO_CANCEL;

         nm.notify(task.getId(), notification);
      }

      private void unzipStarted()
      {
         oNotificationBuilder.setContentTitle(getString(R.string.extract) + ": " + task.getName())
                             .setContentText(null)
                             .setSmallIcon(android.R.drawable.stat_sys_download);

         notification = oNotificationBuilder.build();

         nm.notify(task.getId(), notification);
      }

      private void unzipEntryStart(String sEntry)
      {
         oNotificationBuilder.setContentText(sEntry);

         notification = oNotificationBuilder.build();

         nm.notify(task.getId(), notification);
      }


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

         try
         {
            URL url = new URL(task.getUrl());

            URLConnection connection = url.openConnection();

            inputStream = new BufferedInputStream(url.openStream(), 8192);

//            outputStream = new FileOutputStream(file);

            tmpFile = File.createTempFile(file.getName(), null, file.getParentFile());
            outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), BUFFER_SIZE);

            int lengthOfFile = connection.getContentLength();


            byte data[] = new byte[1024];

            long total = 0;

            int count,
                  progress = 0, progressNew = 0;

            while((count = inputStream.read(data)) != -1)
            {
               total += count;
               // writing data to file
               outputStream.write(data, 0, count);

               if(task.stop)
               {
                  inputStream.close();
                  outputStream.close();
                  tmpFile.delete();
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
               file.delete();
            }
            tmpFile.renameTo(file);

            // Tell android about the file
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            DownloadService.this.sendBroadcast(intent);
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
            if(tmpFile != null)
               tmpFile.delete();

         }
      }

      private void unzip(String zipFilePath, String unzipAtLocation) throws Exception
      {

         File archive = new File(zipFilePath);

         unzip(archive, unzipAtLocation);

////         try
////         {
//
//            ZipFile zipfile = new ZipFile(archive);
//
//            for(Enumeration e = zipfile.entries(); e.hasMoreElements(); )
//            {
//
//               ZipEntry entry = (ZipEntry) e.nextElement();
//
//               unzipEntry(zipfile, entry, unzipAtLocation);
//            }
//
////         }
////         catch(Exception e)
////         {
////
////            Log.e("Unzip zip", "Unzip exception", e);
////         }
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

               unzipEntryStart(entry.getName());

               unzipEntry(zipFile, entry, unzipAtLocation, files);
            }

            Iterator<File> it = files.iterator();
            while(it.hasNext())
            {
               File f = it.next();
               if(f.exists())
               {
                  f.delete();
               }
               it.next()
                 .renameTo(f);
            }
         }
         finally
         {
            zipFile.close();

         }
//         catch(Exception e)
//         {
//
//            Log.e("Unzip zip", "Unzip exception", e);
//         }
      }


      private void unzipEntry(ZipFile zipFile, ZipEntry zipEntry, String outputDir, LinkedList<File> files) throws IOException
      {

         unzipEntryStart(zipEntry.getName());
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

//         InputStream zin = zipFile.getInputStream(zipEntry);
//         BufferedInputStream inputStream = new BufferedInputStream(zin);
         InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry), BUFFER_SIZE);
         try
         {
            File tmpFile = File.createTempFile(outputFile.getName(), null, outputFile.getParentFile());
//         BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), BUFFER_SIZE);

            //IOUtils.copy(inputStream, outputStream);

            try
            {

//               for(int c = inputStream.read(); c != -1; c = inputStream.read())
//               {
//                  outputStream.write(c);
//               }

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
                     tmpFile.delete();
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
               outputStream.close();
            inputStream.close();
         }
      }

      private void createDir(File dir)
      {

         if(dir.exists())
         {
            return;
         }

         Log.v("ZIP E", "Creating dir " + dir.getName());

         if(!dir.mkdirs())
         {

            throw new RuntimeException("Can not create dir " + dir);
         }
      }
   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT)
           .show();
   }
}
