package org.d1scw0rld.wordmatex;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;

import org.d1scw0rld.wordmatex.dictionary.Dict;

public class WordListAdapterNew extends RecyclerView.Adapter<WordListAdapterNew.ViewHolder>
{
   private Dict dict;

   private IError err;

   private View.OnClickListener onClickListener = null;

   private OnItemClickListener onItemClickListener = null;

   public WordListAdapterNew(Dict dict, IError err)
   {
      this.dict = dict;
      this.err = err;
   }

   public void setOnClickListener(View.OnClickListener onClickListener)
   {
      this.onClickListener = onClickListener;
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

      return new ViewHolder(v);
   }

   @Override
   public void onBindViewHolder(@NonNull ViewHolder holder, int position)
   {
      try
      {
         holder.textView.setText(dict.getWord(position));
//         holder.view.setOnClickListener(onItemClickListener);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
   }

//   public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
   public class ViewHolder extends RecyclerView.ViewHolder
   {
      TextView textView;
      View view;

      public ViewHolder(View itemView)
      {
         super(itemView);
         textView = (TextView) itemView.findViewById(android.R.id.text1);
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

//      @Override
//      public void onClick(View view)
//      {
//         if(onItemClickListener != null)
//         {
//            onItemClickListener.OnItemClick(view, getLayoutPosition());
//         }
//      }
   }

   public interface OnItemClickListener
   {
      void OnItemClick(View view, int pos);
   }
}
