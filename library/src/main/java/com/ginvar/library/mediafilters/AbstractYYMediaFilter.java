package com.ginvar.library.mediafilters;

import com.ginvar.library.model.FilterInfo;
import com.ginvar.library.model.YYMediaSample;

import java.util.ArrayList;

public class AbstractYYMediaFilter implements IMediaFilter {

    protected ArrayList<AbstractYYMediaFilter> mDownStreamList = new ArrayList<AbstractYYMediaFilter>();

    protected ArrayList<AbstractYYMediaFilter> mUpStreamList = new ArrayList<AbstractYYMediaFilter>();
    public int mFilterId = -1;
//    protected FilterInfo mFilterInfo = null;
    @Override
    public boolean processMediaSample(YYMediaSample sample, Object upstream) {
        deliverToDownStream(sample);
        return true;
    }

    @Override
    public void configFilter(String jsonCfg) {

    }

    public AbstractYYMediaFilter addDownStream(AbstractYYMediaFilter downStream) {
        //keep unique
        if (mDownStreamList.indexOf(downStream) < 0) {
            mDownStreamList.add(downStream);
        }

        return this;
    }

    public void removeDownStream(AbstractYYMediaFilter downStrean) {
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

    public ArrayList<AbstractYYMediaFilter> getUpStreamList() {
        return mUpStreamList;
    }
    public ArrayList<AbstractYYMediaFilter> getDownStreamList() {
        return mDownStreamList;
    }


    public AbstractYYMediaFilter addUpStream(AbstractYYMediaFilter upStream) {
        //keep unique
        if (mUpStreamList.indexOf(upStream) < 0) {
            mUpStreamList.add(upStream);
        }

        return this;
    }


    public void removeUpStream(AbstractYYMediaFilter upStream) {
        mUpStreamList.remove(upStream);
    }

    public void removeAllUpStream() {
        mUpStreamList.clear();
    }
}
