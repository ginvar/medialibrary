package com.ginvar.library.mediacodec.videocodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import com.ginvar.library.mediacodec.VideoEncoderConfig;
import com.ginvar.library.mediacodec.VideoEncoderType;
import com.ginvar.library.mediacodec.format.YYMediaFormatStrategy;
import com.ginvar.library.model.EncodeMediaSample;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by lookatmeyou on 2016/4/21.
 */

//TODO. 同步服务器下发的配置.
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class HardSurfaceEncoder {
    private final static String TAG = HardSurfaceEncoder.class.getSimpleName();
    private static MediaCodec mEncoder;
    private static Surface mInputSurface;

    MediaCodec.BufferInfo mBufferInfo;

    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBps;
    private int mGopSize;
    private int mBitRateModel;
    private boolean mEnableProfile;

    private String mCodecName = null;
    private LinkedList<Long> mCachedPtsList = new LinkedList<Long>();

    private static boolean mInitialized = false;
    HardEncodeListner mListener;
    private AtomicLong mEncodeId = new AtomicLong(-1);

    int mLevel;
    int mBaseLineLevel;
    int mProfile;
    String mMime = "";

    private int mRequestSyncCnt = 0;

    private int mFrameCnt = 0;

    private String mStrFormat = "";   // 用于输出调试信息，显示在界面上

    static MediaFormat mMediaFormat;
    VideoEncoderConfig mVideoEncoderConfig;

    HardSurfaceEncoder(String tag, String mime, long eid) {
        mMime = mime;
        mEncodeId.set(eid);
    }

    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= 18;
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    public boolean init(VideoEncoderConfig config, HardEncodeListner listener) {
        mVideoEncoderConfig = config;
        synchronized (this) {
            try {
                if (!isAvailable()) {
//                    YYLog.error(TAG, "hardware encoder is not available");
                    return false;
                }

//                YYLog.info(this,
//                        Constant.MEDIACODE_ENCODER + "[procedure] encoder init, configure： " + config.toString());
                this.mWidth = config.getEncodeWidth();
                this.mHeight = config.getEncodeHeight();
                this.mFps = config.mFrameRate;
                this.mBps = config.mBitRate;
                this.mGopSize = config.mGopSize;
                this.mBitRateModel = config.mBitRateModel;
                this.mEnableProfile = config.mEnableProfile;

                if (!mInitialized) {
                    initEncoder();
                }

                mBufferInfo = new MediaCodec.BufferInfo();
                mListener = listener;
//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "MediaCodec format:" + mStrFormat);
            } catch (Throwable t) {
//                YYLog.error(TAG, Constant.MEDIACODE_ENCODER + "[exception]" + t.toString());
            }

            return mInitialized;
        }
    }

    private void initEncoder() {
        try {
            if (mEncoder == null) {
                mEncoder = MediaCodec.createEncoderByType(mMime);

                mCodecName = mEncoder.getName();
                mMediaFormat = MediaFormat.createVideoFormat(mMime, mWidth, mHeight);
                MediaCodecInfo.CodecCapabilities caps = mEncoder.getCodecInfo().getCapabilitiesForType(mMime);
                MediaCodecInfo.CodecProfileLevel[] pr = caps.profileLevels;

                mLevel = 0;
                mProfile = 0;
                if (mMime.equals("video/hevc")) {
                    for (MediaCodecInfo.CodecProfileLevel aPr : pr) {
                        if (mProfile == aPr.profile && mLevel <= aPr.level) {
                            mProfile = aPr.profile;
                            mLevel = aPr.level;
                        }
                    }
                } else if (!mVideoEncoderConfig.mLowDelay &&
                        mVideoEncoderConfig.mEncodeType == VideoEncoderType.HARD_ENCODER_H264) {
                    //find baseline level
                    for (MediaCodecInfo.CodecProfileLevel aPr : pr) {
                        if (aPr.profile <= MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444) {
                            if (mProfile < aPr.profile) {
                                mProfile = aPr.profile;
                                mLevel = aPr.level;
                            } else if (mProfile == aPr.profile && mLevel < aPr.level) {
                                mProfile = aPr.profile;
                                mLevel = aPr.level;
                            }
                        }

                        if (aPr.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline) {
                            if (mBaseLineLevel < aPr.level) {
                                mBaseLineLevel = aPr.level;
                            }
                        }
                    }
                    if (mEnableProfile && mProfile > 0) {
                        // 即使能用high,兼容性也是问题,担心有的机器直接崩溃了
                        mLevel = mLevel > MediaCodecInfo.CodecProfileLevel.AVCLevel42 ?
                                MediaCodecInfo.CodecProfileLevel.AVCLevel42 : mLevel;     // avoid crash
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mMediaFormat.setInteger(MediaFormat.KEY_PROFILE, mProfile);
                        }

//                        YYLog.info(this, "mediaFormat.level:" + mLevel);
                        mMediaFormat.setInteger("level", mLevel);
                    } else {
                        mBaseLineLevel = mBaseLineLevel > MediaCodecInfo.CodecProfileLevel.AVCLevel42 ?
                                MediaCodecInfo.CodecProfileLevel.AVCLevel42 : mBaseLineLevel;     // avoid crash

                        mMediaFormat
                                .setInteger(MediaFormat.KEY_PROFILE,
                                        MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//                        YYLog.info(this, "mediaFormat.Baseline level:" + mBaseLineLevel);
                        mMediaFormat.setInteger("level", mBaseLineLevel);
                    }
                }
                mMediaFormat
                        .setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBps);
//                mediaFormat.setInteger("bitrate-mode", 0);
// 0:BITRATE_MODE_CQ, 1:BITRATE_MODE_VBR, 2:BITRATE_MODE_CBR
                mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);

                if (mGopSize == 0) {
                    //部分机型设为0有问题，故设为1，也认为是全I帧编码的折中策略
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                    } else {
                        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mGopSize);
                    }
                } else {
                    mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mGopSize);
                }

                mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, mBitRateModel);


                //do config from server
//                YYLog.info(TAG,
//                        Constant.MEDIACODE_ENCODER + "MediaCodec params:" + mVideoEncoderConfig.mEncodeParameter);
                try {
                    if (!mVideoEncoderConfig.encodeParameterEmpty()) {
                        String itemDelim = ":";
                        String[] tokens = mVideoEncoderConfig.mEncodeParameter.split(itemDelim);
                        String valueDelim = "=";
                        for (int i = 0; i < tokens.length; i++) {
//                            YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec parse:" + tokens[i]);
                            String[] keyValue = tokens[i].split(valueDelim);
                            if (keyValue.length == 2) {
//                                YYLog.info(TAG,
//                                        Constant.MEDIACODE_ENCODER + "MediaCodec param item: name " + keyValue[0] +
//                                                ", value " + keyValue[1]);
                                YYMediaFormatStrategy.setEncoderParams(mMediaFormat, keyValue[0], keyValue[1]);
                            } else {
//                                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec invalid param item:" +
//                                        Arrays.toString(keyValue));
                            }
                        }
                    }
                } catch (Exception e) {
//                    YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec parse error:" + e);
                }

                mStrFormat = mMediaFormat.toString();
//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "before configure, MediaCodec format-----:" + mStrFormat);

                //更新视频comment中的分辨率信息
                String resolution = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH) + "x" +
                        mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                VideoProcessTracer.getInstace().setResolution(resolution);
                mVideoEncoderConfig.setPlaneEncodeSize(mMediaFormat.getInteger(MediaFormat.KEY_WIDTH),
                        mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
            }

            mEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            if (mInputSurface != null) {
                mInputSurface.release();
                mInputSurface = null;
            }
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();
            mInitialized = true;

        } catch (Exception e) {
//            YYLog.error(TAG, "video initEncoder error," + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deInit() {
        synchronized (this) {
            try {

                try {
                    if (mEncoder != null) {
                        mEncoder.stop();
                        mInitialized = false;
//                        YYLog.info(this, Constant.MEDIACODE_ENCODER + " mEncoder.stop");
                    }
                } catch (Throwable e) {
//                    YYLog.error(TAG, "[exception]" + e.getMessage());
                } finally {
                    if (mEncoder != null) {
                        mEncoder.release();
//                        YYLog.info(this, Constant.MEDIACODE_ENCODER + " mEncoder.release");
                    }
                    mEncoder = null;
                }
            } catch (Throwable e) {
//                YYLog.error(TAG, "[exception]" + e.getMessage());
            }
        }
    }

    public static void releaseEncoder() {
        try {
            if (mEncoder != null) {
                mEncoder.stop();
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + " mEncoder.stop");
            }
        } catch (Throwable e) {
//            YYLog.error(TAG, "[exception]" + e.getMessage());
        } finally {
            if (mEncoder != null) {
                mEncoder.release();
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + " mEncoder.release");
            }
            mEncoder = null;
        }

        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMediaFormat != null) {
            mMediaFormat = null;
        }
        mInitialized = false;
    }

    public void drainEncoder(EncodeMediaSample sample, boolean endOfStream) {
//        YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder begin");
        try {
            if (!mInitialized) {
//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "drainEncoder but encoder not started, just return!");
                return;
            }
            //没有创建成功，则不调用drainEncoder, 不然这里也会有问题， 譬如说这次又失败了，无法保证mEncoder的有效性.
            //仍然会崩溃.
//            if (!mInitialized) {
//                init(mWidth, mHeight, mFps, mBps, mListener);
//            }
            final int timeoutMs = 10000;

            if (endOfStream) {
                mCachedPtsList.clear();
                mEncoder.signalEndOfInputStream();
//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "drainEncoder and siganl that end the encoder!!!! ");
            }

            long pts = (sample == null ? 0 : sample.mAndoridPtsNanos);
            mCachedPtsList.add(pts);

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutMs);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (!endOfStream) {
                        break;      // out of while
                    }
//                    YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder INFO_TRY_AGAIN_LATER ");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
//                    YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder INFO_OUTPUT_BUFFERS_CHANGED ");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    if (mListener != null) {
                        mListener.onEncoderFormatChanged(newFormat);
                    }
//                    YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder INFO_OUTPUT_FORMAT_CHANGED ");
                } else if (encoderStatus < 0) {
//                    YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder error!! ");
                } else {
//                    YYLog.debug(this,
//                            Constant.MEDIACODE_ENCODER + "drainEncoder get a frame!! , frameCnt=" + (++mFrameCnt));
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    long realPts = mBufferInfo.presentationTimeUs / 1000; //TODO: why add 3000
//                    YYLog.info(TAG, Constant.MEDIACODE_PTS_SYNC + "video pts after encode:" + realPts);
                    if (realPts < 0) {
//                        YYLog.info(TAG, "error pts get from encode:" + realPts + ",just return");
                        return;
                    }

                    long dts = 0;
                    if (mCachedPtsList.size() > 0) {
                        dts = mCachedPtsList.pop();
                    }

                    mListener.onEncodeOutputBuffer(encodedData, mBufferInfo, dts, realPts, mEncoder.getOutputFormat(),
                            sample);
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;      // out of while
                    }
                }
            }

            if (endOfStream) {
                mListener.onEndOfInputStream();
            }
        } catch (Throwable e) {
//            YYLog.error(TAG, Constant.MEDIACODE_ENCODER + "[exception]" + e.toString());
            e.printStackTrace();
            deInit();
            mListener.onError(mEncodeId.get(), e.toString()); //notify error.
        }

//        YYLog.debug(this, Constant.MEDIACODE_ENCODER + "drainEncoder end");
    }

    /**
     * Change a video encoder's target bitrate on the fly. The value is an
     * Integer object containing the new bitrate in bps.
     */
    public void adjustBitRate(int bitRateInKbps) {
        if (mEncoder == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            try {
                int bitRateInBps = bitRateInKbps * 1024;
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitRateInBps);
                mEncoder.setParameters(bundle);
//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "succeed to adjustBitRate " + bitRateInBps);
            } catch (Throwable t) {
//                YYLog.error(this, Constant.MEDIACODE_ENCODER + "[exception] adjustBitRate. " + t.toString());
            }
        } else {
//            YYLog.error(this, Constant.MEDIACODE_ENCODER + "adjustBitRate is only available on Android API 19+");
        }
    }

    /**
     * Request that the encoder produce a sync frame "soon".
     */
    public void requestSyncFrame() {
        if (mEncoder == null || !mInitialized) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "build version is:" +
// Build.VERSION.SDK_INT + ",can set PARAMETER_KEY_REQUEST_SYNC_FRAME.");
            try {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mEncoder.setParameters(bundle);
                if (mRequestSyncCnt++ % 30 == 0) {
//                    YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "requestSyncFrame, cnt=" + mRequestSyncCnt);
                }
            } catch (Throwable t) {
//                YYLog.error(this, Constant.MEDIACODE_ENCODER + "[exception] requestSyncFrame: " + t.toString());
            }
        }
    }

    public String getFormat() {
        return mStrFormat;
    }
}
