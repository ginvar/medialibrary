package com.ginvar.library.mediafilters;

import android.content.Context;

import com.ginvar.library.gles.GLManager;
import com.ginvar.library.mediacodec.VideoEncoderConfig;
import com.ginvar.library.mediarecord.RecordConfig;

import android.opengl.EGLContext;

public class MediaFilterContext implements IMediaFilterContext {

    private GLManager mGlManager = null;
    private Context mAndroidContext = null;

    public VideoEncoderConfig mVideoEncoderConfig = new VideoEncoderConfig();

    public RecordConfig getRecordConfig() {
        return mRecordConfig;
    }

    public void setRecordConfig(RecordConfig mRecordConfig) {
        this.mRecordConfig = mRecordConfig;
    }

    private RecordConfig mRecordConfig = null;

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

    public VideoEncoderConfig getVideoEncoderConfig() {
        return mVideoEncoderConfig;
    }

    public void setVideoEncodeConfig(final VideoEncoderConfig vconfig) {
        if (getGLManager().checkSameThread()) {
            mVideoEncoderConfig = new VideoEncoderConfig(vconfig);
        } else {
            getGLManager().post(new Runnable() {
                @Override
                public void run() {
                    mVideoEncoderConfig = new VideoEncoderConfig(vconfig);
                }
            });
        }
    }
}
