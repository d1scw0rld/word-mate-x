package org.d1scw0rld.wordmatex;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

class DictInfo implements Parcelable
{
   //      private long id;
   private long words;

   private long size;

   private String name,
         info,
         url,
         file;

   DictInfo(DictInfo dictInfo)
   {
      name = dictInfo.getName();
      info = dictInfo.getInfo();
      words = dictInfo.getWords();
      size = dictInfo.getSize();
      url = dictInfo.getUrl();
      file = dictInfo.getFile();
   }

   DictInfo(JSONObject joDictInfo) throws JSONException
   {
//         id = joDictInfo.getInt("id");
      name = joDictInfo.getString("title");
      info = joDictInfo.getString("info");
      words = joDictInfo.getInt("words");
      size = joDictInfo.getInt("size");
      url = joDictInfo.getString("url");
      if(joDictInfo.has("file"))
      {
         file = joDictInfo.getString("file");
      }
      else
      {
         file = url.substring(url.lastIndexOf("/"), url.lastIndexOf("?"));
      }
   }

//      public long getId()
//      {
//         return id;
//      }

   public int getId()
   {
      return url.hashCode();
   }

   public String getName()
   {
      return name;
   }

   public String getInfo()
   {
      return info;
   }

   public long getWords()
   {
      return words;
   }

   public long getSize()
   {
      return size;
   }

   public String getUrl()
   {
      return url;
   }

   public DictInfo(Parcel in)
   {
      name = in.readString();
      info = in.readString();
      words = in.readLong();
      size = in.readLong();
      url = in.readString();
      file = in.readString();
   }

   @Override
   public int describeContents()
   {
      return 0;
   }

   @Override
   public void writeToParcel(Parcel parcel, int flags)
   {
      parcel.writeString(name);
      parcel.writeString(info);
      parcel.writeLong(words);
      parcel.writeLong(size);
      parcel.writeString(url);
      parcel.writeString(file);
   }

   public static final Creator<DictInfo> CREATOR = new Creator<DictInfo>()
   {
      @Override
      public DictInfo createFromParcel(Parcel source)
      {
         return new DictInfo(source);
      }

      @Override
      public DictInfo[] newArray(int size)
      {
         return new DictInfo[size];
      }
   };

   public String getFile()
   {
      return file;
   }
}
