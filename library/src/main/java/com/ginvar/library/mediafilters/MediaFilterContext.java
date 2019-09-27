package com.ginvar.library.mediafilters;

import android.content.Context;

import com.ginvar.library.gles.GLManager;

import android.opengl.EGLContext;

public class MediaFilterContext implements IMediaFilterContext {

    private GLManager mGlManager = null;
    private Context mAndroidContext = null;

    public MediaFilterContext(Context context, EGLContext eglContext) {
        mGlManager = new GLManager(eglContext);
        mGlManager.waitUntilRun();
        if (context != null) {
            mAndroidContext = context.getApplicationContext();
        }
    }
    @Override
    public GLManager getGLManager() {
        return mGlManager;
    }

    @Override
    public Context getAndroidContext() {
        return mAndroidContext;
    }
}
