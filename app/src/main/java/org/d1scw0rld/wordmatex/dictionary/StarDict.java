package org.d1scw0rld.wordmatex.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;

public class StarDict extends Dict
{
   static final String newline = new String(new byte[] { (byte) 10 });
   RandomAccessFile definitionFile;
   RandomAccessFile indexFile;
   RandomAccessFile wordFile;

   public StarDict(RandomAccessFile indexFile,
                   RandomAccessFile wordFile,
                   RandomAccessFile definitionFile,
                   String title,
                   String info) throws IOException
   {
      this.indexFile = indexFile;
      this.wordFile = wordFile;
      this.definitionFile = definitionFile;
      this.title = title;
      this.info = info;
      wordCount = (int) (indexFile.length() / 4);
      ignoreCase = true;
   }

   String readWord(int index) throws IOException
   {
      byte[] buffer = new byte[256];
      indexFile.seek((long) (index * 4));
      wordFile.seek((long) indexFile.readInt());
      int i = 0;
      while(i <= 256)
      {
         buffer[i] = wordFile.readByte();
         if(buffer[i] == (byte) 0)
         {
            break;
         }
         i++;
      }
      return new String(buffer, 0, i);
   }

   String readDefinition(int index) throws IOException
   {
      readWord(index);
      definitionFile.seek((long) wordFile.readInt());
      int length = wordFile.readInt();
      byte[] buffer = new byte[length];
      for(int i = 0; i < length; i++)
      {
         buffer[i] = definitionFile.readByte();
      }
      return new String(buffer).replace(newline, "<br>");
   }
}
