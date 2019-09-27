package com.ginvar.library.gpuimagefilters;

import android.content.Context;

import com.ginvar.library.mediafilters.AbstractYYMediaFilter;

public class OFBaseEffectFilter extends AbstractYYMediaFilter {

    protected Context mContext = null;
    protected int mOFContext = -1; // 只是使用，不负责销毁

    public void init(Context context, int ofContext) {
        mContext = context;
        mOFContext = ofContext;
    }
}
