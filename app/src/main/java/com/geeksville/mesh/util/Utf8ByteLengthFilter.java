
package com.geeksville.mesh.util;

import android.text.InputFilter;
import android.text.Spanned;

public class Utf8ByteLengthFilter implements InputFilter {
    private final int mMaxBytes;
    public Utf8ByteLengthFilter(int maxBytes) {
        mMaxBytes = maxBytes;
    }
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        int srcByteCount = 0;
        
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            srcByteCount += (c < (char) 0x0080) ? 1 : (c < (char) 0x0800 ? 2 : 3);
        }
        int destLen = dest.length();
        int destByteCount = 0;
        
        for (int i = 0; i < destLen; i++) {
            if (i < dstart || i >= dend) {
                char c = dest.charAt(i);
                destByteCount += (c < (char) 0x0080) ? 1 : (c < (char) 0x0800 ? 2 : 3);
            }
        }
        int keepBytes = mMaxBytes - destByteCount;
        if (keepBytes <= 0) {
            return "";
        } else if (keepBytes >= srcByteCount) {
            return null; 
        } else {
            
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                keepBytes -= (c < (char) 0x0080) ? 1 : (c < (char) 0x0800 ? 2 : 3);
                if (keepBytes < 0) {
                    return source.subSequence(start, i);
                }
            }

            return null;
        }
    }
}
