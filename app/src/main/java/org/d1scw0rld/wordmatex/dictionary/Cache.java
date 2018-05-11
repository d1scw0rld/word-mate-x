package org.d1scw0rld.wordmatex.dictionary;

import java.io.IOException;

public class Cache
{
   static final int maxSteps = 1000000000;
   Dict dict;
   int[] hit;
   int size;
   int[] idx = null;
   int[] step;
   int steps;
   String[] word;

   Cache(Dict dict)
   {
      this.dict = dict;
      size = (((int) (Math.log((double) dict.wordCount) / Math.log(2.0d))) + 1) * 2;
      idx = new int[size];
      word = new String[size];
      for(int i = 0; i < size; i++)
      {
         idx[i] = -1;
      }
      reset();
   }

   void reset()
   {
      steps = 0;
      step = new int[size];
      hit = new int[size];
   }

   float hitRate(int slot)
   {
      return ((float) hit[slot]) / ((float) (steps - step[slot]));
   }

   String get(int index) throws IOException
   {
      if(steps == maxSteps)
      {
         reset();
      }
      steps++;
      int min = 0;
      float minRate = hitRate(0);
      for(int i = 0; i < size; i++)
      {
         if(idx[i] == index)
         {
            int[] iArr = hit;
            iArr[i] = iArr[i] + 1;
            return word[i];
         }
         float rate = hitRate(i);
         if(rate < minRate)
         {
            min = i;
            minRate = rate;
         }
      }
      step[min] = steps;
      hit[min] = 1;
      idx[min] = index;
      word[min] = dict.readWord(index);
      return word[min];
   }
}
