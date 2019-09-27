package com.ginvar.library.mediafilters;

import android.content.Context;

import com.ginvar.library.gles.GLManager;

public interface IMediaFilterContext {
    GLManager getGLManager();

    Context getAndroidContext();
}
