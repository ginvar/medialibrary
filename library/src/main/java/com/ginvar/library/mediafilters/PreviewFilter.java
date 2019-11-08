package com.ginvar.library.mediafilters;

import android.content.Context;
import android.opengl.GLES20;
import android.view.SurfaceHolder;

import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.gles.WindowSurface;
import com.ginvar.library.gles.utils.GLDataUtils;
import com.ginvar.library.mediarecord.RecordConfig;
import com.ginvar.library.model.YYMediaSample;

public class PreviewFilter extends AbstractYYMediaFilter {

    private MediaFilterContext mFilterContext = null;
    private Context mContext = null;

    protected TextureDrawer mTextureDrawer;

    public WindowSurface mPreviewWindowSurface;

    protected int mViewWidth;
    protected int mViewHeight;

    public PreviewFilter(Context context, MediaFilterContext filterContext) {
        mContext = context.getApplicationContext();
        mFilterContext = filterContext;
    }

    private void doInit() {
        mTextureDrawer = TextureDrawer.create();
    }

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

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClearColor(0,0,0,1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mPreviewWindowSurface.makeCurrent();

        // GLES20.glViewport(mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height);


        mTextureDrawer.drawTexture(sample.texId, GLDataUtils.MATRIX_4X4_IDENTITY, sample.width, sample.height, mViewWidth, mViewHeight);

        mPreviewWindowSurface.swapBuffers();
        deliverToDownStream(sample);
        return true;
    }
    public void releaseWindowSurface() {
        if(mPreviewWindowSurface != null) {
            mPreviewWindowSurface.release();
            mPreviewWindowSurface = null;
        }
    }

    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mViewWidth = width;
        mViewHeight = height;

        releaseWindowSurface();
        if(mPreviewWindowSurface == null) {
            mPreviewWindowSurface = new WindowSurface(mFilterContext.getGLManager().getEglCore(), holder.getSurface(), false);
        }
    }
}
