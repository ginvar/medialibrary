package com.ginvar.library.render;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ginvar.library.camera.CameraInstance;
import com.ginvar.library.common.Common;
import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.drawer.TextureDrawer4ExtOES;
import com.ginvar.library.gles.GLManager;
import com.ginvar.library.gles.WindowSurface;
import com.ginvar.library.gles.utils.GLDataUtils;
import com.ginvar.library.gles.utils.GLFrameBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Created by ginvar on 2019/8/28.
 */

public class PreviewRender implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = PreviewRender.class.getSimpleName();

    public static final float[] MATRIX_4X4_IDENTITY = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
    };

    protected float[] mModelMatrix = new float[16];
    protected float[] mProjectionMatrix = new float[16];
    protected float[] mViewMatrix = new float[16];

    protected float[] mMVPMatrix = new float[16];

    protected boolean mFlipY = false;
    protected boolean mFlipX = false;
    protected float mRotateAngle = 0.0f;

    private SurfaceTexture mCaptureSurfaceTexture;
    private int mCaptureTextureId;
    protected float[] mTransformMatrix = new float[16];

    // private OnSurfaceCreateListener mOnSurfaceCreateListener;
    private GLManager mGLManager;

    private WeakReference<SurfaceView> mWeakSurfaceView;

    // protected JhFrameRenderer mFrameRenderer;
    protected TextureDrawer4ExtOES mTextureDrawer4ExtOES;
    protected TextureDrawer mTextureDrawer;

    protected int mViewWidth;
    protected int mViewHeight;
    protected int mRecordWidth;
    protected int mRecordHeight;

    public static class Viewport {
        public int x, y, width, height;
    }

    protected int mMaxPreviewWidth = 1280;
    protected int mMaxPreviewHeight = 1280;

    protected Viewport mDrawViewport = new Viewport();

    public Viewport getDrawViewport() {
        return mDrawViewport;
    }

    public WindowSurface mPreviewWindowSurface;

    public GLFrameBuffer mPreviewFBO = null;

    protected boolean mFitFullView = false;

    public void setFitFullView(boolean fit) {
        mFitFullView = fit;
        calcViewport();
    }

    protected CameraInstance cameraInstance() { return CameraInstance.getInstance(); }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public PreviewRender() {
        mGLManager = new GLManager(null);
        mGLManager.waitUntilRun();
    }

    public void init() {
        mGLManager.post(new Runnable() {
            @Override
            public void run() {
                mCaptureTextureId = Common.genSurfaceTextureID();
                mCaptureSurfaceTexture = new SurfaceTexture(mCaptureTextureId);
                mCaptureSurfaceTexture.setOnFrameAvailableListener(PreviewRender.this);

                // mFrameRenderer = new JhFrameRenderer();

                // if(!mFrameRenderer.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight)) {
                //     Log.i(TAG, "FrameRenderer init failed.\n");
                // }
                //
                // mFrameRenderer.setRotateAngle(-90);
                mTextureDrawer4ExtOES = TextureDrawer4ExtOES.create();
                mTextureDrawer = TextureDrawer.create();
                // mTextureDrawer4ExtOES.setRotateAngle(-90);
            }
        });
    }
    // public void setOnSurfaceCreateListener(OnSurfaceCreateListener listener) {
    //     mOnSurfaceCreateListener = listener;
    // }

    public void setSurfaceView(SurfaceView surfaceView) {
        mWeakSurfaceView = new WeakReference<SurfaceView>(surfaceView);
        surfaceView.getHolder().addCallback(mCallback);
    }

    public void presetRecordingSize(int width, int height) {
        if (width > mMaxPreviewWidth || height > mMaxPreviewHeight) {
            float scaling = Math.min(mMaxPreviewWidth / (float) width, mMaxPreviewHeight / (float) height);
            width = (int) (width * scaling);
            height = (int) (height * scaling);
        }

        mRecordWidth = width;
        mRecordHeight = height;

        mGLManager.post(new Runnable() {
            @Override
            public void run() {
                cameraInstance().setPreferPreviewSize(mRecordWidth, mRecordHeight);
            }
        });

    }

    protected void calcViewport() {

        float scaling = mRecordWidth / (float) mRecordHeight;
        float viewRatio = mViewWidth / (float) mViewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) (mViewHeight * scaling);
                h = mViewHeight;
            } else {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            } else {
                h = mViewHeight;
                w = (int) (mViewHeight * scaling);
            }
        }

        mDrawViewport.width = w;
        mDrawViewport.height = h;
        mDrawViewport.x = (mViewWidth - mDrawViewport.width) / 2;
        mDrawViewport.y = (mViewHeight - mDrawViewport.height) / 2;
        Log.i(TAG, String.format("View port: %d, %d, %d, %d", mDrawViewport.x, mDrawViewport.y, mDrawViewport.width,
                mDrawViewport.height));
    }


    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            // if (mGLManager != null) {
            //     mGLManager.getHandler().sendMessage(mGLManager.getHandler()
            //             .obtainMessage(GLManager.GLHandler.MSG_SURFACE_CREATED, holder));
            // }
            // mGLManager.post(new Runnable() {
            //     @Override
            //     public void run() {
            //         mCaptureTextureId = Common.genSurfaceTextureID();
            //         mCaptureSurfaceTexture = new SurfaceTexture(mCaptureTextureId);
            //         mCaptureSurfaceTexture.setOnFrameAvailableListener(PreviewRender.this);
            //
            //         mFrameRenderer = new JhFrameRenderer();
            //
            //         if(!mFrameRenderer.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight)) {
            //             Log.i(TAG, "FrameRenderer init failed.\n");
            //         }
            //
            //         mFrameRenderer.setRotateAngle(-90);
            //
            //
            //     }
            // });
            // mCaptureTextureId = Common.genSurfaceTextureID();
            // mCaptureSurfaceTexture = new SurfaceTexture(mCaptureTextureId);
            // mCaptureSurfaceTexture.setOnFrameAvailableListener(this);

            // if(mOnSurfaceCreateListener != null) {
            //     mOnSurfaceCreateListener.onSurfaceCreate(mCaptureSurfaceTexture, mCaptureTextureId);
            // }


        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            mViewWidth = width;
            mViewHeight = height;
            calcViewport();

            releaseWindowSurface();
            if(mPreviewWindowSurface == null) {
                mPreviewWindowSurface = new WindowSurface(mGLManager.getEglCore(), holder.getSurface(), false);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    public void releaseWindowSurface() {
        if(mPreviewWindowSurface != null) {
            mPreviewWindowSurface.release();
            mPreviewWindowSurface = null;
        }
    }

    public void resumePreview() {

        // if (mFrameRecorder == null) {
        //     Log.e(LOG_TAG, "resumePreview after release!!");
        //     return;
        // }


        if (!cameraInstance().isCameraOpened()) {

            int facing = mIsCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;

            cameraInstance().tryOpenCamera(new CameraInstance.CameraOpenCallback() {
                @Override
                public void cameraReady() {
                    Log.i(TAG, "tryOpenCamera OK...");
                }
            }, facing);
        }

        if (!cameraInstance().isPreviewing()) {
            cameraInstance().startPreview(mCaptureSurfaceTexture);
            // mFrameRenderer.srcResize(cameraInstance().previewHeight(), cameraInstance().previewWidth());
            mPreviewFBO = new GLFrameBuffer(cameraInstance().previewHeight(), cameraInstance().previewWidth());
        }

        // requestRender();
    }

    //是否使用后置摄像头
    protected boolean mIsCameraBackForward = true;

    public boolean isCameraBackForward() {
        return mIsCameraBackForward;
    }

    protected void onSwitchCamera() {

    }

    public void startPreview() {
        if(mGLManager.checkSameThread()) {
            if (!cameraInstance().isPreviewing()) {
                resumePreview();
            }
        } else {
            mGLManager.post(new Runnable() {
                @Override
                public void run() {
                    if (!cameraInstance().isPreviewing()) {
                        resumePreview();
                    }
                }
            });
        }
    }

    public final void switchCamera() {
        mIsCameraBackForward = !mIsCameraBackForward;

        mGLManager.post(new Runnable() {
            @Override
            public void run() {

                cameraInstance().stopCamera();
                onSwitchCamera();
                int facing = mIsCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;

                cameraInstance().tryOpenCamera(new CameraInstance.CameraOpenCallback() {
                    @Override
                    public void cameraReady() {
                        resumePreview();
                    }
                }, facing);

                // requestRender();
            }
        });
    }

    public void requestRender() {
        mGLManager.post(new Runnable() {
            @Override
            public void run() {
                if (mCaptureSurfaceTexture == null ||
                        !cameraInstance().isPreviewing() ||
                        mPreviewWindowSurface == null ||
                        mPreviewFBO == null) {

                    return;
                }

                 // mGLManager.resetContext();
                // GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                mPreviewFBO.bind();

                int previewWidth = cameraInstance().previewWidth();
                int previewHeight = cameraInstance().previewHeight();
                mCaptureSurfaceTexture.updateTexImage();
                //
                mCaptureSurfaceTexture.getTransformMatrix(mTransformMatrix);
                //
                // mFrameRenderer.update(mCaptureTextureId, mTransformMatrix);

                mTextureDrawer4ExtOES.setTransform(mTransformMatrix);
                mTextureDrawer4ExtOES.drawTexture(mCaptureTextureId, previewHeight, previewWidth, previewHeight, previewWidth);

                GLES20.glFlush();


//                IntBuffer rgbaBuffer = IntBuffer.allocate(previewWidth * previewHeight * 4);

//                rgbaBuffer.position(0);
//                GLES20.glReadPixels(0, 0, previewHeight, previewWidth, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuffer);
//
//                int[] buffer = new int[previewWidth * previewHeight * 4];
//                rgbaBuffer.get(buffer);
//                Bitmap bitmap = Bitmap.createBitmap(buffer, previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
//                saveToJPEG(bitmap, "oes");
                mPreviewFBO.unbind();

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GLES20.glClearColor(0,0,0,1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                mPreviewWindowSurface.makeCurrent();

                // GLES20.glViewport(mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height);



                mTextureDrawer.setTransform(GLDataUtils.MATRIX_4X4_IDENTITY);
                mTextureDrawer.drawTexture(mPreviewFBO.getTextureId(), mPreviewFBO.getWidth(), mPreviewFBO.getHeight
                        (), mViewWidth, mViewHeight);

                mPreviewWindowSurface.swapBuffers();

//                IntBuffer rgbaBuffer1 = IntBuffer.allocate(previewWidth * previewHeight * 4);
////                rgbaBuffer.order(ByteOrder.LITTLE_ENDIAN);
//                rgbaBuffer1.position(0);
//                GLES20.glReadPixels(0, 0, previewHeight, previewWidth, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaBuffer1);
//
//                int[] buffer = new int[previewWidth * previewHeight * 4];
//                rgbaBuffer.get(buffer);
//                Bitmap bitmap = Bitmap.createBitmap(buffer, previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
//                saveToJPEG(bitmap, "oes");
                // mGLManager.swapBuffers();
            }
        });
    }

    private void saveToJPEG(Bitmap bitmap, String fileName) {
        String dir = "/data/data/com.ginvar.medialibrary/cache/";

        try {
            File file = new File(dir + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {

        startPreview();
    }
}
