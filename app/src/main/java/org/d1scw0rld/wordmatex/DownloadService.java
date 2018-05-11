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

import static java.lang.Integer.*;

public class DownloadService extends Service
{
   static final int bufferSize = 65536;
   static final int cancel = 2;
   static final int download = 1;
   static final int view = 3;
   Handler handler = new Handler()
   {

      public void handleMessage(Message msg)
      {
         done(msg.what);
      }
   };
   NotificationManager nm;
   
   TreeMap<Integer, Task> tasks;

   class Runner extends Thread
   {
      Task f0t;

      Runner(Task t)
      {
         this.f0t = t;
      }

      public void run()
      {
         Exception e;
//         Intent intent;
         Notification notification = new Notification(android.R.drawable.stat_sys_download,
                                                            f0t.title,
                                                            System.currentTimeMillis());
         notification.flags = DownloadService.cancel;
         Intent intent2 = new Intent(DownloadService.this, DownloadService.class);
         intent2.putExtra("action", DownloadService.view);
         intent2.putExtra("id", f0t.id);
         intent2.putExtra("size", f0t.size);
         intent2.putExtra("downloads", f0t.downloads);
         intent2.putExtra("title", f0t.title);
         intent2.putExtra("info", f0t.info);
         intent2.putExtra("user", f0t.user);
         intent2.putExtra("file", f0t.file);
         intent2.setAction(Long.toString(System.currentTimeMillis()));
         PendingIntent pi = PendingIntent.getService(DownloadService.this,
                                                     0,
                                                     intent2,
                                                     0);
         String title = String.valueOf(getString(R.string.download)) + ": " + f0t.title;
//         notification.setLatestEventInfo(DownloadService.this, title, "", pi);
         Notification.Builder builder = new Notification.Builder(DownloadService.this);
         builder.setContentTitle(title);
         builder.setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
         notification = builder.getNotification();

         nm.notify(f0t.id, notification);
         try
         {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://dictdownload.wordmate.net/download?id="
                                     + Integer.toString(f0t.id)).openConnection();
            if(conn.getResponseCode() == 200 && conn.getContentLength() == f0t.size)
            {
               BufferedInputStream bufferedInputStream = new BufferedInputStream(conn.getInputStream(), bufferSize);
               File file = new File(f0t.file);
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
                             + Integer.toString(f0t.size / 1000)
                             + "KB";
//               notification.setLatestEventInfo(DownloadService.this,
//                                               title,
//                                               "0" + text,
//                                               pi);

               builder = new Notification.Builder(DownloadService.this);
               builder.setContentTitle(title)
                      .setContentText("0" + text)
                      .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
               notification = builder.getNotification();

               nm.notify(f0t.id, notification);
               int interval = f0t.size / 100;
               if(interval < download)
               {
                  interval = download;
               }
               int percent = 0;
               int p = 0;
               int to = 0;
               while(p < this.f0t.size)
               {
                  to += interval;
                  if(to > this.f0t.size)
                  {
                     to = this.f0t.size;
                  }
                  while(p < to)
                  {
                     bufferedOutputStream.write(bufferedInputStream.read());
                     p += download;
                  }
                  if(this.f0t.stop)
                  {
                     bufferedInputStream.close();
                     bufferedOutputStream.close();
                     tmpFile.delete();
                     return;
                  }
                  else if(percent != 100)
                  {
                     percent += download;
//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     title,
//                                                     Integer.toString(percent) + text,
//                                                     pi);

                     builder = new Notification.Builder(DownloadService.this);
                     builder.setContentTitle(title)
                            .setContentText(Integer.toString(percent) + text)
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.notify(f0t.id, notification);
                  }
               }
               bufferedInputStream.close();
               bufferedOutputStream.close();
               if(file.exists())
               {
                  file.delete();
               }
               tmpFile.renameTo(file);
               if(file.getName().toLowerCase().endsWith(".zip"))
               {
                  title = new StringBuilder(String.valueOf(getString(R.string.extract))).append(": ")
                                                                                        .append(f0t.title)
                                                                                        .toString();
//                  notification.setLatestEventInfo(DownloadService.this,
//                                                  title,
//                                                  "",
//                                                  pi);

                  builder = new Notification.Builder(DownloadService.this);
                  builder.setContentTitle(title)
                         .setContentText("")
                         .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                  notification = builder.getNotification();

                  nm.notify(f0t.id, notification);
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
//                                                        title,
//                                                        "0" + text,
//                                                        pi);

                        builder = new Notification.Builder(DownloadService.this);
                        builder.setContentTitle(title)
                               .setContentText("0" + text)
                               .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                        notification = builder.getNotification();

                        nm.notify(f0t.id, notification);
                        bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(e2), bufferSize);
                        dir = file.getParentFile();
                        if(!dir.exists())
                        {
                           dir.mkdirs();
                        }
                        tmpFile = File.createTempFile(file.getName(), null, dir);
                        bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tmpFile),  bufferSize);
                        interval = size / 100;
                        if(interval < download)
                        {
                           interval = download;
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
                              p += download;
                           }
                           if(f0t.stop)
                           {
                              bufferedInputStream.close();
                              bufferedOutputStream.close();
                              zipFile.close();
                              tmpFile.delete();
                              return;
                           }
                           else if(percent != 100)
                           {
                              percent += download;
//                              notification.setLatestEventInfo(DownloadService.this,
//                                                              title,
//                                                              Integer.toString(percent) + text,
//                                                              pi);

                              builder = new Notification.Builder(DownloadService.this);
                              builder.setContentTitle(title)
                                     .setContentText(Integer.toString(percent) + text)
                                     .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                              notification = builder.getNotification();

                              nm.notify(f0t.id, notification);
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
               intent2 = new Intent(DownloadService.this, WordMate.class);
               try
               {
                  intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  intent2.setAction(Long.toString(System.currentTimeMillis()));
                  pi = PendingIntent.getActivity(DownloadService.this,
                                                 0,
                                                 intent2,
                                                 0);
                  notification = new Notification(android.R.drawable.stat_sys_download_done,
                                                  f0t.title,
                                                  System.currentTimeMillis());
               }
               catch(Exception e3)
               {
                  e = e3;
//                  intent = intent2;
                  if(!f0t.stop)
                  {
                     notification = new Notification(android.R.drawable.stat_notify_error,
                                                     f0t.title,
                                                     System.currentTimeMillis());
                     notification.flags = 16;
//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     title,
//                                                     e.getMessage(),
//                                                     pi);

                     builder = new Notification.Builder(DownloadService.this);
                     builder.setContentTitle(title)
                            .setContentText(e.getMessage())
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.cancel(f0t.id);
                     nm.notify(f0t.id, notification);
                     handler.sendEmptyMessage(this.f0t.id);
                     return;
                  }
                  return;
               }
               Notification notification2;
               try
               {
                  notification.flags = 16;
//                  notification.setLatestEventInfo(DownloadService.this,
//                                                  f0t.title,
//                                                  getString(android.R.string.ok),
//                                                  pi);

                  builder = new Notification.Builder(DownloadService.this);
                  builder.setContentTitle(title)
                         .setContentText(getString(android.R.string.ok))
                         .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                  notification = builder.getNotification();

                  nm.cancel(f0t.id);
                  nm.notify(f0t.id, notification);
                  handler.sendEmptyMessage(f0t.id);
//                  intent = intent2;
                  notification2 = notification;
                  return;
               }
               catch(Exception e32)
               {
                  e = e32;
//                  intent = intent2;
                  notification2 = notification;
                  if(!f0t.stop)
                  {
                     notification = new Notification(android.R.drawable.stat_notify_error,
                                                     f0t.title,
                                                     System.currentTimeMillis());
                     
                     notification.flags = Notification.FLAG_AUTO_CANCEL;
                     
//                     notification.setLatestEventInfo(DownloadService.this,
//                                                     title,
//                                                     e.getMessage(),
//                                                     pi);

                     builder = new Notification.Builder(DownloadService.this);
                     builder.setContentTitle(title)
                            .setContentText(e.getMessage())
                            .setContentIntent(pi);
//         builder.build(); // TODO Check and fix it. Require API 16
                     notification = builder.getNotification();

                     nm.cancel(f0t.id);
                     nm.notify(f0t.id, notification);
                     handler.sendEmptyMessage(f0t.id);
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

   class Task
   {
      int downloads;
      String file;
      int id;
      String info;
      int size;
      boolean stop;
      String title;
      String user;

      Task()
      {}
   }

   public void onCreate()
   {
      tasks = new TreeMap();
      nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
   }

   public IBinder onBind(Intent intent)
   {
      return null;
   }

   public void onStart(Intent intent, int startId)
   {
      String str = "info";
      String str2 = "file";
      String str3 = "downloads";
      int id = intent.getIntExtra("id", 0);
      Task t;
      String str4;
      switch (intent.getIntExtra("action", 0))
      {
         case download /* 1 */:
            if(this.tasks.get(valueOf(id)) == null)
            {
               t = new Task();
               t.id = id;
               t.size = intent.getIntExtra("size", 0);
               str4 = "downloads";
               t.downloads = intent.getIntExtra(str3, 0);
               t.title = intent.getStringExtra("title");
               str4 = "info";
               t.info = intent.getStringExtra(str);
               t.user = intent.getStringExtra("user");
               str4 = "file";
               t.file = intent.getStringExtra(str2);
               this.tasks.put(id, t);
               showToast(getString(R.string.download) + ": " + t.title);
               new Runner(t).start();
               break;
            }
         break;
         case cancel /* 2 */:
            t = (Task) this.tasks.get(valueOf(id));
            if(t != null)
            {
               t.stop = true;
               this.nm.cancel(id);
               this.tasks.remove(valueOf(id));
               showToast(getString(R.string.cancel_download)
                         + ": "
                         + t.title);
               break;
            }
         break;
         case view /* 3 */:
            Intent i = new Intent(this, DownloadViewer.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("id", id);
            i.putExtra("size", intent.getIntExtra("size", 0));
//            str4 = "downloads";
            i.putExtra(str3, intent.getIntExtra(str3, 0));
            i.putExtra("title", intent.getStringExtra("title"));
//            str4 = "info";
            i.putExtra(str, intent.getStringExtra(str));
            i.putExtra("user", intent.getStringExtra("user"));
//            str4 = "file";
            i.putExtra(str2, intent.getStringExtra(str2));
            if(tasks.get(id) != null)
            {
               i.putExtra("isDownloading", true);
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

//   void showToast(int id)
//   {
//      Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
//   }

   void showToast(String s)
   {
      Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
   }
}
