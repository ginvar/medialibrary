package com.ginvar.library.gpuimagefilters;

import android.content.Context;

import com.ginvar.library.mediafilters.AbstractYYMediaFilter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FilterManager {

    public static final int Input_Filter_Mask = 0x01;
    public static final int Output_Filter_Mask = 0x02;
    public static final int Process_Filter_Mask = 0x04;

    public static final int High_Priority_Filter_Mask = 0x08;
    public static final int Low_Priority_Filter_Mask = 0x0A;



    public static final int Filter_Priority_0 = 0;
    public static final int Filter_Priority_1 = 1;
    public static final int Filter_Priority_2 = 2;

    private ArrayList<AbstractYYMediaFilter> mInputFilters = new ArrayList<>();
    private ArrayList<AbstractYYMediaFilter> mOutputFilters = new ArrayList<>();
    private ArrayList<AbstractYYMediaFilter> mBaseFilters = new ArrayList<>();
//    private AbstractYYMediaFilter mFirst = new AbstractYYMediaFilter();
//    private AbstractYYMediaFilter mTailFilters = new AbstractYYMediaFilter();
//    private AbstractYYMediaFilter mTailFilters = new AbstractYYMediaFilter();

    private ArrayList<AbstractYYMediaFilter> mLevelNode = new ArrayList<>();

    private Context mContext = null;
    private int mOFContext = -1;

    private int mLowestFilterId = -1;
    private int mHighestFilterId = -1;


    private AtomicInteger mFilterID = new AtomicInteger(0);

    public FilterManager(Context context, int ofContext) {
        mContext = context;
        mOFContext = ofContext;

//        mHeadFilters.addDownStream(mTailFilters);
//        mTailFilters.addUpStream(mHeadFilters);


        for(int i = 0; i < 3; i++) {
            AbstractYYMediaFilter levelFilter = new AbstractYYMediaFilter();
            mLevelNode.add(levelFilter);

            if(i > 0) {
                linkToFilter(mLevelNode.get(i-1), mLevelNode.get(i));
            }
        }
    }

    public AbstractYYMediaFilter addFilter(int type, String jsonCfg, int flag) {
        AbstractYYMediaFilter filter = initFilter(type, jsonCfg);

        if(filter != null) {
            if ((flag & Input_Filter_Mask) > 0) {
                mInputFilters.add(filter);
            } else if ((flag & Output_Filter_Mask) > 0) {
                mOutputFilters.add(filter);
            } else if((flag & Process_Filter_Mask) > 0) {

                addFilterWithPriorityLevel(filter, Filter_Priority_1);

            } else if ((flag & Low_Priority_Filter_Mask) > 0) {
//                mBaseFilters.add(filter);

                addFilterWithPriorityLevel(filter, Filter_Priority_2);

            } else if ((flag & High_Priority_Filter_Mask) > 0) {
                //直接在head前面追加
//                mBaseFilters.add(0, filter);

                addFilterWithPriorityLevel(filter, Filter_Priority_0);
            }
        }

        return filter;
    }

    private AbstractYYMediaFilter initFilter(int type, String jsonCfg) {
        AbstractYYMediaFilter mediaFilter = (AbstractYYMediaFilter)FilterFactory.getInstance().createFilter(type);
        if(mediaFilter instanceof OFBaseEffectFilter) {
            ((OFBaseEffectFilter) mediaFilter).init(mContext, mOFContext);
        }

        mediaFilter.configFilter(jsonCfg);
//        ((AbstractYYMediaFilter)mFilterList.get(mFilterList.size()-1)).addDownStream(mediaFilter);
//        mFilterList.add(mediaFilter);
        return mediaFilter;
    }

//    //获取所有输出到ID为outputFilterId的特效列表
//    private ArrayList<AbstractYYMediaFilter> getFilterList(int outputFilterId) {
//        ArrayList<AbstractYYMediaFilter>
//    }

    public void removeFilter(int id) {
        for(int i = 0; i < mInputFilters.size(); i++) {
            AbstractYYMediaFilter inputFilter = mInputFilters.get(i);
            AbstractYYMediaFilter resFilter = search(inputFilter, id);
            if(resFilter != null) {
                for(int j = 0; j < resFilter.getUpStreamList().size(); j++) {
                    AbstractYYMediaFilter usFilter = resFilter.getUpStreamList().get(j);
                    usFilter.removeDownStream(resFilter);

                    for(int k = 0; k < resFilter.getDownStreamList().size(); k++) {
                        AbstractYYMediaFilter dsFilter = resFilter.getDownStreamList().get(k);
                        dsFilter.removeUpStream(resFilter);
                        usFilter.addDownStream(resFilter.getDownStreamList().get(k));

                    }
                }
            }
        }
    }

    private AbstractYYMediaFilter search(AbstractYYMediaFilter filter, int filterId) {
        if(filter.mFilterId == filterId) {
            return filter;
        }
        for(int i = 0; i < filter.getDownStreamList().size(); i++) {
            AbstractYYMediaFilter downStreamFilter = filter.getDownStreamList().get(i);
            AbstractYYMediaFilter resFilter = search(downStreamFilter, filterId);
            if(resFilter != null) {
                return resFilter;
            }
        }
        return null;
    }

    private void performFilterLayout() {
//        for(int i = 0; i < mInputFilters.size(); i++) {
//            AbstractYYMediaFilter inputfilter = mInputFilters.get(i);
//
//            inputfilter.removeAllDownStream();
//
//            inputfilter.addDownStream(mHeadFilters);
//            mHeadFilters.addUpStream(inputfilter);
//        }
//
//        for(i)
    }

    private void addFilterWithPriorityLevel(AbstractYYMediaFilter filter, int priority) {

        if(priority == Filter_Priority_0) {
            for(int i = 0; i < mLevelNode.get(priority).getDownStreamList().size(); i++) {
                AbstractYYMediaFilter dsFilter = mLevelNode.get(priority).getDownStreamList().get(i);
                filter.addDownStream(dsFilter);
                dsFilter.removeUpStream(mLevelNode.get(priority));
                mLevelNode.get(priority).removeDownStream(dsFilter);
            }
            mLevelNode.get(priority).addDownStream(filter);
        } else {
            for (int i = 0; i < mLevelNode.get(priority).getUpStreamList().size(); i++) {
                AbstractYYMediaFilter t = mLevelNode.get(priority).getUpStreamList().get(i);
                t.removeDownStream(mLevelNode.get(priority));
                mLevelNode.get(priority).removeUpStream(t);
                t.addDownStream(filter);
                filter.addUpStream(t);
            }

            filter.addDownStream(mLevelNode.get(priority));
            mLevelNode.get(priority).addUpStream(filter);
        }
    }

    private void linkToFilter(AbstractYYMediaFilter filter1, AbstractYYMediaFilter filter2) {
        filter1.addDownStream(filter2);
        filter2.addUpStream(filter1);
    }
}
