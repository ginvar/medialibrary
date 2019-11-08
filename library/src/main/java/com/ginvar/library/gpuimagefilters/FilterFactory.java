package com.ginvar.library.gpuimagefilters;

import com.ginvar.library.api.common.FilterType;
import com.ginvar.library.mediafilters.CameraCaptureFilter;
import com.ginvar.library.mediafilters.IMediaFilter;
import com.ginvar.library.mediafilters.PreviewFilter;

import java.util.HashMap;

public class FilterFactory {

    private static FilterFactory mInstance;

    private HashMap<Integer, Class<IMediaFilter>> m_clsMap;

    public static FilterFactory getInstance() {
        if (mInstance == null) {
            synchronized (FilterFactory.class) {
                if (mInstance == null) {
                    mInstance = new FilterFactory();
                    mInstance.init();
                }
            }
        }
        return mInstance;
    }

    public FilterFactory() {
        m_clsMap = new HashMap<>();
    }

    public void init() {

        registerFilterCls(FilterType.CAMERA_CAPTURE_FILTER, CameraCaptureFilter.class);
        registerFilterCls(FilterType.PREVIEW_FILTER, PreviewFilter.class);
    }

    private void registerFilterCls(int type, Class cls) {
        m_clsMap.put(type, cls);
    }

    public IMediaFilter createFilter(int type) {
        Class cls = m_clsMap.get(type);
        IMediaFilter filter = null;
        try {
            filter = (IMediaFilter) cls.newInstance();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return filter;
    }
}
