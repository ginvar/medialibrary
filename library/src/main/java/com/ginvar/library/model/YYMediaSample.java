package com.ginvar.library.model;

import android.media.MediaFormat;
import android.opengl.GLUtils;

import com.ginvar.library.common.SampleType;
import com.ginvar.library.gles.utils.GLDataUtils;
import com.ginvar.library.mediacodec.VideoConstant;
import com.ginvar.library.mediacodec.VideoEncoderType;

import java.nio.ByteBuffer;

/**
 * Created by ginvar on 2019/9/13.
 */



public class YYMediaSample {

    public SampleType mSampleType = SampleType.UNKNOWN;

    public int texId;
    public int width;
    public int height;

    /**
     * Video Data
     */
    public ByteBuffer mDataByteBuffer = null;
    public int mBufferOffset = 0;
    public int mBufferSize = 0;
    public int mBufferFlag = 0; //用于传递mediacodec中的buffinfo中的flag

    public float[] mTransform = new float[16];

    public int mFrameFlag = 0;
    public int mFrameType = VideoConstant.VideoFrameType.kVideoUnknowFrame;

    public MediaFormat mMediaFormat = null;
    //图片在编码处理中的编码分辨率大小.
    public int mEncodeWidth = 0;
    public int mEncodeHeight = 0;
    //默认h264的硬编码.
    public VideoEncoderType mEncoderType = VideoEncoderType.HARD_ENCODER_H264;

    public boolean mEndOfStream = false;
    /**
     * 来自android系统的camera采集系统打的时间戳,
     * YY传输系统的音视频同步 "不是" 基于此时间戳.
     */
    public long mAndoridPtsNanos = 0;

    /**
     * camera采集系统回调onFrameAvailable函数后, YY媒体框架会获取系统时间戳
     * YY传输系统的音视频同步是基于此时间戳, 单位是毫秒. .
     */
    public long mYYPtsMillions = 0;

    /**
     * 编码时间戳, 单位毫秒
     */
    public long mDtsMillions = 0;

    public void reset() {
        texId = -1;
        width = 0;
        height = 0;

        mDataByteBuffer = null;
        mBufferOffset = 0;
        mBufferSize = 0;
        mBufferFlag = 0;

        System.arraycopy(GLDataUtils.MATRIX_4X4_IDENTITY, 0, mTransform, 0, mTransform.length);

        mEncoderType = VideoEncoderType.HARD_ENCODER_H264;
        mEncodeWidth = 0;
        mEncodeHeight = 0;

        mMediaFormat = null;

        mAndoridPtsNanos = 0;
        mYYPtsMillions = 0;
        mDtsMillions = 0;
    }
}
