package org.d1scw0rld.wordmatex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

public class Downloader extends Activity
{
   static final String baseUrl = "http://dictdownload.wordmate.net/";
   static final int done = 1;
   static final int idle = 0;
   static final int loading = 2;
   static final int retry = 3;
   DownloaderAdapter adapter;
   Document doc;
   Handler handler = new Handler()
   {
      public void handleMessage(Message msg)
      {
         onLoad();
      }
   };
   ArrayList<Item> items;
   ListView listView;
   int state;
   TextView textView;
   String url;

//   class C00051 extends Handler
//   {
//      C00051()
//      {}
//
//      public void handleMessage(Message msg)
//      {
//         Downloader.this.onLoad();
//      }
//   }

//   class C00062 implements OnItemClickListener
//   {
//      C00062()
//      {}
//
//      public void onItemClick(AdapterView<?> adapterView,
//                              View view,
//                              int position,
//                              long id)
//      {
//         Downloader.this.view(position);
//      }
//   }

   class DownloadThread extends Thread
   {
      DownloadThread()
      {
      }

      public void run()
      {
         try
         {
            doc = DocumentBuilderFactory.newInstance()
                                        .newDocumentBuilder()
                                        .parse(new URL(new StringBuilder(String.valueOf(url)).append(Integer.toString(items.size()))
                                                                                             .toString()).openStream());
         }
         catch(Exception e)
         {
         }
         handler.sendEmptyMessage(Downloader.idle);
      }
   }

   class DownloaderAdapter extends BaseAdapter
   {
      LayoutInflater inflater;

      class C00081 implements OnClickListener
      {
         C00081()
         {
         }

         public void onClick(View v)
         {
            load();
            adapter.notifyDataSetChanged();
         }
      }

      DownloaderAdapter()
      {
         inflater = LayoutInflater.from(Downloader.this);
      }

      public int getCount()
      {
         if(Downloader.this.state == Downloader.done)
         {
            return items.size();
         }
         return items.size() + Downloader.done;
      }

      public Object getItem(int position)
      {
         return Integer.valueOf(position);
      }

      public long getItemId(int position)
      {
         return (long) position;
      }

      public View getView(int position, View convertView, ViewGroup parent)
      {
         View view;
         if(position < items.size())
         {
            view = inflater.inflate(R.layout.downloader_list_item,
                                    parent,
                                    false);
            Item item = (Item) items.get(position);
            ((TextView) view.findViewById(R.id.title)).setText(item.title);
            ((TextView) view.findViewById(R.id.user)).setText(item.user);
            ((TextView) view.findViewById(R.id.size)).setText(String.valueOf(Integer.toString(item.size / 1000)) + "KB");
            ((TextView) view.findViewById(R.id.downloads)).setText(String.valueOf(Integer.toString(item.downloads)) + " " +
                                                                         getString(
                                                                               R.string.downloads));
            return view;
         }
         else if(Downloader.this.state == Downloader.retry)
         {
            view =
                  this.inflater.inflate(R.layout.downloader_list_item_retry,
                                        parent,
                                        false);
            ((Button) view.findViewById(R.id.retryButton)).setOnClickListener(new C00081());
            return view;
         }
         else
         {
            view =
                  this.inflater.inflate(R.layout.downloader_list_item_loading,
                                        parent,
                                        false);
            if(Downloader.this.state != 0)
            {
               return view;
            }
            load();
            return view;
         }
      }
   }

   class Item
   {
      int downloads;
      String file;
      int id;
      String info;
      int size;
      String title;
      String user;

      Item()
      {
      }
   }

   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.downloader);
      textView = (TextView) findViewById(R.id.textView);
      listView = (ListView) findViewById(R.id.listView);
      listView.setOnItemClickListener(new OnItemClickListener()
      {

         @Override
         public void onItemClick(AdapterView<?> adapterView,
                                 View view,
                                 int position,
                                 long id)
         {
            view(position);
         }
      });
      url = "http://dictdownload.wordmate.net/search?limit=10";
      try
      {
         int v = getPackageManager().getPackageInfo(getPackageName(), idle).versionCode;
         url += "&v=";
         url += Integer.toString(v);
      }
      catch(Exception e)
      {
      }
      search(null);
   }

   protected void onNewIntent(Intent intent)
   {
      search(intent.getStringExtra("query"));
   }

   void search(String q)
   {
      if(q == null)
      {
         q = new String();
      }
      if(q.length() > 0)
      {
         setTitle(getString(android.R.string.search_go) + ": " + q);
      }
      try
      {
         q = URLEncoder.encode(q, "UTF-8");
      }
      catch(Exception e)
      {
      }
      url += "&q=";
      url += q;
      url += "&start=";
      state = idle;
      items = new ArrayList();
      adapter = new DownloaderAdapter();
      listView.setAdapter(this.adapter);
      listView.setVisibility(idle);
      textView.setVisibility(View.GONE);
   }

   void load()
   {
      state = loading;
      doc = null;
      new DownloadThread().start();
   }

   void onLoad()
   {
      if(this.doc == null)
      {
         state = retry;
         adapter.notifyDataSetChanged();
         return;
      }
      Element root = this.doc.getDocumentElement();
      root.normalize();
      if(root.getTagName()
             .equals("message"))
      {
         showMessage(root.getFirstChild()
                         .getNodeValue());
         return;
      }
      NodeList nodeList = root.getElementsByTagName("dict");
      int length = nodeList.getLength();
      for(int i = idle; i < length; i += done)
      {
         Element e = (Element) nodeList.item(i);
         Item item = new Item();
         item.id = Integer.parseInt(e.getAttribute("id"));
         item.size = Integer.parseInt(e.getAttribute("size"));
         item.downloads = Integer.parseInt(e.getAttribute("downloads"));
         item.title = e.getElementsByTagName("title")
                       .item(idle)
                       .getFirstChild()
                       .getNodeValue();
         item.info = e.getElementsByTagName("info")
                      .item(idle)
                      .getFirstChild()
                      .getNodeValue();
         item.user = e.getElementsByTagName("user")
                      .item(idle)
                      .getFirstChild()
                      .getNodeValue();
         item.file = e.getElementsByTagName("file")
                      .item(idle)
                      .getFirstChild()
                      .getNodeValue();
         item.file = item.file.substring(item.file.lastIndexOf(47) + done);
         item.file = "/sdcard/wordmate/" + item.file;
         items.add(item);
      }
      if(length < 10)
      {
         state = done;
      }
      else
      {
         state = idle;
      }
      if(this.items.size() > 0)
      {
         adapter.notifyDataSetChanged();
      }
      else
      {
         showMessage(getString(R.string.no_result));
      }
   }

   void showMessage(String s)
   {
      listView.setVisibility(View.GONE);
      textView.setVisibility(idle);
      textView.setText(s);
   }

   void view(int position)
   {
      if(position < this.items.size())
      {
         Item item = (Item) this.items.get(position);
         Intent i = new Intent(this, DownloadService.class);
         i.putExtra("action", retry);
         i.putExtra("id", item.id);
         i.putExtra("size", item.size);
         i.putExtra("downloads", item.downloads);
         i.putExtra("title", item.title);
         i.putExtra("info", item.info);
         i.putExtra("user", item.user);
         i.putExtra("file", item.file);
         startService(i);
      }
   }

   public boolean onCreateOptionsMenu(Menu menu)
   {
      menu.add(idle, idle, idle, android.R.string.search_go)
          .setIcon(android.R.drawable.ic_search_category_default)
          .setAlphabeticShortcut('s');
      return super.onCreateOptionsMenu(menu);
   }

   public boolean onOptionsItemSelected(MenuItem item)
   {
      onSearchRequested();
      return super.onOptionsItemSelected(item);
   }
}
