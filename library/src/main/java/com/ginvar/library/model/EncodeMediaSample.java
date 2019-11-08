package com.ginvar.library.model;

//import com.orangefilter.OrangeFilter;
//import com.ycloud.toolbox.gles.utils.GLDataUtils;

/**
 * Created by bleach on 2019/4/15.
 */
public class EncodeMediaSample {
    /**
     * 来自android系统的camera采集系统打的时间戳,
     * YY传输系统的音视频同步 "不是" 基于此时间戳.
     */
    public long mAndoridPtsNanos = 0;
    /**
     * 实际的图片信息的长，宽
     */
    public int mWidth = 0;
    public int mHeight = 0;

    /**
     * 经过裁剪后，需要输出的图片长宽
     */
    public int mClipWidth = 0;
    public int mClipHeight = 0;

    public float[] mTransform = new float[16];

//    public OrangeFilter.OF_FaceFrameData[] mFaceFrameDataArr;
//    public OrangeFilter.OF_BodyFrameData[] mBodyFrameDataArr;

    public void assigne(YYMediaSample sample) {
//        this.mWidth = sample.mWidth;
//        this.mHeight = sample.mHeight;
//        this.mClipWidth = sample.mClipWidth;
//        this.mClipHeight = sample.mClipHeight;
//        this.mAndoridPtsNanos = sample.mAndoridPtsNanos;
//        this.mTransform = sample.mTransform;
//        this.mFaceFrameDataArr = sample.mFaceFrameDataArr;
//        this.mBodyFrameDataArr = sample.mBodyFrameDataArr;
    }

    public void reset() {
//        this.mWidth = 0;
//        this.mHeight = 0;
//        this.mClipWidth = 0;
//        this.mClipHeight = 0;
//        this.mAndoridPtsNanos = 0;
//        System.arraycopy(GLDataUtils.MATRIX_4X4_IDENTITY, 0, this.mTransform, 0, this.mTransform.length);
//        this.mFaceFrameDataArr = null;
//        this.mBodyFrameDataArr = null;
    }
}
