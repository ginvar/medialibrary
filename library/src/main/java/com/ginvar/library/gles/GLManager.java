package com.ginvar.library.gles;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import android.opengl.EGLContext;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by ginvar on 2019/9/11.
 */

public class GLManager implements Runnable {
    private static final String TAG = "GLManager";

    private static final int mDefaultWidth = 10;
    private static final int mDefaultHeight = 10;

    public Thread mLooperThread;
    public GLHandler mGLHandler = null;

    private EglCore mEglCore;
    private OffscreenSurface mOffscreenSurface;
    private EGLContext mSharedContext = null;

    private int mBitFlag = EglCore.FLAG_RECORDABLE;

    private AtomicBoolean mInitCompletion = new AtomicBoolean(false);

    public GLManager(EGLContext sharedContext) {
        if(sharedContext != null) {
            mSharedContext = sharedContext;
        }

        mLooperThread = new Thread(this, "GLManager");
        mLooperThread.setPriority(Thread.NORM_PRIORITY + 4);
        mLooperThread.start();
    }

    public long getThreadId() {
        return mLooperThread.getId();
    }

    public Looper getLooper() {
        return mGLHandler.getLooper();
    }

    public Handler getHandler() {
        return mGLHandler;
    }

    public EglCore getEglCore() {
        return mOffscreenSurface.getEglCore();
    }

    public Object getEglContext() {
        return mOffscreenSurface.getEglCore().getEGLContext();
    }

    public void resetContext() {
        mOffscreenSurface.makeCurrent();
    }

    public void setBitFlag(int bitFlag) { mBitFlag = bitFlag; }

    public boolean checkSameThread() {
        return (Thread.currentThread().getId() == this.getThreadId());
    }

    public void swapBuffers() { mOffscreenSurface.swapBuffers(); }

    public void waitUntilRun() {
        synchronized (mInitCompletion) {
            if(!mInitCompletion.get()) {

                try {
                    mInitCompletion.wait();
                } catch(InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
    }

    public boolean post(Runnable task) {
        boolean ret = false;
        try {
            ret = mGLHandler.post(task);
        } catch (Throwable t) {
            Log.e(TAG, "[exception] GlManager PostRunnable exeception:" + t.toString());
        }
        return ret;
    }

    private void initEGL() {
        try {
            mEglCore = new EglCore(mSharedContext, mBitFlag);
            mOffscreenSurface = new OffscreenSurface(mEglCore, mDefaultWidth, mDefaultHeight);
            mOffscreenSurface.makeCurrent();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void deInitEGL() {

        if(mOffscreenSurface != null) {
            mOffscreenSurface.release();
            mOffscreenSurface = null;
        }

        mEglCore.release();
    }

    @Override
    public void run() {
        try {
            Looper.prepare();
            initEGL();
            mGLHandler = new GLHandler(this);

            synchronized (mInitCompletion) {
                mInitCompletion.set(true);

                mInitCompletion.notifyAll();
            }

            Looper.loop();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                deInitEGL();
            } catch (Throwable t) {
                Log.e(TAG, "[exception]deInitEGL exception occur, " + t.toString());
            }
        }
    }

    // public void onSurfaceCreated(SurfaceHolder holder) {
    //     initEGL(holder);
    // }

    public static class GLHandler extends Handler {

        // Surface创建
        public static final int MSG_SURFACE_CREATED = 0x001;
        // Surface改变
        public static final int MSG_SURFACE_CHANGED = 0x002;
        // Surface销毁
        public static final int MSG_SURFACE_DESTROYED = 0x003;
        // 渲染
        public static final int MSG_RENDER = 0x004;

        private WeakReference<GLManager> mWeakGLManager;

        GLHandler(GLManager glManager) {
            mWeakGLManager = new WeakReference<GLManager>(glManager);
        }
        @Override
        public void handleMessage(Message msg) {

            GLManager manager = mWeakGLManager.get();
            // switch(msg.what) {
            //     case MSG_SURFACE_CREATED:
            //         // manager.onSurfaceCreated((SurfaceHolder)msg.obj);
            //         break;
            //     case MSG_SURFACE_CHANGED:
            //         break;
            //     case MSG_SURFACE_DESTROYED:
            //         break;
            //     case MSG_RENDER:
            //         break;
            // }
        }
    }
}
