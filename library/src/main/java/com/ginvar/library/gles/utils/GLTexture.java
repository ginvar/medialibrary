/***************** BEGIN FILE HRADER BLOCK *********************************
 *                                                                         *
 *   Copyright (C) 2016 by YY.inc.                                         *
 *                                                                         *
 *   Proprietary and Trade Secret.                                         *
 *                                                                         *
 *   Authors:                                                              *
 *   1): Cheng Yu -- <chengyu@yy.com>                                      *
 *                                                                         *
 ***************** END FILE HRADER BLOCK ***********************************/

package com.ginvar.library.gles.utils;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.os.Build;

import java.nio.ByteBuffer;

public class GLTexture {
    private static final String TAG = GLTexture.class.getSimpleName();

    private int mTextureID = -1;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mTarget = 0;

    public GLTexture() {
        mTextureID = createTexture(false);
    }

    public GLTexture(final boolean isExt) {
        mTextureID = createTexture(isExt);
    }

    public GLTexture(final int width, final int height) {
        mWidth = width;
        mHeight = height;
        mTextureID = createTexture(width, height);
    }

    public GLTexture(final int target, final int format, final int type, final int width, final int height) {
        mWidth = width;
        mHeight = height;
        mTextureID = createTexture(target, format, type, width, height);
    }

    public GLTexture(final Bitmap bitmap, final boolean recycle) {
        mTextureID = createTexture(false);
        updateTexture(bitmap, recycle);
    }

    public GLTexture(ByteBuffer data, int width, int height, int format, int type) {
        mTextureID = createTexture(false);
        updateTexture(data, width, height, format, type);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void setTextureId(int textureId) {
        mTextureID = textureId;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void deInit() {
        if (mTextureID != -1) {
            int[] ids = {mTextureID};
            GLES20.glDeleteTextures(1, ids, 0);
            mTextureID = -1;
            mWidth = mHeight = 0;
        }
    }

    private int createTexture(final boolean isExt) {
        if (isExt) {
            return createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0, 0);
        } else {
            return createTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0, 0);
        }
    }

    private int createTexture(final int width, final int height) {
        return createTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, width, height);
    }

    private int createTexture(final int target, final int format, final int type, final int width, final int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(target, textures[0]);
        if (width != 0 && height != 0) {
            if (type == GLES20.GL_UNSIGNED_BYTE) {
                GLES20.glTexImage2D(target, 0, format, width, height, 0,
                        format, type, null);
            } else if (type == GLES31.GL_RGBA16F && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                GLES31.glTexStorage2D(target, 1, type, width, height);
            }
        }
        GLES20.glTexParameterf(target,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(target,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(target,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(target,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(target, 0);

        // GLErrorUtils.checkGlError("GLTextureUtils createTexture failed width:" + width + ", height:" + height);

        mTarget = target;
        return textures[0];
    }

    public void updateTexture(Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        if (mTarget != GLES20.GL_TEXTURE_2D) {
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTarget, mTextureID);
        if (mWidth != bitmap.getWidth() && mHeight != bitmap.getHeight()) {
            GLUtils.texImage2D(mTarget, 0, bitmap, 0);
        } else {
            GLUtils.texSubImage2D(mTarget, 0, 0, 0, bitmap);
        }
        GLES20.glBindTexture(mTarget, 0);

        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        if (recycle) {
            bitmap.recycle();
        }
    }

    public void updateTexture(final ByteBuffer data, final int width, final int height, final int format,
                              final int type) {
        if (mTarget != GLES20.GL_TEXTURE_2D) {
            return;
        }

        GLES20.glBindTexture(mTarget, mTextureID);
        if (data != null) {
            if (mWidth != width && mHeight != height) {
                GLES20.glTexImage2D(mTarget, 0, format,
                        width, height, 0, format, type, data);
            } else {
                GLES20.glTexSubImage2D(mTarget, 0, 0, 0,
                        width, height, format, type, data);
            }
        }
        GLES20.glBindTexture(mTarget, 0);
        mWidth = width;
        mHeight = height;
    }

}
