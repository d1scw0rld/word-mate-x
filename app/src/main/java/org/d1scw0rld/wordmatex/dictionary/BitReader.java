package org.d1scw0rld.wordmatex.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BitReader
{
   byte bit;
   byte count = (byte) 0;
   byte currentByte;
   RandomAccessFile file;

   BitReader(RandomAccessFile file)
   {
      this.file = file;
   }

   void reset()
   {
      count = (byte) 0;
   }

   byte read() throws IOException
   {
      if(count == (byte) 0)
      {
         currentByte = file.readByte();
      }
      bit = (byte) ((currentByte >> (7 - count)) & 1);
      count = (byte) (count + 1);
      if(count > (byte) 7)
      {
         count = (byte) 0;
      }
      return bit;
   }
}
