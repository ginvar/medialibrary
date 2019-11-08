package com.ginvar.library.gpuimagefilters;

import android.content.Context;

import com.ginvar.library.mediafilters.AbstractYYMediaFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FilterManager {

    private ArrayList<AbstractYYMediaFilter> mInputFilters = new ArrayList<>();
    private ArrayList<AbstractYYMediaFilter> mOutputFilters = new ArrayList<>();

    private ArrayList<AbstractYYMediaFilter> mBaseFilter = new ArrayList<>();


    private ArrayList<AbstractYYMediaFilter> mLevelNode = new ArrayList<>();

    private Context mContext = null;
    private int mOFContext = -1;

    private AtomicInteger mFilterID = new AtomicInteger(0);

    public FilterManager(Context context, int ofContext) {
        mContext = context;
        mOFContext = ofContext;

    }

    public void addPathInFilter(AbstractYYMediaFilter filter) {
        if(mInputFilters.indexOf(filter) < 0) {
            mInputFilters.add(filter);
        }

        performLayout();
    }

    public void addPathOutFilter(AbstractYYMediaFilter filter) {
        if(mOutputFilters.indexOf(filter) < 0) {
            mOutputFilters.add(filter);
        }

        performLayout();
    }

    public int addFilter(int type, Map<String, Object> config) {
        AbstractYYMediaFilter filter = (AbstractYYMediaFilter) FilterFactory.getInstance().createFilter(type);
        filter.configFilter(config);

        mBaseFilter.add(filter);
        performLayout();

        return mBaseFilter.size() - 1;
    }

    private void performLayout() {

        if(mBaseFilter.isEmpty()) {

            defaultLayout();
            return;
        }

        for(AbstractYYMediaFilter filter : mInputFilters) {
            filter.removeAllDownStream();
            filter.addDownStream(mBaseFilter.get(0));
        }

        for (int i = 0, j = 1; i < mBaseFilter.size() && j < mBaseFilter.size(); i++, j++) {
            mBaseFilter.get(i).removeAllDownStream();
            mBaseFilter.get(i).addDownStream(mBaseFilter.get(j));

        }

        mBaseFilter.get(mBaseFilter.size() - 1).removeAllDownStream();

        for(AbstractYYMediaFilter filter : mOutputFilters) {
            mBaseFilter.get(mBaseFilter.size() - 1).addDownStream(filter);
        }
    }

    private void defaultLayout() {
        for(AbstractYYMediaFilter infilter : mInputFilters) {
            infilter.removeAllDownStream();
            for(AbstractYYMediaFilter outfilter : mOutputFilters) {
                infilter.addDownStream(outfilter);
            }
        }
    }
}
