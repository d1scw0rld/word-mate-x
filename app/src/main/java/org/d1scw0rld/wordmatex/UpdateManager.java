package org.d1scw0rld.wordmatex;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import java.net.URL;
import java.util.Calendar;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public class UpdateManager
{
   Handler handler = new Handler()
   {      
      public void handleMessage(Message msg)
      {
         onUpdate();
      }
   };
   WordMate wm;

   class Runner extends Thread
   {
      Runner()
      {}

      public void run()
      {
         try
         {
            Element root = DocumentBuilderFactory.newInstance()
                                                 .newDocumentBuilder()
                                                 .parse(new URL("http://update.wordmate.net/?v="
                                                              + Integer.toString(wm.getPackageManager()
                                                                                   .getPackageInfo(wm.getPackageName(), 0).versionCode)).openStream())
                                                 .getDocumentElement();
            root.normalize();
            if(root.getElementsByTagName("update")
                   .item(0)
                   .getFirstChild()
                   .getNodeValue()
                   .equals("Y"))
            {
               handler.sendEmptyMessage(0);
            }
         }
         catch(Exception e)
         {}
      }
   }

   UpdateManager(WordMate wm)
   {
      this.wm = wm;
      if(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != wm.getPref("Update", Context.MODE_PRIVATE))
      {
         new Runner().start();
      }
   }

   void onUpdate()
   {
      wm.putPref("Update", Calendar.getInstance().get(6));
      if(wm.getPref("Notify", true))
      {
         new Builder(wm).setIcon(R.drawable.icon)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.update)
                        .setPositiveButton(R.string.yes, new OnClickListener()
                        {
                           
                           @Override
                           public void onClick(DialogInterface dialog, int which)
                           {
                              Intent i = new Intent();
                              i.setAction("android.intent.action.VIEW");
                              i.setData(Uri.parse("http://market.android.com/search?q=WordMate"));
                              wm.startActivity(i);
                              
                           }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
      }
   }
}
