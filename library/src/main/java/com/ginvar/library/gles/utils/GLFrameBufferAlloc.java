package com.ginvar.library.gles.utils;

import android.util.Log;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bleach on 2019/4/15.
 */
public class GLFrameBufferAlloc {

    private static final String TAG = GLFrameBufferAlloc.class.getSimpleName();
    private ConcurrentLinkedQueue<FrameBufferObject> mFrameBufferDeque = new ConcurrentLinkedQueue<FrameBufferObject>();

    public GLFrameBufferAlloc() {

    }

    public void deInit() {
        for (FrameBufferObject frameBufferObject : mFrameBufferDeque) {
            frameBufferObject.mFrameBuffer.deInit();
        }
        mFrameBufferDeque.clear();
    }

    public FrameBufferObject alloc(int width, int height) {
        FrameBufferObject freeFrameBufferObject = null;
        try {
            for (FrameBufferObject frameBufferObject : mFrameBufferDeque) {
                if (frameBufferObject.mFrameBuffer.getWidth() == width
                        && frameBufferObject.mFrameBuffer.getHeight() == height
                        && frameBufferObject.mRefCnt.get() == 0) {
                    freeFrameBufferObject = frameBufferObject;
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            Log.i(TAG, "fail allocate a sample buffer, no buffer in pool, e=" + e.toString());
            freeFrameBufferObject = null;
        }

        if (freeFrameBufferObject == null) {
            freeFrameBufferObject = new FrameBufferObject();
            freeFrameBufferObject.mFrameBuffer = new GLFrameBuffer(width, height);
            mFrameBufferDeque.add(freeFrameBufferObject);
            Log.i(TAG, "add more framebuffer");
        }
        freeFrameBufferObject.mRefCnt.incrementAndGet();

        return freeFrameBufferObject;
    }

    public void free(FrameBufferObject frameBufferObject) {
        if (frameBufferObject != null) {
            frameBufferObject.mRefCnt.decrementAndGet();
        }
    }

    public class FrameBufferObject {
        public GLFrameBuffer mFrameBuffer;
        public AtomicInteger mRefCnt = new AtomicInteger(0);

        FrameBufferObject() {
            mFrameBuffer = null;
        }
    }
}
