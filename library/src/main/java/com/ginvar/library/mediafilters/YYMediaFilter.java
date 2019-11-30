package com.ginvar.library.mediafilters;

import com.ginvar.library.model.YYMediaSample;

/**
 * Created by kele on 2017/4/27.
 */

public class YYMediaFilter extends AbstractYYMediaFilter {
    @Override
    public boolean processMediaSample(YYMediaSample sample, Object upstream) {
        deliverToDownStream(sample);
        return false;
    }
}
