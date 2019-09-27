package com.ginvar.library.drawer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.ginvar.library.common.Common;

/**
 * Created by ginvar on 2019/8/7.
 */

public class TextureDrawer4ExtOES extends TextureDrawer {

    protected static final String fshDrawer4oes =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // private int mTransformLoc;

    protected String getFragmentShaderString() {
        return fshDrawer4oes;
    }

    protected String getVertexShaderString() {
        return vshDrawer;
    }

    public static TextureDrawer4ExtOES create() {
        TextureDrawer4ExtOES drawer = new TextureDrawer4ExtOES();
        if(!drawer.init())
        {
            Log.e(Common.LOG_TAG, "TextureDrawer4ExtOES create failed!");
            drawer.release();
            drawer = null;
        }
        return drawer;
    }

    @Override
    public void drawTexture(int texID, float[] textureVertex, int inputWidth, int inputHeight, int outputWidth, int outputHeight) {
        _drawTexture(texID, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureVertex, inputWidth, inputHeight, outputWidth, outputHeight);
    }
}
