package com.ginvar.library.mediarecord;

import android.content.Context;
import android.hardware.Camera;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ginvar.library.api.common.FilterType;
import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.gpuimagefilters.FilterFactory;
import com.ginvar.library.gpuimagefilters.FilterKey;
import com.ginvar.library.gpuimagefilters.FilterManager;
import com.ginvar.library.gpuimagefilters.OFBaseEffectFilter;
import com.ginvar.library.mediafilters.AbstractYYMediaFilter;
import com.ginvar.library.mediafilters.CameraCaptureFilter;
import com.ginvar.library.mediafilters.IMediaFilter;
import com.ginvar.library.mediafilters.MediaFilterContext;
import com.ginvar.library.mediafilters.MediaFormatAdapterFilter;
import com.ginvar.library.mediafilters.PreviewFilter;
import com.ginvar.library.mediafilters.VideoEncoderGroupFilter;
import com.ginvar.library.model.Size;
import com.ginvar.library.render.PreviewRender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ginvar on 2019/8/28.
 */

public class VideoRecordSession implements SurfaceHolder.Callback{

    private static final String TAG = VideoRecordSession.class.getSimpleName();

    private SurfaceView mSurfaceView = null;
    private PreviewRender mPreviewRender = null;
    private ArrayList<IMediaFilter> mFilterList = new ArrayList<>();
    private MediaFilterContext mMediaFilterContext = null;

    private CameraCaptureFilter mCameraCaptureFilter = null;
    private PreviewFilter mPreviewFilter = null;

    private VideoEncoderGroupFilter mVideoEncoderFilter = null;
    MediaFormatAdapterFilter mMediaFormatAdapterFilter = null;

    private FilterManager mFilterManager = null;

    private Object mRecordLock = new Object();

    public VideoRecordSession(Context context, SurfaceView surfaceView) {

//        mPreviewRender = new PreviewRender();
//        mPreviewRender.presetRecordingSize(720,720);
//        // CameraInstance.getInstance().setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        mPreviewRender.setSurfaceView(surfaceView);
//
//        mPreviewRender.init();
        mMediaFilterContext = new MediaFilterContext(context, null);
        mFilterManager = new FilterManager(context, 0);

        mSurfaceView = surfaceView;
        mSurfaceView.getHolder().addCallback(this);

        HashMap<String, Object> config = new HashMap<String, Object>();
        Size sz = new Size();
        sz.setWidth(720);
        sz.setHeight(1280);
        config.put(FilterKey.KEY_PRESET_CAPTURE_SIZE, sz);
        mCameraCaptureFilter = new CameraCaptureFilter(context, mMediaFilterContext);
        mCameraCaptureFilter.init();
        mCameraCaptureFilter.configFilter(config);

        mPreviewFilter = new PreviewFilter(context, mMediaFilterContext);
        mPreviewFilter.init();


        mMediaFormatAdapterFilter = new MediaFormatAdapterFilter(mMediaFilterContext);
        mMediaFormatAdapterFilter.setNAL3ValidNAL4(true);

        mFilterManager.addPathInFilter(mCameraCaptureFilter);
        mFilterManager.addPathOutFilter(mPreviewFilter);
    }

    public void init() {
//        addFilter(FilterType.CAMERA_CAPTURE_FILTER, "");
//        addFilter(FilterType.PREVIEW_FILTER, "");
    }

    public void onResume() {
        if(mCameraCaptureFilter != null) {
            mCameraCaptureFilter.startPreview();
        }

//        mPreviewRender.startPreview();
    }

//    public void startPreview() {
        //mPreviewRender.startPreview();
//    }

    public int addFilter(int type, Map<String, Object> config) {

        return mFilterManager.addFilter(type, config);
    }

    public void removeFilter(int id) {
        if (id < mFilterList.size()) {
            mFilterList.remove(id);
        }
    }

    public void startRecord() {

        synchronized (mRecordLock) {
            if (mMediaFilterContext != null) {
                mMediaFilterContext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mVideoEncoderFilter != null) {
                                mVideoEncoderFilter.startEncode(mMediaFilterContext.getVideoEncoderConfig());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "video startRecord exception occur:" + e.getMessage());
                        } finally {
                            synchronized (mRecordLock) {
                                mRecordLock.notify();
                            }
                        }
                    }
                });
            }

            try {
                mRecordLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecord() {
        synchronized (mRecordLock) {
            if (mMediaFilterContext != null) {
                mMediaFilterContext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mVideoEncoderFilter != null) {
                                mVideoEncoderFilter.stopEncode();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "video stopRecord exception occur:" + e.getMessage());
                        } finally {
                            synchronized (mRecordLock) {
                                mRecordLock.notify();
                            }
                        }
                    }
                });
            }

            try {
                mRecordLock.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "video mVideoStopRecordLock ," + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mPreviewFilter != null) {
            mPreviewFilter.onSurfaceChanged(holder, format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
