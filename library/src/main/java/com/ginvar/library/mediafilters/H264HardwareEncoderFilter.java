package com.ginvar.library.mediafilters;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.ginvar.library.common.Constant;
import com.ginvar.library.common.SampleType;
import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.gles.utils.GLFrameBuffer;
import com.ginvar.library.mediacodec.VideoEncoderType;
import com.ginvar.library.mediacodec.videocodec.AbstractTextureMoiveEncoder;
import com.ginvar.library.mediacodec.videocodec.HardEncodeListner;
import com.ginvar.library.mediacodec.videocodec.TextureMoiveEncoderAsync;
import com.ginvar.library.model.EncodeMediaSample;
import com.ginvar.library.model.YYMediaSample;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoBFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoIDRFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoIFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoPFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoPFrameSEI;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoPPSFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoSPSFrame;
import static com.ginvar.library.mediacodec.VideoConstant.VideoFrameType.kVideoUnknowFrame;


public class H264HardwareEncoderFilter extends AbstractEncoderFilter implements HardEncodeListner {

    private static final String TAG = H264HardwareEncoderFilter.class.getSimpleName();
    private static final int kMaxRetryCnt = 5;
    private MediaFilterContext mFilterContrext;
    private boolean mCameraFacingFront = false;

    private int mSpsSize = 0;
    private int mPpsSize = 0;

    private AtomicInteger mState = new AtomicInteger(Constant.EncoderState.EncoderStateInit);
    private AtomicLong mCurrentEID = new AtomicLong(-1);

    private AbstractTextureMoiveEncoder mEncoder = null;
    private boolean mHasBFrame = false;

    private int mDebugCount = 0;

    private boolean mRecordEncodeSync = true;

//    private GLFrameBufferAlloc mGLFrameBufferAlloc = null;
//    private EncodeMediaSampleAlloc mEncodeMediaSampleAlloc = null;
    private TextureDrawer mTextureDrawer = null;

    public H264HardwareEncoderFilter(MediaFilterContext filterContext) {
        // TODO Auto-generated constructor stub
        mRecordEncodeSync = true; //GlobalConfig.getInstance().isRecordEncodeSync();
        if (mRecordEncodeSync) {
            mFilterContrext = new MediaFilterContext(filterContext.getAndroidContext(),
                    filterContext.getGLManager().getEglContext());
            mFilterContrext.setRecordConfig(filterContext.getRecordConfig());
            mFilterContrext.setVideoEncodeConfig(filterContext.getVideoEncoderConfig());
        } else {
            mFilterContrext = filterContext;
        }
//
//        mGLFrameBufferAlloc = new GLFrameBufferAlloc();
//        mEncodeMediaSampleAlloc = new EncodeMediaSampleAlloc();

        mEncoder = new TextureMoiveEncoderAsync(mFilterContrext.getGLManager(), this);
        //mEncoder = new TextureMoiveEncoderSync(mFilterContrext.getGlManager(), this);
        //mEncoder = new TextureMovieEncoder();
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264HardwareEncoderFilter.constructor, vconfig-");
    }

    public void deInit() {
        if (mRecordEncodeSync) {
            mFilterContrext.getGLManager().quit();
        }
//        if (mGLFrameBufferAlloc != null) {
//            mGLFrameBufferAlloc.deInit();
//            mGLFrameBufferAlloc = null;
//        }
        if (mTextureDrawer != null) {
            mTextureDrawer.release();
            mTextureDrawer = null;
        }
    }

    public static boolean isAvaible() {
        //return false;
        return android.os.Build.VERSION.SDK_INT >= 18;
    }

    @Override
    public VideoEncoderType getEncoderFilterType() {
        return VideoEncoderType.HARD_ENCODER_H264;
    }

    //wait for implement.
    public boolean startEncode() {
        if (mRecordEncodeSync) {
            if (mFilterContrext.getGLManager().checkSameThread()) {
                doStartEncode();
            } else {
                mFilterContrext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        doStartEncode();
                    }
                });
            }
        } else {
            return doStartEncode();
        }

        return true;
    }

    //wait for implement.
    private boolean doStartEncode() {
        if (Constant.EncoderState.isStart(mState.get())) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "[procedure] startEncode already, so return");
            return true;
        }

        Log.i(TAG, Constant.MEDIACODE_ENCODER + "[procedure] H264HardwareEncoderFilter.startEncode begin");
        setEncodeCfg(mFilterContrext.getVideoEncoderConfig());
        long eid = mEncoder.startEncode(mEncoderConfig, mFilterContrext.getRecordConfig());
        if (eid > 0) {
            mCurrentEID.set(eid);
            mState.set(Constant.EncoderState.EncoderStateStarting);
            Log.i(TAG,
                    Constant.MEDIACODE_ENCODER + "[procedure] H264HardwareEncoderFilter.startEncode succeed end");

            mEncodeParam = "config:" + mEncoderConfig.toString() + ", real:" + mEncoder.getMediaFormat();
//            mFilterContrext.getEncodeParamTipsMgr().setEncoderParam(mEncodeParam);
            return true;
        } else {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "[procedure] H264HardwareEncoderFilter.startEncode fail end");
            return false;
        }
    }

    private void switchEncoder() {
        if (mRecordEncodeSync) {
            if (mFilterContrext.getGLManager().checkSameThread()) {
                doSwitchEncoder();
            } else {
                mFilterContrext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        doSwitchEncoder();
                    }
                });
            }
        } else {
            doSwitchEncoder();
        }
    }

    private void doSwitchEncoder() {
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 switchEncoder begin");
        if (Constant.EncoderState.isStoped(mState.getAndSet(Constant.EncoderState.EncoderStateStoped))) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 switchEncoder: no initialized state, so return");
            return;
        }

        mEncoder.switchEncoder();
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 switchEncoder end");
    }

    public void stopEncode() {
        if (mRecordEncodeSync) {
            if (mFilterContrext.getGLManager().checkSameThread()) {
                doStopEncode();
            } else {
                mFilterContrext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        doStopEncode();
                    }
                });
            }
        } else {
            doStopEncode();
        }
    }

    public void doStopEncode() {
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 stopEncode begin");
        if (Constant.EncoderState.isStoped(mState.getAndSet(Constant.EncoderState.EncoderStateStoped))) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 stopEncode: no initialized state, so return");
            return;
        }

//        mFilterContrext.getEncodeParamTipsMgr().setNoEncoder();
        //mEncoder.handleStopRecording();
        mState.set(Constant.EncoderState.EncoderStateStoped);
        mEncoder.releaseEncoder();
        mCameraFacingFront = false;
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264 stopEncode end");
    }

    public void requestSyncFrame() {
        if (mEncoder != null) {
            mEncoder.requestSyncFrame();
        }
    }

    @Override
    public boolean processMediaSample(final YYMediaSample sample, Object upstream) {
        //需要启动解码器.
        //如果编码器失败.

        int state = mState.get();
        if (Constant.EncoderState.blockStream(state)) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "processMediaSample, encoder is not started or stoped!!");
            return false;
        }

        if (state == Constant.EncoderState.EncoderStateError) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "processMediaSample, encoder is now in error state");
            return false;
        }

        if ((sample.mBufferFlag & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, Constant.MEDIACODE_ENCODER + "processMediaSample: end of stream");
            stopEncode();
            return false;
        }

        if (checkEncodeUpdate(sample.mEncodeWidth, sample.mEncodeHeight,
                mFilterContrext.getVideoEncoderConfig().mLowDelay,
                mFilterContrext.getVideoEncoderConfig().mFrameRate,
                mFilterContrext.getVideoEncoderConfig().mBitRate,
                mFilterContrext.getVideoEncoderConfig().mEncodeParameter)) {
            //reset the encoder....
            //TODO, 硬编码先不清楚怎么平滑.
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "image size changed, so restart the hardware encoder!!");
            switchEncoder();
            startEncode();
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "image size changed, so restart hardeware encoder success!!");
        }

        //mCameraFacingFront默认是false, 前置摄像头编码也需要镜像.
//        if(mCameraFacingFront != sample.mCameraFacingFront) {
//            mEncoder.setEncoderFlipX();
//            mCameraFacingFront = sample.mCameraFacingFront;
//        }

        final EncodeMediaSample encodeMediaSample = new EncodeMediaSample();
        encodeMediaSample.assigne(sample);
        if (mRecordEncodeSync) {
            GLFrameBuffer frameBuffer = new GLFrameBuffer(sample.width,
                    sample.height);
//            final GLFrameBufferAlloc.FrameBufferObject frameBufferObject = mGLFrameBufferAlloc.alloc(sample.mWidth,
//                    sample.mHeight);
            if (mTextureDrawer == null) {
                mTextureDrawer = new TextureDrawer();
            }
            frameBuffer.bind();
            mTextureDrawer.drawTexture(sample.texId, sample.mTransform, sample.width, sample.height,
                    sample.width, sample.height);
            frameBuffer.unbind();
            final int textureId = frameBuffer.getTextureId();
            if (mFilterContrext.getGLManager().checkSameThread()) {
                mEncoder.encodeFrame(encodeMediaSample, textureId);
            } else {
                mFilterContrext.getGLManager().post(new Runnable() {
                    @Override
                    public void run() {
                        mEncoder.encodeFrame(encodeMediaSample, textureId);
                    }
                });
            }
        } else {
            mEncoder.encodeFrame(encodeMediaSample, sample.texId);
        }

        Log.d(TAG, Constant.MEDIACODE_ENCODER + "H264HardwareEncoderFilter.encodeFrame!!");
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onEncodedHeaderAvailableSample(ByteBuffer buffer, MediaCodec.BufferInfo buffInfo, long dtsMs, long ptsMs,
                                               MediaFormat mediaFormat) {
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "H264SurfaceEndoerFilter.onEncodeHeadAvailable");
        if (buffer == null || buffInfo == null) {
            Log.e(TAG, Constant.MEDIACODE_ENCODER +
                    "H264SurfaceEndoerFilter.onEncodeHeadAvailable error, buffer or bufferInfo is null");
            return;
        }

//      if(mFileRecorder != null) {
//          mFileRecorder.processMediaData(buffer, buffInfo.offset, buffInfo.size);
//      }

        if ((buffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (mSpsSize <= 0 || mPpsSize <= 0) {
                Log.e(TAG, Constant.MEDIACODE_ENCODER +
                        "OnEncodedHeaderAvailableSample error, should set setMediaFormatChanged first!");
                return;
            }
            buffer.position(buffInfo.offset);
            if (buffer.remaining() < mSpsSize + mPpsSize) {
                Log.e(TAG, Constant.MEDIACODE_ENCODER + "setVideoCodecConfigBuffer error, buffer length error!");
                return;
            }

            //标准视频格式以 0x00000001 开头，需要根据字节序转化成不同的int值
            boolean isBigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
            int header = isBigEndian ? 0x00000001 : 0x01000000;

            int spsHeader = buffer.getInt();
            //YY视频特殊格式，SPS不需要 0x00000001 header，要手动去掉这四个字节
            int spsSize = mSpsSize;
            if (spsHeader == header) {
                spsSize = spsSize - 4;
            } else {
                buffer.position(buffer.position() - 4);
            }
            byte[] sps = new byte[spsSize];
            buffer.get(sps);
            //pps sample
            YYMediaSample spsSample = new YYMediaSample();
            spsSample.mDataByteBuffer = ByteBuffer.wrap(sps);
            spsSample.mDataByteBuffer.order(ByteOrder.nativeOrder());
            spsSample.mBufferSize = spsSize;
            spsSample.mBufferOffset = 0;
            spsSample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;
            spsSample.mFrameType = kVideoSPSFrame;
            spsSample.mYYPtsMillions = 0;
            spsSample.mDtsMillions = 0;
            deliverToDownStream(spsSample);

//            Log.i(TAG, Constant.MEDIACODE_ENCODER + "OnEncodedHeaderAvailableSample sps:" +
//                    StringUtils.bytesToHexString(sps) + " sps size:" + mSpsSize);

            int ppsSize = mPpsSize;
            int ppsHeader = buffer.getInt();
            //YY视频特殊格式，SPS不需要 0x00000001 header，要手动去掉这四个字节
            if (ppsHeader == header) {
                ppsSize = ppsSize - 4;
            } else {
                buffer.position(buffer.position() - 4);
            }
            byte[] pps = new byte[ppsSize];
            buffer.get(pps);

            YYMediaSample ppsSample = new YYMediaSample();
            ppsSample.mDataByteBuffer = ByteBuffer.wrap(pps);
            ppsSample.mDataByteBuffer.order(ByteOrder.nativeOrder());
            ppsSample.mBufferSize = ppsSize;
            ppsSample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;
            ppsSample.mFrameType = kVideoPPSFrame;
            ppsSample.mYYPtsMillions = 0;
            ppsSample.mDtsMillions = 0;
            deliverToDownStream(ppsSample);

//            Log.i(TAG, Constant.MEDIACODE_ENCODER + "OnEncodedHeaderAvailableSample pps:" +
//                    StringUtils.bytesToHexString(pps) + " pps size:" + mPpsSize);

            if (buffer.remaining() > 0) {
                Log.d(TAG, Constant.MEDIACODE_ENCODER + "H264SurfaceEndoerFilter.OnEncodeDataAvailableSample");
                YYMediaSample sample = new YYMediaSample();

                sample.mDtsMillions = dtsMs;
                sample.mYYPtsMillions = ptsMs;
                sample.mMediaFormat = mediaFormat;
                sample.mFrameFlag = buffInfo.flags;

                sample.width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH); //TODO;
                sample.height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT); //TODO;

                sample.mDataByteBuffer = buffer;
                sample.mBufferOffset = buffer.position();
                sample.mBufferSize = buffer.remaining();
                sample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;

                if ((buffInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    sample.mFrameType = kVideoIDRFrame;
                } else {
                    sample.mDataByteBuffer.position(sample.mBufferOffset);
                    int frameTypeValue = sample.mDataByteBuffer.getInt(4);
                    sample.mDataByteBuffer.position(sample.mBufferOffset);
                    sample.mFrameType = fetchFrameType(frameTypeValue);
                }

                deliverToDownStream(sample);

                handleEncodedFrameStats(sample.mBufferSize, getInputFrameByteSize(), sample.mFrameType);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onEncodedDataAvailableSample(ByteBuffer buffer, MediaCodec.BufferInfo buffInfo, long dtsMs, long ptsMs,
                                             MediaFormat mediaFormat) {
//      if(mFileRecorder != null) {
//          mFileRecorder.processMediaData(buffer, buffInfo.offset, buffInfo.size);
//      }

        Log.d(TAG, Constant.MEDIACODE_ENCODER + "H264SurfaceEndoerFilter.OnEncodeDataAvailableSample");
        YYMediaSample sample = new YYMediaSample();

        sample.mYYPtsMillions = ptsMs;
        sample.mMediaFormat = mediaFormat;
        sample.mFrameFlag = buffInfo.flags;

        sample.width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH); //TODO;
        sample.height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT); //TODO;

        sample.mDataByteBuffer = buffer;
        sample.mBufferOffset = buffInfo.offset;
        sample.mBufferSize = buffInfo.size;
        sample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;

        if ((buffInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            sample.mFrameType = kVideoIDRFrame;
        } else {
            sample.mDataByteBuffer.position(sample.mBufferOffset);
            int frameTypeValue = sample.mDataByteBuffer.get(4);
            sample.mDataByteBuffer.position(sample.mBufferOffset);
            sample.mFrameType = fetchFrameType(frameTypeValue);
        }

        if (sample.mFrameType == kVideoBFrame) {
            if (!mHasBFrame) {
                Log.i(TAG, "onEncodedDataAvailableSample hasBframe:" + mHasBFrame);
                mHasBFrame = true;
            }
        }

        if (mHasBFrame) {
            sample.mDtsMillions = dtsMs == 0 ? sample.mYYPtsMillions - 200 : dtsMs - 200; //保证dts小于pts
        } else {
            sample.mDtsMillions = sample.mYYPtsMillions; //无B帧时，让dts和pts保持一致
        }

//        Log.i(this, "onEncodedDataAvailableSample H264 pts:"+sample.mYYPtsMillions+"
// dts:"+sample.mDtsMillions+" gap:"+(sample.mYYPtsMillions-sample.mDtsMillions)+
// " frameType:"+sample.mFrameType);
        deliverToDownStream(sample);
        handleEncodedFrameStats(sample.mBufferSize, getInputFrameByteSize(), sample.mFrameType);
    }

    private int fetchFrameType(int value) {
        int type = value & 0x1f;
        int frametype = kVideoUnknowFrame;
        switch (type) {
            case 1:
                if (value == 1) {
                    frametype = kVideoBFrame;
                    break;
                }
            case 2:
            case 3:
            case 4:
                frametype = kVideoPFrame;
                break;
            case 5:
                frametype = kVideoIDRFrame;
                break;
            case 9:
                break;
            default:
                frametype = kVideoIDRFrame;
                break;
        }
        return frametype;
    }

    private String getFrameTypeStr(int type) {
        switch (type) {
            case kVideoUnknowFrame:
                return "Unknown";
            case kVideoIFrame:
                return " I ";
            case kVideoPFrame:
                return " P ";
            case kVideoBFrame:
                return " B ";
            case kVideoPFrameSEI:
                return " SEI ";
            case kVideoIDRFrame:
                return " IDR ";
            case kVideoSPSFrame:
                return " SPS ";
            case kVideoPPSFrame:
                return " PPS ";
            default:
                break;
        }
        return " Unknown ";
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onEncodeOutputBuffer(ByteBuffer buffer, MediaCodec.BufferInfo buffInfo, long dtsMs, long ptsMs,
                                     MediaFormat mediaFormat, EncodeMediaSample origSample) {
        YYMediaSample sample = new YYMediaSample();

        sample.mSampleType = SampleType.VIDEO;
        sample.mDataByteBuffer = buffer;
        sample.mBufferOffset = buffInfo.offset;
        sample.mBufferSize = buffInfo.size;
        sample.mBufferFlag = buffInfo.flags;
        sample.mMediaFormat = mediaFormat;
        sample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;

        sample.mDataByteBuffer.position(sample.mBufferOffset);
        if (sample.mDataByteBuffer.remaining() > 4) {
            int frameTypeValue = sample.mDataByteBuffer.get(4);
            sample.mFrameType = fetchFrameType(frameTypeValue);
        } else {
            sample.mFrameType = kVideoUnknowFrame;
        }
        sample.mDataByteBuffer.position(sample.mBufferOffset);

        if ((sample.mBufferFlag & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 0 && mDebugCount++ < 3) {
            Log.d(TAG,
                    "get Frame which not key frame-------------------!!!, sampleFrameTyp:" + sample.mFrameType);
        }


        //Log.i(TAG," sample.mFrameType " + sample.mFrameType + " " + getFrameTypeStr(sample.mFrameType));

        sample.mAndoridPtsNanos = buffInfo.presentationTimeUs * 1000;
        sample.mYYPtsMillions = ptsMs;
        sample.mDtsMillions = dtsMs;
//        sample.mBodyFrameDataArr = (origSample != null ? origSample.mBodyFrameDataArr : null);
//        sample.mFaceFrameDataArr = (origSample != null ? origSample.mFaceFrameDataArr : null);
        deliverToDownStream(sample);
//        sample.decRef();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onEncoderFormatChanged(MediaFormat mediaFormat) {

        YYMediaSample sample = new YYMediaSample();
        sample.mSampleType = SampleType.VIDEO;
        sample.mMediaFormat = mediaFormat;
        sample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;
        deliverToDownStream(sample);
//        sample.decRef();

        handleEncodeResolution(mediaFormat.getInteger(MediaFormat.KEY_WIDTH),
                mediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
    }

    @Override
    public void onEndOfInputStream() {
        YYMediaSample sample = new YYMediaSample();
        sample.mSampleType = SampleType.VIDEO;
        sample.mBufferFlag |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        sample.mEncoderType = VideoEncoderType.HARD_ENCODER_H264;
        deliverToDownStream(sample);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void __onEncoderFormatChanged(MediaFormat mediaFormat) {
        //set the pps, sps..
        if (mediaFormat == null) {
//            Log.e(this, Constant.MEDIACODE_ENCODER + "setMediaFormatChanged error, format null!");
            return;
        }
        ByteBuffer sps = mediaFormat.getByteBuffer("csd-0");
        ByteBuffer pps = mediaFormat.getByteBuffer("csd-1");
        if (sps == null || pps == null) {
//            Log.e(this,
//                    Constant.MEDIACODE_ENCODER + "setMediaFormatChanged error, csd-0:" + sps + ", csd-1:" + pps);
            return;
        }
        mSpsSize = sps.limit() - sps.position();
        mPpsSize = pps.limit() - pps.position();
        Log.i(TAG,
                Constant.MEDIACODE_ENCODER + "setMediaFormatChanged spsSize:" + mSpsSize + ", ppsSize:" + mPpsSize);

        handleEncodeResolution(mediaFormat.getInteger(MediaFormat.KEY_WIDTH),
                mediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
    }

    public int getInputFrameByteSize() {
        return mEncoderConfig.getEncodeHeight() * mEncoderConfig.getEncodeWidth() * 4;
    }

    public void _OnError(long eid, String errMsg) {
        if (mCurrentEID.get() != eid) {
            return;
        }
        if (Constant.EncoderState.isStoped(mState.get())) {
            //Log.i(this, Constant.MEDIACODE_ENCODER + "encoder error, but encoder is stoped, so just return!!");
            return;
        }

        mRetryCnt++;
        stopEncode();
        mState.set(Constant.EncoderState.EncoderStateError);
        if (!startEncode()) {
            //retry error, try to switch the encoder.
            mRetryCnt++;
            stopEncode();
        }
    }

    @Override
    public void onError(final long eid, final String errMsg) {
        //may not run the gl thread, as encoder support multithread.
        //encoder error.
        Log.e(TAG, Constant.MEDIACODE_ENCODER + "hardware encoder error: " + (errMsg == null ? "null" : errMsg) +
                ", retryCnt=" + mRetryCnt++);
        if (eid == -1 || eid != mCurrentEID.get()) {
            Log.i(TAG, Constant.MEDIACODE_ENCODER + "encoder error, but it is out of date!!");
            return;
        }

        //如果编码器是单线程同步的， 而非异步，可以直接在这里尝试重启编码器.
        if (mFilterContrext.getGLManager().checkSameThread()) {
            _OnError(eid, errMsg);
        } else {
            mFilterContrext.getGLManager().post(new Runnable() {
                @Override
                public void run() {
                    _OnError(eid, errMsg);
                }
            });
        }
    }

    //设置动态码率.,
    public void adjustBitRate(final int bitRateInKbps) {
        Log.i(TAG, Constant.MEDIACODE_ENCODER + "[tracer] adjust bitrate: " + bitRateInKbps);
        if (mFilterContrext.getGLManager().checkSameThread()) {
            if (Constant.EncoderState.isStart(mState.get())) {
                mEncoder.adjustBitRate(bitRateInKbps);
            }
        } else {
            mFilterContrext.getGLManager().post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (Constant.EncoderState.isStart(mState.get())) {
                        mEncoder.adjustBitRate(bitRateInKbps);
                    }
                }
            });
        }
    }
}
