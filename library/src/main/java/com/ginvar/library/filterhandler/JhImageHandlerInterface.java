package com.ginvar.library.filterhandler;

import com.ginvar.library.model.YYMediaSample;

/**
 * Created by ginvar on 2019/9/13.
 */

public interface JhImageHandlerInterface {

    public void processingFilters(YYMediaSample sample);


    public YYMediaSample getMediaSample();

}
