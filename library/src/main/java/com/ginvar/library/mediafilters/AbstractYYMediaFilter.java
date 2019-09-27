package com.ginvar.library.mediafilters;

import com.ginvar.library.model.FilterInfo;
import com.ginvar.library.model.YYMediaSample;

import java.util.ArrayList;

public class AbstractYYMediaFilter implements IMediaFilter {

    protected ArrayList<IMediaFilter> mDownStreamList = new ArrayList<IMediaFilter>();

//    protected FilterInfo mFilterInfo = null;
    @Override
    public boolean processMediaSample(YYMediaSample sample, Object upstream) {
        return false;
    }

    @Override
    public void configFilter(String jsonCfg) {

    }

    public AbstractYYMediaFilter addDownStream(IMediaFilter downStream) {
        //keep unique
        if (mDownStreamList.indexOf(downStream) < 0) {
            mDownStreamList.add(downStream);
        }
        return this;
    }

    public void removeDownStream(IMediaFilter downStrean) {
        mDownStreamList.remove(downStrean);
    }

    public void removeAllDownStream() {
        mDownStreamList.clear();
    }

    public void deliverToDownStream(YYMediaSample sample) {
        for (IMediaFilter filter : mDownStreamList) {
            filter.processMediaSample(sample, this);
        }
    }
}
