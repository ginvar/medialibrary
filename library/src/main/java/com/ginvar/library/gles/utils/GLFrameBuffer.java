package com.ginvar.library.gles.utils;

import android.opengl.GLES20;

import java.nio.IntBuffer;

/**
 * Created by bleach on 2019/1/10.
 */
public class GLFrameBuffer {
    private static final String TAG = GLFrameBuffer.class.getSimpleName();

    private GLTexture mTexture = null;
    // 用于外部纹理引用,无需销毁
    private int mTextureId = -1;
    private int mFrameBufferId = -1;
    private int mWidth = 0;
    private int mHeight = 0;
    private IntBuffer mOldFrameBuffer;

    private GLFrameBuffer() {

    }

    public GLFrameBuffer(int width, int height) {
        mWidth = width;
        mHeight = height;

        mTexture = new GLTexture(mWidth, mHeight);
        init(mTexture.getTextureId());
    }

    public GLFrameBuffer(int textureId) {
        mTextureId = textureId;
        init(mTextureId);
    }

    private void init(int textureId) {
        mOldFrameBuffer = IntBuffer.allocate(1);

        int[] frameBuffers = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        mFrameBufferId = frameBuffers[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                textureId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void storeOldFrameBuffer() {
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, mOldFrameBuffer);
    }

    private void recoverOldFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOldFrameBuffer.get(0));
    }

    public void bind() {
        storeOldFrameBuffer();
        if (mWidth != 0 && mHeight != 0) {
            GLES20.glViewport(0, 0, mWidth, mHeight);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
    }

    public void unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        recoverOldFrameBuffer();
    }

    public int getFrameBufferId() {
        return mFrameBufferId;
    }

    public int getTextureId() {
        if (mTexture != null) {
            return mTexture.getTextureId();
        }

        if (mTextureId != -1) {
            return mTextureId;
        }

        return -1;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void changeSize(int width, int height) {
        if (mWidth == width && mHeight == height) {
            return;
        }

        deInit();
        mWidth = width;
        mHeight = height;
        mTexture = new GLTexture(mWidth, mHeight);
        init(mTexture.getTextureId());
    }

    public void changeTexture(int textureId) {
        mTextureId = textureId;

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                textureId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void deInit() {
        if (mOldFrameBuffer != null) {
            mOldFrameBuffer.clear();
            mOldFrameBuffer = null;
        }

        if (mTexture != null) {
            mTexture.deInit();
            mTexture = null;
        }

        mTextureId = -1;

        if (mFrameBufferId > 0) {
            int[] framebuffers = new int[1];
            framebuffers[0] = mFrameBufferId;
            GLES20.glDeleteFramebuffers(1, framebuffers, 0);
            mFrameBufferId = -1;
        }
    }
}
