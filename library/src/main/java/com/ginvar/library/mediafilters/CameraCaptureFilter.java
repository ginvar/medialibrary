package com.ginvar.library.mediafilters;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.common.Common;
import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.drawer.TextureDrawer4ExtOES;
import com.ginvar.library.gles.utils.GLFrameBuffer;
import com.ginvar.library.gles.utils.GLTexture;
import com.ginvar.library.gpuimagefilters.FilterKey;
import com.ginvar.library.mediarecord.RecordConfig;
import com.ginvar.library.model.Size;
import com.ginvar.library.model.YYMediaSample;

import java.util.Iterator;
import java.util.Map;
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

    protected int mRecordWidth;
    protected int mRecordHeight;


    protected int mMaxPreviewWidth = 1280;
    protected int mMaxPreviewHeight = 1280;

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
            mCaptureSurfaceTexture.setOnFrameAvailableListener(this);

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
    public void configFilter(Map<String, Object> config) {
        Iterator<Map.Entry<String, Object>> it = config.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if(entry.getKey() == FilterKey.KEY_PRESET_CAPTURE_SIZE) {
                Size sz = (Size)entry.getValue();
                presetRecordingSize(sz.getWidth(), sz.getHeight());
            }
        }
    }

    @Override
    public void updateParams(Map<String, Object> config) {
        Iterator<Map.Entry<String, Object>> it = config.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if(entry.getKey() == FilterKey.KEY_PRESET_CAPTURE_SIZE) {
                Size sz = (Size)entry.getValue();
                presetRecordingSize(sz.getWidth(), sz.getHeight());
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mFilterContext.getGLManager().post(new Runnable() {
            @Override
            public void run() {
                handleFrameAvailble(mCaptureSurfaceTexture);
            }
        });
    }

    private void handleFrameAvailble(SurfaceTexture surfaceTexture) {
        processFrame(surfaceTexture);
    }

    private void processFrame(SurfaceTexture surfaceTexture) {
        mCameraFBO.bind();

        int previewWidth = cameraInstance().previewWidth();
        int previewHeight = cameraInstance().previewHeight();
        mCaptureSurfaceTexture.updateTexImage();

        mCaptureSurfaceTexture.getTransformMatrix(mTransformMatrix);

        mTextureDrawer4ExtOES.drawTexture(mCaptureTextureId, mTransformMatrix, previewHeight, previewWidth, previewHeight, previewWidth);

        GLES20.glFlush();

        mCameraFBO.unbind();

        YYMediaSample sample = new YYMediaSample();
        sample.texId = mCameraFBO.getTextureId();
        sample.width = mCameraFBO.getWidth();
        sample.height = mCameraFBO.getHeight();

        deliverToDownStream(sample);
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

    private void presetRecordingSize(int width, int height) {
        if (width > mMaxPreviewWidth || height > mMaxPreviewHeight) {
            float scaling = Math.min(mMaxPreviewWidth / (float) width, mMaxPreviewHeight / (float) height);
            width = (int) (width * scaling);
            height = (int) (height * scaling);
        }

        mRecordWidth = width;
        mRecordHeight = height;

//        mFilterContext.getGLManager().post(new Runnable() {
//            @Override
//            public void run() {
                cameraInstance().setPreferPreviewSize(mRecordWidth, mRecordHeight);
//            }
//        });

    }

}
