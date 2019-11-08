package com.ginvar.library.mediacodec.videocodec;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.ginvar.library.model.EncodeMediaSample;

import java.nio.ByteBuffer;

/**
 * Created by lookatme on 2016/3/26.
 */
public interface HardEncodeListner {
    void onEncodeOutputBuffer(ByteBuffer buffer, MediaCodec.BufferInfo buffInfo, long dtsMs, long ptsMs,
                              MediaFormat mediaFormat, EncodeMediaSample sample);

    void onEncoderFormatChanged(MediaFormat mediaFormat);

    void onEndOfInputStream();

    void onError(long eid, String errMsg); //硬编码出错.
}
