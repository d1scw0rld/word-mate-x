package org.d1scw0rld.wordmatex.dictionary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class WMDict extends Dict
{
   BitReader bitReader;
   String charset;
   RandomAccessFile file;
   int offset;
   Node root;

   class Node
   {
      Node[] child = null;
      byte data;

      Node(byte data)
      {
         this.data = data;
      }

      Node(Node left, Node right)
      {
         child = new Node[2];
         child[0] = left;
         child[1] = right;
      }
   }

   public WMDict(RandomAccessFile file) throws IOException
   {
      this.file = file;
      bitReader = new BitReader(file);
      file.seek(8);
      int flags = file.readInt();
      ignoreCase = false;
      if((flags & 1) == 1)
      {
         ignoreCase = true;
      }
      charset = Charset.defaultCharset().displayName();
      if((flags & 256) == 256)
      {
         charset = "GBK";
      }
      wordCount = file.readInt();
      byte[] buffer = new byte[1024];
      int length = 0;
      while(length < 128)
      {
         byte c = (byte) (file.readByte() ^ -1);
         if(c == (byte) 0)
         {
            break;
         }
         buffer[length] = c;
         length++;
      }
      title = new String(buffer, 0, length, charset);
      length = 0;
      byte c;
      while(length < 1024)
      {
         c = (byte) (file.readByte() ^ -1);
         if(c == (byte) -1)
         {
            break;
         }
         buffer[length] = c;
         length++;
      }
      info = new String(buffer, 0, length, charset);
      root = createNode();
      offset = ((int) file.getFilePointer()) - 1;
   }

   String readWord(int index) throws IOException
   {
      return readString(index, (byte) 0);
   }

   String readDefinition(int index) throws IOException
   {
      String s = readString(index, (byte) -1);
      return s.substring(s.indexOf(0));
   }

   String readString(int index, byte suffix) throws IOException
   {
      file.seek((long) (offset + (index * 4)));
      file.seek((long) file.readInt());
      bitReader.reset();
      ByteBuffer buffer = ByteBuffer.allocate(1024);
      while(true)
      {
         byte c = readByte();
         if(c == suffix)
         {
            return new String(buffer.array(),
                              0,
                              buffer.position(),
                              charset);
         }
         if(buffer.position() == buffer.capacity())
         {
            ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
            newBuffer.put(buffer.array());
            buffer = newBuffer;
         }
         buffer.put(c);
      }
   }

   byte readByte() throws IOException
   {
      Node node = root;
      while(node.child != null)
      {
         node = node.child[bitReader.read()];
      }
      return node.data;
   }

   Node createNode() throws IOException
   {
      if(file.readByte() == (byte) 0)
      {
         byte data = file.readByte();
         file.skipBytes(1);
         return new Node(data);
      }
      file.skipBytes(2);
      return new Node(createNode(), createNode());
   }
}
