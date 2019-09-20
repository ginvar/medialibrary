package com.ginvar.library.drawer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.ginvar.library.common.Common;
import com.ginvar.library.common.ProgramObject;
import com.ginvar.library.gles.utils.GLDataUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by ginvar on 2019/8/7.
 */

public class TextureDrawer {
    protected static final String vshDrawer =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    protected static final String fshDrawer =
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public static final float[] vertices = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };

    public static final int DRAW_FUNCTION = GLES20.GL_TRIANGLE_STRIP;
    protected static final int VERTEX_DATA_POSITION = 0;
    protected static final int UV_DATA_POSITION = 1;

    protected ProgramObject mProgram;
    protected int mVertBuffer;
    protected FloatBuffer mVertBufferData;

    protected int mInputWidth = 0;
    protected int mInputHeight = 0;
    protected int mOutputWidth = 0;
    protected int mOutputHeight = 0;

    protected int mViewPortX = 0;
    protected int mViewPortY = 0;
    protected int mViewPortW = 0;
    protected int mViewPortH = 0;

    // private int muMVPMatrixLoc;
    // private int muSTMatrixLoc;

    protected float[] mModelMatrix = new float[16];
    protected float[] mProjectionMatrix = new float[16];
    protected float[] mViewMatrix = new float[16];

    protected float[] mMVPMatrix = new float[16];

    protected boolean mFlipY = false;
    protected boolean mFlipX = false;
    protected float mRotateAngle = 0.0f;

    protected boolean mDirty = false;

    public ProgramObject getProgram() {
        return mProgram;
    }

    protected String getFragmentShaderString() {
        return fshDrawer;
    }

    protected String getVertexShaderString() {
        return vshDrawer;
    }


    protected boolean init() {
        return init(getVertexShaderString(), getFragmentShaderString());
    }

    protected boolean init(String vsh, String fsh) {

        // Matrix.setIdentityM(mSTMatrix, 0);

        mProgram = new ProgramObject();
        mProgram.bindAttribLocation("aPosition", VERTEX_DATA_POSITION);
        mProgram.bindAttribLocation("aTextureCoord", UV_DATA_POSITION);
        if (!mProgram.init(vsh, fsh)) {
            mProgram.release();
            mProgram = null;
            return false;
        }

        mProgram.bind();

        // muMVPMatrixLoc = mProgram.getUniformLoc("uMVPMatrix");
        // muSTMatrixLoc = mProgram.getUniformLoc("uSTMatrix");

        int[] vertBuffer = new int[1];
        GLES20.glGenBuffers(1, vertBuffer, 0);
        mVertBuffer = vertBuffer[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertBuffer);

        mVertBufferData = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mVertBufferData.put(vertices);
        mVertBufferData.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4, mVertBufferData, GLES20.GL_STATIC_DRAW);

        return true;
    }

    public static TextureDrawer create() {
        TextureDrawer drawer = new TextureDrawer();
        if(!drawer.init(vshDrawer, fshDrawer))
        {
            Log.e(Common.LOG_TAG, "TextureDrawer create failed!");
            drawer.release();
            drawer = null;
        }
        return drawer;
    }

    public void release() {
        if(mProgram != null) {
            mProgram.release();
            mProgram = null;
        }
        GLES20.glDeleteBuffers(1, new int[]{mVertBuffer}, 0);
        mVertBuffer = 0;
    }

    public void setTransform(float[] transformMatrix) {
        mProgram.bind();
        mProgram.sendUniformMat4("uSTMatrix", 1, false, transformMatrix);
    }

    public void setMVPMatrix(float[] mvpMatrix) {
        mProgram.bind();
        mProgram.sendUniformMat4("uMVPMatrix", 1, false, mvpMatrix);
    }

    public void setRotateAngle(float rotateAngle) {
        mRotateAngle = rotateAngle;
        mDirty = true;
    }

    // public void drawTexture(int texID) {
    //     drawTexture(texID, GLES20.GL_TEXTURE_2D);
    // }
    protected void buildMatrix() {
        if(mDirty) {
            calcViewport(mInputWidth, mInputHeight, mOutputWidth, mOutputHeight);

            Matrix.setIdentityM(mViewMatrix, 0);
            Matrix.setIdentityM(mProjectionMatrix, 0);
            Matrix.setIdentityM(mModelMatrix, 0);

            Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
            Matrix.orthoM(mProjectionMatrix, 0, -1, 1, -mInputHeight / mInputWidth, mInputHeight / mInputWidth, 0.0f, 2.0f);
//            Matrix.translateM(mModelMatrix, 0, mInputWidth / 2, mInputHeight / 2, 0.0f); 
//            Matrix.scaleM(mModelMatrix, 0, mInputWidth, mInputHeight, 1.0f);
            if (mRotateAngle != 0.0f) {
                Matrix.rotateM(mModelMatrix, 0, mRotateAngle, 0.0f, 0.0f, 1.0f);
            }

            if (mFlipY) {
                Matrix.rotateM(mModelMatrix, 0, 180.0f, 1.0f, 0.0f, 0.0f);
            }

            if (mFlipX) {
                Matrix.rotateM(mModelMatrix, 0, 180.0f, 0.0f, 1.0f, 0.0f);
            }

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mViewMatrix, 0);

            setMVPMatrix(mMVPMatrix);
            mDirty = false;
        }
    }

    private void calcViewport(int srcWidth, int srcHeight, int dstWidth, int dstHeight)
    {
        // float scaling = Math.max(dstWidth / (float)srcWidth, dstHeight / (float)srcHeight);
        //
        // if(scaling != 0.0f)
        // {
        //     int sw = (int)(srcWidth * scaling);
        //     int sh = (int)(srcHeight * scaling);
        //     mViewPortX = (dstWidth - sw) / 2;
        //     mViewPortY = (dstHeight - sh) / 2;
        //     mViewPortW = sw;
        //     mViewPortH = sh;
        // }

        float scaling = Math.min(dstWidth / (float)srcWidth, dstHeight / (float)srcHeight);

        if(scaling != 0.0f)
        {
            int sw = (int)(srcWidth * scaling);
            int sh = (int)(srcHeight * scaling);
            mViewPortX = (dstWidth - sw) / 2;
            mViewPortY = (dstHeight - sh) / 2;
            mViewPortW = sw;
            mViewPortH = sh;
        }
    }

    protected void checkRenderDirty(int inputWidth, int inputHeight, int outputWidth, int outputHeight) {
        if (mInputWidth != inputWidth || mInputHeight != inputHeight ||
                mOutputWidth != outputWidth || mOutputHeight != outputHeight) {
            mInputWidth = inputWidth;
            mInputHeight = inputHeight;

            mOutputWidth = outputWidth;
            mOutputHeight = outputHeight;

            mDirty = true;
        }
    }

    public void drawTexture(int texID, int inputWidth, int inputHeight, int outputWidth, int outputHeight) {

        _drawTexture(texID, GLES20.GL_TEXTURE_2D, inputWidth, inputHeight, outputWidth, outputHeight);
    }

    protected void _drawTexture(int texID, int type, int inputWidth, int inputHeight, int outputWidth, int
            outputHeight) {

        checkRenderDirty(inputWidth, inputHeight, outputWidth, outputHeight);

        buildMatrix();

//         if(type == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
//             setMVPMatrix(GLDataUtils.MATRIX_4X4_IDENTITY);
//         }

        GLES20.glViewport(mViewPortX, mViewPortY, mViewPortW, mViewPortH);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(type, texID);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertBuffer);
        // mVertBufferData.position(0);
        GLES20.glEnableVertexAttribArray(VERTEX_DATA_POSITION);
        GLES20.glVertexAttribPointer(VERTEX_DATA_POSITION, 3, GLES20.GL_FLOAT, false, 5 * 4, 0);
        // mVertBufferData.position(3);
        GLES20.glEnableVertexAttribArray(UV_DATA_POSITION);
        GLES20.glVertexAttribPointer(UV_DATA_POSITION, 2, GLES20.GL_FLOAT, false, 5 * 4, 3 * 4);

        mProgram.bind();
        // mProgram.sendUniformi("sTexture", 0);

        GLES20.glDrawArrays(DRAW_FUNCTION, 0, 4);
    }


}
