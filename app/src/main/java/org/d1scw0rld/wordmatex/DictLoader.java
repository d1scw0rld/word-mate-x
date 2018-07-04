package org.d1scw0rld.wordmatex;

import android.app.ProgressDialog;
import android.content.Context;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.TreeMap;

import org.d1scw0rld.wordmatex.dictionary.Dict;
import org.d1scw0rld.wordmatex.dictionary.StarDict;
import org.d1scw0rld.wordmatex.dictionary.WMDict;

class DictLoader
{
   private static final int bufferSize = 65536;
   private int                   currentFile;
   private ProgressDialog        dialog;
   private TreeMap<String, Dict> dicts;
   private ArrayList<File>       files;

   static class LoaderHandler extends Handler
   {
      private final WeakReference<DictLoader> dictLoaderWeakReference;

      LoaderHandler(DictLoader dictLoader)
      {
         dictLoaderWeakReference = new WeakReference<>(dictLoader);
      }

      public void handleMessage(Message msg)
      {
         DictLoader dictLoader = dictLoaderWeakReference.get();
         if(dictLoader == null)
         {
            return;
         }
         if(dictLoader.dialog != null)
         {
            dictLoader.dialog.dismiss();
            dictLoader.dialog = null;
         }
         dictLoader.currentFile += msg.what;
         if(dictLoader.currentFile < dictLoader.files.size())
         {
            File file = dictLoader.files.get(dictLoader.currentFile);
            if(file.getName()
                   .endsWith(".dwm"))
            {
               dictLoader.loadWMDict(file);
               return;
            }
            else
            {
               dictLoader.loadStarDict(file);
               return;
            }
         }
         dictLoader.done();
      }
   }

   private LoaderHandler handler;

   private boolean stop;

   private IWordMate wm;

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
                                                                             DictLoader.bufferSize));
            File tmpFile = File.createTempFile(this.indexFile.getName(),
                                               null,
                                               this.indexFile.getParentFile());
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile, false),
                                                                                 DictLoader.bufferSize));
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
                  if(!tmpFile.delete())
                  {
                     throw new IOException("The temporary can't be delete!");
                  }
                  return;
               }
            }
            in.close();
            out.close();
            if(!tmpFile.renameTo(indexFile))
            {
               throw new IOException("The temporary file can't be rename");
            }
            handler.sendEmptyMessage(0);
         }
         catch(IOException e)
         {
            wm.showError(e.getMessage());
            handler.sendEmptyMessage(1);
         }
      }
   }

   DictLoader(IWordMate wm)
   {
      this.wm = wm;
      dicts = new TreeMap<>();
      files = new ArrayList<>();
      File dir = new File(WordMateX.FILES_PATH);

      handler = new LoaderHandler(this);

      try
      {
         if(!dir.exists())
         {
            if(!dir.mkdirs())
            {
               throw new IOException("The directory can't be created");
            }
         }

         File[] fileList = dir.listFiles();
         if(fileList != null)
         {
            loadDir(fileList);
         }
         handler.sendEmptyMessage(0);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
   }

   private void done()
   {
      wm.onLoad(dicts.values()
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

   private void loadDir(File[] fileList)
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
               files.add(file);
            }
            else if(name.endsWith(".dict.dz"))
            {
               File dictFile = new File(name.substring(0, name.length() - 3));
               File idxFile = new File(name.substring(0, name.length() - 7) + "idx");
               if(!dictFile.exists() && idxFile.exists())
               {
                  wm.showAlert(name, R.string.stardict_dz);
               }
            }
         }
      }
   }

   private void loadWMDict(File file)
   {
      try
      {
         String name = file.getAbsolutePath();
         RandomAccessFile f = new RandomAccessFile(file, "r");
         if(f.length() < 20)
         {
            wm.showAlert(name, R.string.error_format);
         }
         else
         {
            f.skipBytes(4);
            if(f.readInt() > 19999999)
            {
               wm.showAlert(name, R.string.error_format_version);
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

   private void loadStarDict(File file)
   {
      String path = file.getAbsolutePath();
      path = path.substring(0, path.length() - 4);
      String title = file.getName();
      title = title.substring(0, title.length() - 5);
      String info = "";
      File wordFile = new File(path + "idx");
      if(wordFile.exists())
      {
         File infoFile = new File(path + "ifo");
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
            {
               e.printStackTrace();
            }
         }
         File indexFile = new File(path + "ids");
         if(indexFile.exists())
         {
            try
            {
               Dict dict = new StarDict(new RandomAccessFile(indexFile, "r"),
                                        new RandomAccessFile(wordFile, "r"),
                                        new RandomAccessFile(file, "r"),
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
         dialog.setMessage(wm.getContext()
                             .getString(R.string.stardict_index));
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
