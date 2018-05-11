package org.d1scw0rld.wordmatex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.d1scw0rld.wordmatex.dictionary.Dict;

import java.io.IOException;

public class WordlistAdapter extends BaseAdapter
{
   Dict dict;
   LayoutInflater inflater;
   WordMate wm;

   WordlistAdapter(WordMate wm)
   {
      this.wm = wm;
      dict = wm.dicts[wm.currentDict];
      inflater = LayoutInflater.from(wm);
   }

   public int getCount()
   {
      return dict.wordCount;
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
      TextView textView;
      if(convertView == null)
      {
         textView = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
      }
      else
      {
         textView = (TextView) convertView;
      }
      try
      {
         textView.setText(dict.getWord(position));
      }
      catch(IOException e)
      {
         wm.showError(e.getMessage());
      }
      return textView;
   }
}
