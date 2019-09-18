package com.ginvar.library.filterhandler;

import com.ginvar.library.gles.utils.GLFrameBuffer;
import com.ginvar.library.model.YYMediaSample;

import java.util.ArrayList;

/**
 * Created by ginvar on 2019/8/8.
 */

public class JhImageHandler implements  JhImageHandlerInterface {

    // protected ArrayList<IMediaFilter>
    protected YYMediaSample mMediaSample;
    protected GLFrameBuffer mFrameBuffer;

    JhImageHandler() {
    }
    @Override
    public void processingFilters(YYMediaSample sample) {
        mMediaSample = sample;
    }

    @Override
    public YYMediaSample getMediaSample() {
        return mMediaSample;
    }
}
