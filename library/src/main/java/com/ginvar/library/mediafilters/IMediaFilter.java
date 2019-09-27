package com.ginvar.library.mediafilters;

import com.ginvar.library.model.FilterInfo;
import com.ginvar.library.model.YYMediaSample;

public interface IMediaFilter {

    boolean processMediaSample(YYMediaSample sample, Object upstream);

    void configFilter(String jsonCfg);
}
