package com.ginvar.library.model;

import android.util.Log;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by bleach on 2019/4/15.
 */
public class EncodeMediaSampleAlloc {
    private static final String TAG = EncodeMediaSampleAlloc.class.getSimpleName();
    private ConcurrentLinkedQueue<EncodeMediaSample> mFreeDeque = new ConcurrentLinkedQueue<EncodeMediaSample>();

    public EncodeMediaSampleAlloc() {

    }

    public EncodeMediaSample alloc() {
        EncodeMediaSample sample = null;
        try {
            sample = mFreeDeque.poll();
        } catch (NoSuchElementException e) {
            Log.i(TAG, "fail allocate a sample buffer, no buffer in pool, e=" + e.toString());
            sample = null;
        }

        if (sample == null) {
            sample = new EncodeMediaSample();
            Log.i(TAG, "add more encodeSample");
        }

        return sample;
    }

    public void free(EncodeMediaSample sample) {
        if (sample == null) {
            return;
        }

        if (!mFreeDeque.add(sample)) {
            Log.e(TAG, "EncodeMediaSampleAlloc.free failed");
        }
    }
}
