package com.ginvar.library.mediarecord;

import android.content.Context;
import android.hardware.Camera;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ginvar.library.api.common.FilterType;
import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.gpuimagefilters.FilterFactory;
import com.ginvar.library.gpuimagefilters.OFBaseEffectFilter;
import com.ginvar.library.mediafilters.AbstractYYMediaFilter;
import com.ginvar.library.mediafilters.CameraCaptureFilter;
import com.ginvar.library.mediafilters.IMediaFilter;
import com.ginvar.library.mediafilters.MediaFilterContext;
import com.ginvar.library.render.PreviewRender;

import java.util.ArrayList;

/**
 * Created by ginvar on 2019/8/28.
 */

public class VideoRecordSession implements SurfaceHolder.Callback{
    private SurfaceView mSurfaceView = null;
    private PreviewRender mPreviewRender = null;
    private ArrayList<IMediaFilter> mFilterList = new ArrayList<>();
    private MediaFilterContext mMediaFilterContext = null;

    public VideoRecordSession(Context context, SurfaceView surfaceView) {

//        mPreviewRender = new PreviewRender();
//        mPreviewRender.presetRecordingSize(720,720);
//        // CameraInstance.getInstance().setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        mPreviewRender.setSurfaceView(surfaceView);
//
//        mPreviewRender.init();
        mMediaFilterContext = new MediaFilterContext(context, null);
        mSurfaceView = surfaceView;
    }

    public void init() {
        addFilter(FilterType.CAMERA_CAPTURE_FILTER, "");
        addFilter(FilterType.PREVIEW_FILTER, "");
    }

    public void onResume() {
        //mPreviewRender.startPreview();
    }

    public void startPreview() {
        //mPreviewRender.startPreview();
    }

    public int addFilter(int type, String jsonCfg) {
        AbstractYYMediaFilter mediaFilter = (AbstractYYMediaFilter)FilterFactory.getInstance().createFilter(type);
        if(mediaFilter instanceof OFBaseEffectFilter) {
            ((OFBaseEffectFilter) mediaFilter).init(mMediaFilterContext.getAndroidContext(), -1);
        }

        mediaFilter.configFilter(jsonCfg);
        ((AbstractYYMediaFilter)mFilterList.get(mFilterList.size()-1)).addDownStream(mediaFilter);
        mFilterList.add(mediaFilter);

        return mFilterList.size() - 1;
    }

    public void removeFilter(int id) {
        if (id < mFilterList.size()) {
            mFilterList.remove(id);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
