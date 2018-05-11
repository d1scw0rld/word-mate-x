package org.d1scw0rld.wordmatex.dictionary;

import java.io.IOException;

public abstract class Dict
{
   Cache cache;
   String id;
   boolean ignoreCase;
   public String info;
   public String title;
   public int wordCount;

   abstract String readDefinition(int i) throws IOException;

   abstract String readWord(int i) throws IOException;

   public void init(String id)
   {
      this.id = id;
      cache = new Cache(this);
   }

   public boolean equals(Dict dict)
   {
      return id.equals(dict.id);
   }

   public String getWord(int index) throws IOException
   {
      return cache.get(index);
   }

   public String getDefinition(int index) throws IOException
   {
      return readDefinition(index);
   }

   public int query(String word) throws IOException
   {
      if(word == null || word.length() == 0)
      {
         return 0;
      }
      if(ignoreCase)
      {
         return queryIgnoreCase(word);
      }
      int min = 0;
      int max = wordCount - 1;
      while(min + 1 < max)
      {
         int index = ((max - min) / 2) + min;
         int compare = word.compareTo(getWord(index));
         if(compare < 0)
         {
            max = index;
         }
         else if(compare <= 0)
         {
            return index;
         }
         else
         {
            min = index;
         }
      }
      String minWord = getWord(min);
      String maxWord = getWord(max);
      int length = word.length();
      int minLength = minWord.length();
      int i = 0;
      while(i < length)
      {
         char c = word.charAt(i);
         if(c < maxWord.charAt(i))
         {
            return min;
         }
         if(i == minLength || c > minWord.charAt(i))
         {
            return max;
         }
         i++;
      }
      return 0;
   }

   int queryIgnoreCase(String word) throws IOException
   {
      int min = 0;
      int max = wordCount - 1;
      while(min + 1 < max)
      {
         int index = ((max - min) / 2) + min;
         int compare = word.compareToIgnoreCase(getWord(index));
         if(compare < 0)
         {
            max = index;
         }
         else if(compare <= 0)
         {
            return index;
         }
         else
         {
            min = index;
         }
      }
      word = word.toLowerCase();
      String minWord = getWord(min).toLowerCase();
      String maxWord = getWord(max).toLowerCase();
      int length = word.length();
      int minLength = minWord.length();
      int i = 0;
      while(i < length)
      {
         char c = word.charAt(i);
         if(c < maxWord.charAt(i))
         {
            return min;
         }
         if(i == minLength || c > minWord.charAt(i))
         {
            return max;
         }
         i++;
      }
      return 0;
   }
}
