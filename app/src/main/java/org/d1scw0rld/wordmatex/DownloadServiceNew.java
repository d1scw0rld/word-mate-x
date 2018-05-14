package org.d1scw0rld.wordmatex;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.Integer.valueOf;

public class DownloadServiceNew extends Service
{
   static final int bufferSize = 65536;

   final static int ACT_DOWNLOAD = 1,
         ACT_CANCEL              = 2,
         ACT_VIEW                = 3;

   final static String XTR_NAME = "name",
         XTR_INFO               = "info",
         XTR_SIZE               = "size",
         XTR_ID                 = "id",
         XTR_ACTION             = "action",
         XTR_FILE               = "file",
         XTR_IS_DOWNLOADING     = "is_downloading";


   NotificationManager nm;

   TreeMap<Integer, Task> tasks;

   Handler handler = new Handler()
   {

      public void handleMessage(Message msg)
      {
         done(msg.what);
      }
   };

   public void onCreate()
   {
      tasks = new TreeMap<>();
      nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
   }

   public IBinder onBind(Intent intent)
   {
      return null;
   }

   public void onStart(Intent intent, int startId)
   {
      Task t;

      int id = intent.getIntExtra(XTR_ID, 0);
      switch(intent.getIntExtra(XTR_ACTION, 0))
      {
         case ACT_DOWNLOAD /* 1 */:
            if(tasks.get(valueOf(id)) == null)
            {
               t = new Task();
               t.id = id;
               t.size = intent.getIntExtra(XTR_SIZE, 0);
               t.name = intent.getStringExtra(XTR_NAME);
               t.file = intent.getStringExtra(XTR_FILE);
               t.info = intent.getStringExtra(XTR_INFO);
               tasks.put(id, t);

               showToast(getString(R.string.download) + ": " + t.name);

               new Runner(t).start();
            }
            break;

         case ACT_CANCEL /* 2 */:
            t = tasks.get(valueOf(id));
            if(t != null)
            {
               t.stop = true;
               nm.cancel(id);
               tasks.remove(valueOf(id));
               showToast(getString(R.string.cancel_download)
                               + ": "
                               + t.name);
            }
            break;

         case ACT_VIEW /* 3 */:
            Intent i = new Intent(this, DownloadViewer.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(XTR_ID, id);
            i.putExtra(XTR_SIZE, intent.getIntExtra(XTR_SIZE, 0));
            i.putExtra(XTR_NAME, intent.getStringExtra(XTR_NAME));
            i.putExtra(XTR_INFO, intent.getStringExtra(XTR_INFO));
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
         startForeground(id, null); // TODO Fix it. It just replace setForeground
      }
      else
      {
         stopSelf();
      }
   }

   void done(int id)
   {
      tasks.remove(id);
      if(tasks.size() == 0)
      {
         stopSelf();
      }
   }

   class Task
   {

      boolean stop;
      int     id,
            size;

      String file,
            name,
            info;

      Task()
      {
      }

   }

   class Runner extends Thread
   {


      Task task;

      Runner(Task t)
      {
         this.task = t;
      }

      public void run()
      {
         Exception e;
//         Intent intent;
         Notification notification = new Notification(android.R.drawable.stat_sys_download,
                                                      task.name,
                                                      System.currentTimeMillis());
         notification.flags = DownloadServiceNew.ACT_CANCEL;
         Intent intent2 = new Intent(DownloadServiceNew.this, DownloadServiceNew.class);
         intent2.putExtra(XTR_ACTION, DownloadServiceNew.ACT_VIEW);
         intent2.putExtra(XTR_ID, task.id);
         intent2.putExtra(XTR_SIZE, task.size);
         intent2.putExtra(XTR_NAME, task.name);
         intent2.putExtra(XTR_FILE, task.file);
         intent2.setAction(Long.toString(System.currentTimeMillis()));
         PendingIntent pi = PendingIntent.getService(DownloadServiceNew.this,
                                                     0,
                                                     intent2,
                                                     0);
         String title = getString(R.string.download) + ": " + task.name;
//         notification.setLatestEventInfo(DownloadService.this, name, "", pi);
         Notification.Builder builder = new Notification.Builder(DownloadServiceNew.this);
         builder.setContentTitle(title);
         builder.setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
         notification = builder.getNotification();

         nm.notify(task.id, notification);
         try
         {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://dictdownload.wordmate.net/ACT_DOWNLOAD?id="
                                                                       + Integer.toString(task.id)).openConnection();
            if(conn.getResponseCode() == 200 && conn.getContentLength() == task.size)
            {
               BufferedInputStream bufferedInputStream = new BufferedInputStream(conn.getInputStream(), bufferSize);
               File file = new File(task.file);
               File dir = file.getParentFile();
               if(!dir.exists())
               {
                  dir.mkdirs();
               }
               File tmpFile = File.createTempFile(file.getName(), null, dir);
               BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
               String text = "% "
                     + file.getName()
                     + " "
                     + Integer.toString(task.size / 1000)
                     + "KB";
//               notification.setLatestEventInfo(DownloadService.this,
//                                               name,
//                                               "0" + text,
//                                               pi);

               builder = new Notification.Builder(DownloadServiceNew.this);
               builder.setContentTitle(title)
                      .setContentText("0" + text)
                      .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
               notification = builder.getNotification();

               nm.notify(task.id, notification);
               int interval = task.size / 100;
               if(interval < ACT_DOWNLOAD)
               {
                  interval = ACT_DOWNLOAD;
               }
               int percent = 0;
               int p = 0;
               int to = 0;
               while(p < this.task.size)
               {
                  to += interval;
                  if(to > this.task.size)
                  {
                     to = this.task.size;
                  }
                  while(p < to)
                  {
                     bufferedOutputStream.write(bufferedInputStream.read());
                     p += ACT_DOWNLOAD;
                  }
                  if(this.task.stop)
                  {
                     bufferedInputStream.close();
                     bufferedOutputStream.close();
                     tmpFile.delete();
                     return;
                  }
                  else if(percent != 100)
                  {
                     percent += ACT_DOWNLOAD;
//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     name,
//                                                     Integer.toString(percent) + text,
//                                                     pi);

                     builder = new Notification.Builder(DownloadServiceNew.this);
                     builder.setContentTitle(title)
                            .setContentText(Integer.toString(percent) + text)
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.notify(task.id, notification);
                  }
               }
               bufferedInputStream.close();
               bufferedOutputStream.close();
               if(file.exists())
               {
                  file.delete();
               }
               tmpFile.renameTo(file);
               if(file.getName()
                      .toLowerCase()
                      .endsWith(".zip"))
               {
                  title = new StringBuilder(String.valueOf(getString(R.string.extract))).append(": ")
                                                                                        .append(task.name)
                                                                                        .toString();
//                  notification.setLatestEventInfo(DownloadService.this,
//                                                  name,
//                                                  "",
//                                                  pi);

                  builder = new Notification.Builder(DownloadServiceNew.this);
                  builder.setContentTitle(title)
                         .setContentText("")
                         .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                  notification = builder.getNotification();

                  nm.notify(task.id, notification);
                  LinkedList<File> files = new LinkedList();
                  ZipFile zipFile = new ZipFile(file);
                  Enumeration<? extends ZipEntry> entries = zipFile.entries();
                  while(entries.hasMoreElements())
                  {
                     ZipEntry e2 = (ZipEntry) entries.nextElement();
                     if(!e2.isDirectory())
                     {
                        file = new File("/sdcard/wordmate/" + e2.getName());
                        int size = (int) e2.getSize();
                        text = "% "
                              + file.getName()
                              + " "
                              + Integer.toString(size / 1000)
                              + "KB";
//                        notification.setLatestEventInfo(DownloadService.this,
//                                                        name,
//                                                        "0" + text,
//                                                        pi);

                        builder = new Notification.Builder(DownloadServiceNew.this);
                        builder.setContentTitle(title)
                               .setContentText("0" + text)
                               .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                        notification = builder.getNotification();

                        nm.notify(task.id, notification);
                        bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(e2), bufferSize);
                        dir = file.getParentFile();
                        if(!dir.exists())
                        {
                           dir.mkdirs();
                        }
                        tmpFile = File.createTempFile(file.getName(), null, dir);
                        bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
                        interval = size / 100;
                        if(interval < ACT_DOWNLOAD)
                        {
                           interval = ACT_DOWNLOAD;
                        }
                        percent = 0;
                        p = 0;
                        to = 0;
                        while(p < size)
                        {
                           to += interval;
                           if(to > size)
                           {
                              to = size;
                           }
                           while(p < to)
                           {
                              bufferedOutputStream.write(bufferedInputStream.read());
                              p += ACT_DOWNLOAD;
                           }
                           if(task.stop)
                           {
                              bufferedInputStream.close();
                              bufferedOutputStream.close();
                              zipFile.close();
                              tmpFile.delete();
                              return;
                           }
                           else if(percent != 100)
                           {
                              percent += ACT_DOWNLOAD;
//                              notification.setLatestEventInfo(DownloadService.this,
//                                                              name,
//                                                              Integer.toString(percent) + text,
//                                                              pi);

                              builder = new Notification.Builder(DownloadServiceNew.this);
                              builder.setContentTitle(title)
                                     .setContentText(Integer.toString(percent) + text)
                                     .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                              notification = builder.getNotification();

                              nm.notify(task.id, notification);
                           }
                        }
                        bufferedInputStream.close();
                        bufferedOutputStream.close();
                        files.add(file);
                        files.add(tmpFile);
                     }
                  }
                  zipFile.close();
                  Iterator<File> it = files.iterator();
                  while(it.hasNext())
                  {
                     File f = (File) it.next();
                     if(f.exists())
                     {
                        f.delete();
                     }
                     ((File) it.next()).renameTo(f);
                  }
               }
               intent2 = new Intent(DownloadServiceNew.this, WordMate.class);
               try
               {
                  intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  intent2.setAction(Long.toString(System.currentTimeMillis()));
                  pi = PendingIntent.getActivity(DownloadServiceNew.this,
                                                 0,
                                                 intent2,
                                                 0);
                  notification = new Notification(android.R.drawable.stat_sys_download_done,
                                                  task.name,
                                                  System.currentTimeMillis());
               }
               catch(Exception e3)
               {
                  e = e3;
//                  intent = intent2;
                  if(!task.stop)
                  {
                     notification = new Notification(android.R.drawable.stat_notify_error,
                                                     task.name,
                                                     System.currentTimeMillis());
                     notification.flags = 16;
//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     name,
//                                                     e.getMessage(),
//                                                     pi);

                     builder = new Notification.Builder(DownloadServiceNew.this);
                     builder.setContentTitle(title)
                            .setContentText(e.getMessage())
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.cancel(task.id);
                     nm.notify(task.id, notification);
                     handler.sendEmptyMessage(this.task.id);
                     return;
                  }
                  return;
               }
               Notification notification2;
               try
               {
                  notification.flags = 16;
//                  notification.setLatestEventInfo(DownloadService.this,
//                                                  task.name,
//                                                  getString(android.R.string.ok),
//                                                  pi);

                  builder = new Notification.Builder(DownloadServiceNew.this);
                  builder.setContentTitle(title)
                         .setContentText(getString(android.R.string.ok))
                         .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                  notification = builder.getNotification();

                  nm.cancel(task.id);
                  nm.notify(task.id, notification);
                  handler.sendEmptyMessage(task.id);
//                  intent = intent2;
                  notification2 = notification;
                  return;
               }
               catch(Exception e32)
               {
                  e = e32;
//                  intent = intent2;
                  notification2 = notification;
                  if(!task.stop)
                  {
                     notification = new Notification(android.R.drawable.stat_notify_error,
                                                     task.name,
                                                     System.currentTimeMillis());

                     notification.flags = Notification.FLAG_AUTO_CANCEL;

//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     name,
//                                                     e.getMessage(),
//                                                     pi);

                     builder = new Notification.Builder(DownloadServiceNew.this);
                     builder.setContentTitle(title)
                            .setContentText(e.getMessage())
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.cancel(task.id);
                     nm.notify(task.id, notification);
                     handler.sendEmptyMessage(task.id);
                     return;
                  }
                  return;
               }
            }
            throw new Exception(getString(R.string.connection_error));
         }
         catch(Exception e322)
         {
            e = e322;
         }
      }

   }

//   void showToast(int id)
//   {
//      Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
//   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT)
           .show();
   }
}
