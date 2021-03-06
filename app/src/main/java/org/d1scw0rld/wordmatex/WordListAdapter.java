package org.d1scw0rld.wordmatex;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;

import org.d1scw0rld.wordmatex.dictionary.Dict;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.ViewHolder>
{
   private Dict dict;

   private Context context;

   private OnItemClickListener onItemClickListener = null;

   private WordListAdapter(Dict dict)
   {
      this.dict = dict;
   }

   WordListAdapter(Context context, Dict dict)
   {
      this(dict);
      this.context = context;
   }

   public void setOnItemClickListener(OnItemClickListener onItemClickListener)
   {
      this.onItemClickListener = onItemClickListener;
   }

   @Override
   public int getItemCount()
   {
      return dict.wordCount;
   }

   @NonNull
   @Override
   public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
   {
      View v = LayoutInflater.from(parent.getContext())
                             .inflate(android.R.layout.simple_list_item_1, parent, false);

      TypedValue outValue = new TypedValue();
      context.getTheme()
             .resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
      v.setBackgroundResource(outValue.resourceId);

      return new ViewHolder(v);
   }

   @Override
   public void onBindViewHolder(@NonNull ViewHolder holder, int position)
   {
      try
      {
         holder.textView.setText(dict.getWord(position));
//         holder.ACT_VIEW.setOnClickListener(onItemClickListener);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
   }

   class ViewHolder extends RecyclerView.ViewHolder
   {
      TextView textView;
      View     view;

      ViewHolder(View itemView)
      {
         super(itemView);
         textView = itemView.findViewById(android.R.id.text1);
         view = itemView;
         view.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(View view)
            {
               onItemClickListener.OnItemClick(view, getLayoutPosition());
            }
         });
      }
   }

   public interface OnItemClickListener
   {
      void OnItemClick(View view, int pos);
   }
}
