package com.ginvar.library.gpuimagefilters;

import android.content.Context;

import com.ginvar.library.mediafilters.AbstractYYMediaFilter;

import java.util.concurrent.atomic.AtomicInteger;

public class FilterManager {

    private Context mContext = null;
    private int mOFContext = -1;


    private AtomicInteger mFilterID = new AtomicInteger(0);

    public FilterManager(Context context, int ofContext) {
        mContext = context;
        mOFContext = ofContext;
    }

    public int addFilter(int type, String jsonCfg) {
        AbstractYYMediaFilter mediaFilter = (AbstractYYMediaFilter)FilterFactory.getInstance().createFilter(type);
        if(mediaFilter instanceof OFBaseEffectFilter) {
            ((OFBaseEffectFilter) mediaFilter).init(mContext, mOFContext);
        }

        mediaFilter.configFilter(jsonCfg);
//        ((AbstractYYMediaFilter)mFilterList.get(mFilterList.size()-1)).addDownStream(mediaFilter);
//        mFilterList.add(mediaFilter);

        return mFilterID.getAndIncrement();
    }

    public void removeFilter(int id) {
//        if (id < mFilterList.size()) {
//            mFilterList.remove(id);
//        }
    }
}
