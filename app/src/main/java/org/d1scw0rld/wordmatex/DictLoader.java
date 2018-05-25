package org.d1scw0rld.wordmatex;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.TreeMap;

import org.d1scw0rld.wordmatex.dictionary.Dict;
import org.d1scw0rld.wordmatex.dictionary.StarDict;
import org.d1scw0rld.wordmatex.dictionary.WMDict;

class DictLoaderNew
{
   static final int bufferSize = 65536;
   private int currentFile;
   private ProgressDialog dialog;
   private TreeMap<String, Dict> dicts;
   private ArrayList files;
//   Handler handler = new C00001();
   private Handler handler = new Handler()
   {
      public void handleMessage(Message msg)
      {
         if(dialog != null)
         {
            dialog.dismiss();
            dialog = null;
         }
         DictLoaderNew dictLoader = DictLoaderNew.this;
         dictLoader.currentFile += msg.what;
         if(DictLoaderNew.this.currentFile < files.size())
         {
            File file = (File) files.get(currentFile);
            if(file.getName().endsWith(".dwm"))
            {
               loadWMDict(file);
               return;
            }
            else
            {
               loadStarDict(file);
               return;
            }
         }
         done();
      }
   };
   boolean stop;
   IWordMate wm;

//   class C00001 extends Handler
//   {
//      C00001()
//      {}
//
//      public void handleMessage(Message msg)
//      {
//         if(DictLoader.this.dialog != null)
//         {
//            DictLoader.this.dialog.dismiss();
//            DictLoader.this.dialog = null;
//         }
//         DictLoader dictLoader = DictLoader.this;
//         dictLoader.currentFile += msg.what;
//         if(DictLoader.this.currentFile < DictLoader.this.files.size())
//         {
//            File file =
//                      (File) DictLoader.this.files.get(DictLoader.this.currentFile);
//            if(file.getName().endsWith(".dwm"))
//            {
//               DictLoader.this.loadWMDict(file);
//               return;
//            }
//            else
//            {
//               DictLoader.this.loadStarDict(file);
//               return;
//            }
//         }
//         DictLoader.this.done();
//      }
//   }

   class StarDictIndexGenerator extends Thread
   {
      File indexFile;
      File wordFile;

      StarDictIndexGenerator(File indexFile, File wordFile)
      {
         this.indexFile = indexFile;
         this.wordFile = wordFile;
      }

      public void run()
      {
         try
         {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(this.wordFile),
                                                                             DictLoaderNew.bufferSize));
            File tmpFile = File.createTempFile(this.indexFile.getName(),
                                               null,
                                               this.indexFile.getParentFile());
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile, false),
                                                                                 DictLoaderNew.bufferSize));
            int length = (int) this.wordFile.length();
            int interval = length / 100;
            int progress = 0;
            int position = 0;
            while(position < length)
            {
               out.writeInt(position);
               do
               {
                  position++;
               } while(in.readByte() != (byte) 0);
               position += 8;
               in.skipBytes(8);
               if(progress + interval < position)
               {
                  progress = position;
                  dialog.setProgress(progress / interval);
               }
               if(position + 8 > length)
               {
                  break;
               }
               else if(stop)
               {
                  in.close();
                  out.close();
                  tmpFile.delete();
                  return;
               }
            }
            in.close();
            out.close();
            tmpFile.renameTo(indexFile);
            handler.sendEmptyMessage(0);
         }
         catch(IOException e)
         {
            wm.showError(e.getMessage());
            handler.sendEmptyMessage(1);
         }
      }
   }

   DictLoaderNew(IWordMate wm)
   {
      this.wm = wm;
      dicts = new TreeMap();
      files = new ArrayList();
//      File dir = new File("/sdcard/wordmate/");
//      File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/wordmate/");
      File dir = new File(WordMateX.FILES_PATH);
//      File dir = new File(Environment.getDataDirectory().getPath() + "/wordmate/");

      if(!dir.exists())
      {
         dir.mkdirs();
      }
      File[] fileList = dir.listFiles();
      if(fileList != null)
      {
         loadDir(fileList);
      }
      handler.sendEmptyMessage(0);
   }

   void done()
   {
      this.wm.onLoad((Dict[]) this.dicts.values()
                                        .toArray(new Dict[dicts.size()]));
   }

   void stop()
   {
      stop = true;
      if(dialog != null)
      {
         dialog.dismiss();
      }
   }

   void loadDir(File[] fileList)
   {
      for(File file : fileList)
      {
         if(file.isDirectory())
         {
            loadDir(file.listFiles());
         }
         else
         {
            String name = file.getAbsolutePath();
            if(name.endsWith(".dwm") || name.endsWith(".dict"))
            {
               this.files.add(file);
            }
            else if(name.endsWith(".dict.dz"))
            {
               File dictFile = new File(name.substring(0, name.length() - 3));
               File idxFile = new File(name.substring(0, name.length() - 7) + "idx");
               if(!dictFile.exists() && idxFile.exists())
               {
                  wm.showAlert(name, (int) R.string.stardict_dz);
               }
            }
         }
      }
   }

   void loadWMDict(File file)
   {
      try
      {
         String name = file.getAbsolutePath();
         RandomAccessFile f = new RandomAccessFile(file, "r");
         if(f.length() < 20)
         {
            wm.showAlert(name, (int) R.string.error_format);
         }
         else
         {
            f.skipBytes(4);
            if(f.readInt() > 19999999)
            {
               wm.showAlert(name, (int) R.string.error_format_version);
            }
            else
            {
               Dict dict = new WMDict(f);
               dict.init(name);
               dicts.put(dict.title, dict);
            }
         }
      }
      catch(IOException e)
      {
         wm.showError(e.getMessage());
      }
      handler.sendEmptyMessage(1);
   }

   void loadStarDict(File file)
   {
      File dictFile = file;
      String path = dictFile.getAbsolutePath();
      path = path.substring(0, path.length() - 4);
      String title = dictFile.getName();
      title = title.substring(0, title.length() - 5);
      String info = new String();
      File wordFile = new File(new StringBuilder(String.valueOf(path)).append("idx").toString());
      if(wordFile.exists())
      {
         File infoFile = new File(new StringBuilder(String.valueOf(path)).append("ifo").toString());
         if(infoFile.exists())
         {
            try
            {
               BufferedReader reader = new BufferedReader(new FileReader(infoFile));
               while(true)
               {
                  String line = reader.readLine();
                  if(line == null)
                  {
                     break;
                  }
                  if(line.startsWith("bookname="))
                  {
                     title = line.substring(9);
                  }
                  if(line.startsWith("description="))
                  {
                     info = line.substring(12);
                  }
               }
            }
            catch(IOException e)
            {}
         }
         File indexFile = new File(new StringBuilder(String.valueOf(path)).append("ids").toString());
         if(indexFile.exists())
         {
            try
            {
               Dict dict = new StarDict(new RandomAccessFile(indexFile, "r"),
                                        new RandomAccessFile(wordFile, "r"),
                                        new RandomAccessFile(dictFile, "r"),
                                        title,
                                        info);
               dict.init(file.getAbsolutePath());
               this.dicts.put(title, dict);
            }
            catch(IOException e2)
            {
               wm.showError(e2.getMessage());
            }
            handler.sendEmptyMessage(1);
            return;
         }
         dialog = new ProgressDialog(this.wm.getContext());
         dialog.setTitle(title);
         dialog.setMessage(wm.getContext().getString(R.string.stardict_index));
         dialog.setCancelable(false);
         dialog.setProgressStyle(1);
         dialog.show();
         new StarDictIndexGenerator(indexFile, wordFile).start();
         return;
      }
      handler.sendEmptyMessage(1);
   }

   public interface IWordMate extends IError
   {
      void onLoad(Dict[] dicts);

      void showAlert(String name, int error_format);

      Context getContext();
   }
}
