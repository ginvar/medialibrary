package com.ginvar.library.mediafilters;

import com.ginvar.library.model.FilterInfo;
import com.ginvar.library.model.YYMediaSample;

import java.util.Map;

public interface IMediaFilter {

    boolean processMediaSample(YYMediaSample sample, Object upstream);

    void configFilter(Map<String, Object> config);

    void updateParams(Map<String, Object> config);
}
