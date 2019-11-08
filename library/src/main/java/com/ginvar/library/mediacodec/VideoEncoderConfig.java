package com.ginvar.library.mediacodec;

import android.media.MediaCodecInfo;
import android.util.Log;

/**
 * 编码参数结构.
 */
public class VideoEncoderConfig {
    //主要用来设置size是否改变.
    /**
     * 编码参数分辨率宽度
     */
    private int mEncodeWidth;
    /**
     * 编码参数分辨率高度
     */
    private int mEncodeHeight;
    /**
     * 编码参数分辨率宽度,补齐后的数值(如果需要变更输出的编码尺寸,这个也需要改)
     */
    private int mPlaneEncodeWidth;
    /**
     * 编码参数分辨率宽度,补齐后的数值(如果需要变更输出的编码尺寸,这个也需要改)
     */
    private int mPlaneEncodeHeight;

    /**
     * 编码帧率
     */
    public int mFrameRate;
    /**
     * 编码码率, 单位是bps
     */
    public int mBitRate;
    /**
     * 编码GOP Size
     */
    public int mGopSize;
    /**
     * 编码Bitrate Model
     */
    public int mBitRateModel;
    /**
     * 编码质量
     */
    public float mQuality = DEFAULT_ENCODE_QUALITY;
    /* 防抖标识 */
    public boolean videoStabilization;
    /**
     * 编码类型， 默认是h264硬编码类型， 取值见 {@link VideoEncoderType}
     */
    public VideoEncoderType mEncodeType = VideoEncoderType.HARD_ENCODER_H264;

    /**
     * 编码动态扩展参数， 各个编码器具体识别此参数，可由服务器下发
     */
    public String mEncodeParameter = null;

    /**
     * 是否低延时模式
     */
    public boolean mLowDelay = false; //默认是false;
    /**
     * 是否高质量
     */
    public boolean mHighQuality = false;
    /**
     * 是否全I帧模式
     */
    public boolean mIFrameMode = true;

    private final static int DEFAULT_ENCODE_WIDTH = 544;
    private final static int DEFAULT_ENCODE_HEIGHT = 960;
    private final static int DEFAULT_ENCODE_BITRATE = 1200;
    private final static int DEFAULT_ENCODE_FRAMERATE = 24;
    private final static int DEFAULT_ENCODE_QUALITY = 21;
    private final static boolean DEFAULT_ENCODE_STABILIZATION = true;

    public boolean mEnableProfile = false;

    // 默认参数
    public VideoEncoderConfig() {
        this(DEFAULT_ENCODE_WIDTH, DEFAULT_ENCODE_HEIGHT, DEFAULT_ENCODE_FRAMERATE, DEFAULT_ENCODE_BITRATE,
                VideoEncoderType.HARD_ENCODER_H264, null);
    }

    public VideoEncoderConfig(int encodeWidth, int encodeHeight, int frameRate, int bitRate,
                              VideoEncoderType encoderType, String encodeParam) {
        setEncodeSize(encodeWidth, encodeHeight);
        mPlaneEncodeWidth = encodeWidth;
        mPlaneEncodeHeight = encodeHeight;
        mFrameRate = frameRate;
        mBitRate = bitRate;
        mEncodeType = encoderType;
        mEncodeParameter = encodeParam;
        videoStabilization = true;
        //输出6帧一个I帧.
        mGopSize = 6;
        mBitRateModel = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
        mIFrameMode = true;

    }

    public VideoEncoderConfig(VideoEncoderConfig config) {
        this.assign(config);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" mEncodeWidth:").append(getEncodeWidth());
        sb.append(" mEncodeHeith:").append(getEncodeHeight());
        sb.append(" mPlaneEncodeWidth:").append(mPlaneEncodeWidth);
        sb.append(" mPlaneEncodeHeight:").append(mPlaneEncodeHeight);
        sb.append(" mFrameRate:").append(mFrameRate);
        sb.append(" mBitRate:").append(mBitRate);
        sb.append(" mEncodeType:").append(mEncodeType);
        sb.append(" mLowDelay:").append(mLowDelay);
        sb.append(" mGopSize:").append(mGopSize);
        sb.append(" mBitRateModel:").append(mBitRateModel);
        sb.append(" mQuality:").append(mQuality);
        if (mEncodeParameter != null) {
            sb.append(" mEncodeParameter:").append(mEncodeParameter);
        }
        sb.append(" mIFrameMode:").append(mIFrameMode);
        return sb.toString();
    }

    public void assign(VideoEncoderConfig config) {
        setEncodeSize(config.getEncodeWidth(), config.getEncodeHeight());
        mPlaneEncodeWidth = config.getPlaneEncodeWidth();
        mPlaneEncodeHeight = config.getPlaneEncodeHeight();
        mFrameRate = config.mFrameRate;
        videoStabilization = config.videoStabilization;
        mEncodeType = config.mEncodeType;
        mBitRate = config.mBitRate;
        mEncodeParameter = config.mEncodeParameter;
        mLowDelay = config.mLowDelay;
        mGopSize = config.mGopSize;
        mBitRateModel = config.mBitRateModel;
        mIFrameMode = config.mIFrameMode;
        mHighQuality = config.mHighQuality;
        mQuality = config.mQuality;
    }

    public void setEncodeSize(int width, int height) {
        mEncodeWidth = width;
        mEncodeHeight = height;
    }

    public void setPlaneEncodeSize(int width, int height) {
        mPlaneEncodeWidth = width;
        mPlaneEncodeHeight = height;
    }

    public void setFrameRate(int frameRate) {
        mFrameRate = frameRate;
    }

    public void setBitRate(int bitRate) {
        mBitRate = bitRate;
    }

    public void setEncodeParam(String encodeParameter) {
        mEncodeParameter = encodeParameter;
    }

    public void setVideoEncoderType(VideoEncoderType videoEncoderType) {
        mEncodeType = videoEncoderType;
    }

    public int getEncodeWidth() {
        return mEncodeWidth;
    }

    public int getEncodeHeight() {
        return mEncodeHeight;
    }

    public int getPlaneEncodeWidth() {
        return mPlaneEncodeWidth;
    }

    public int getPlaneEncodeHeight() {
        return mPlaneEncodeHeight;
    }

    public boolean encodeParameterEmpty() {
        return (mEncodeParameter == null || mEncodeParameter.equals(""));
    }

    public void setHighQuality(boolean enable) {
        mHighQuality = enable;
    }

    public void setQuality(float quality) {
        mQuality = quality;
    }

    public void setGopSize(int gopSize) {
        mGopSize = gopSize;
    }

    public void setEnableProfile(boolean enableProfile) {
        Log.i("VideoEncoderConfig", "setEnableProfile enableProfile:" + enableProfile);
        mEnableProfile = enableProfile;
    }

    public void setBitRateModel(int bitRateModel) {
        mBitRateModel = bitRateModel;
    }

    public void setIFrameMode(boolean iFrameMode) {
        mIFrameMode = iFrameMode;
    }
}
