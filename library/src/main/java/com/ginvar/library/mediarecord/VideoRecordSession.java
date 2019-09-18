package com.ginvar.library.mediarecord;

import android.hardware.Camera;
import android.provider.MediaStore;
import android.view.SurfaceView;

import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.render.PreviewRender;

/**
 * Created by ginvar on 2019/8/28.
 */

public class VideoRecordSession {
    private SurfaceView mSurfaceView = null;
    private PreviewRender mPreviewRender = null;

    public VideoRecordSession(SurfaceView surfaceView) {

        mPreviewRender = new PreviewRender();
        mPreviewRender.presetRecordingSize(1080,1920);
        // CameraInstance.getInstance().setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mPreviewRender.setSurfaceView(surfaceView);

        mPreviewRender.init();

    }


    public void onResume() {
        mPreviewRender.startPreview();
    }

    public void startPreview() {
        mPreviewRender.startPreview();
    }
}
