package com.ginvar.library.mediafilters;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.common.Common;
import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.drawer.TextureDrawer4ExtOES;
import com.ginvar.library.gles.utils.GLFrameBuffer;
import com.ginvar.library.gles.utils.GLTexture;
import com.ginvar.library.mediarecord.RecordConfig;
import com.ginvar.library.model.YYMediaSample;

import java.util.concurrent.atomic.AtomicBoolean;

public class CameraCaptureFilter extends AbstractYYMediaFilter
        implements SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {

    private SurfaceTexture mCaptureSurfaceTexture;
    private int mCaptureTextureId;
    protected float[] mTransformMatrix = new float[16];

    protected TextureDrawer4ExtOES mTextureDrawer4ExtOES;

    private AtomicBoolean mInited = new AtomicBoolean(false);
    private MediaFilterContext mFilterContext = null;
    private Context mContext = null;

    private RecordConfig mRecordConfig;

    public GLFrameBuffer mCameraFBO = null;

    public CameraCaptureFilter(Context context, MediaFilterContext filterContext) {
        mContext = context.getApplicationContext();
        mFilterContext = filterContext;
        mRecordConfig = new RecordConfig();
    }

    protected CameraInstance cameraInstance() { return CameraInstance.getInstance(); }

    private void doInit() {

        synchronized (mInited) {
            if (mInited.get()) {
                return;
            }

            mCaptureTextureId = Common.genSurfaceTextureID();
            mCaptureSurfaceTexture = new SurfaceTexture(mCaptureTextureId);

            mTextureDrawer4ExtOES = TextureDrawer4ExtOES.create();
            mInited.set(true);
            mInited.notifyAll();
        }
    }
    //running in gl thread.
    public void init() {

        if (mFilterContext.getGLManager().checkSameThread()) {
            doInit();
        } else {
            mFilterContext.getGLManager().post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    doInit();
                }
            });
            //wait for implement.
        }
    }

    @Override
    public boolean processMediaSample(YYMediaSample sample, Object upstream) {
        return true;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    private void handleFrameAvailble(SurfaceTexture surfaceTexture) {
        processFrame(surfaceTexture);
    }

    private void processFrame(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mFilterContext.getGLManager().post(new Runnable() {
            @Override
            public void run() {
                handleFrameAvailble(mCaptureSurfaceTexture);
            }
        });
    }

    public void resumePreview() {

        // if (mFrameRecorder == null) {
        //     Log.e(LOG_TAG, "resumePreview after release!!");
        //     return;
        // }


        if (!cameraInstance().isCameraOpened()) {

            int facing = mRecordConfig.getCameraId();

            cameraInstance().tryOpenCamera(new CameraInstance.CameraOpenCallback() {
                @Override
                public void cameraReady() {
//                    Log.i(TAG, "tryOpenCamera OK...");
                }
            }, facing);
        }

        if (!cameraInstance().isPreviewing()) {
            cameraInstance().startPreview(mCaptureSurfaceTexture);
            // mFrameRenderer.srcResize(cameraInstance().previewHeight(), cameraInstance().previewWidth());
            mCameraFBO = new GLFrameBuffer(cameraInstance().previewHeight(), cameraInstance().previewWidth());
        }

        // requestRender();
    }

    public void startPreview() {
        if(mFilterContext.getGLManager().checkSameThread()) {
            if (!cameraInstance().isPreviewing()) {
                resumePreview();
            }
        } else {
            mFilterContext.getGLManager().post(new Runnable() {
                @Override
                public void run() {
                    if (!cameraInstance().isPreviewing()) {
                        resumePreview();
                    }
                }
            });
        }
    }

}
