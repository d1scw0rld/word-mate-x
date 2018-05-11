package org.d1scw0rld.wordmatex;

import android.text.Editable;
import android.text.TextWatcher;

public class InputWatcher implements TextWatcher
{
   WordMate wm;

   InputWatcher(WordMate wm)
   {
      this.wm = wm;
   }

   public void afterTextChanged(Editable s)
   {
      this.wm.onInput(s.toString());
   }

   public void beforeTextChanged(CharSequence s,
                                 int start,
                                 int count,
                                 int after)
   {}

   public void onTextChanged(CharSequence s, int start, int before, int count)
   {}
}
