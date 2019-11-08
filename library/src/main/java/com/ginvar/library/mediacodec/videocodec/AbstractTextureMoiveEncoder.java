package com.ginvar.library.mediacodec.videocodec;

import android.annotation.TargetApi;
import android.os.Build;

import com.ginvar.library.drawer.TextureDrawer;
import com.ginvar.library.gles.EglCore;
import com.ginvar.library.gles.GLManager;
import com.ginvar.library.gles.WindowSurface;
import com.ginvar.library.mediacodec.VideoEncoderConfig;
import com.ginvar.library.mediacodec.VideoEncoderType;
import com.ginvar.library.mediarecord.RecordConfig;
import com.ginvar.library.model.EncodeMediaSample;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Administrator on 2017/1/3.
 */

//对google的grafric项目中的texture moive encoder的封装.
public class AbstractTextureMoiveEncoder {
    protected EglCore mEglCore = null;
    protected HardEncodeListner mHardEncoderListener = null;
    protected HardSurfaceEncoder mVideoEncoderImpl = null;

    protected static AtomicLong sEncodeIds = new AtomicLong(1);

    // ----- accessed exclusively by encoder thread -----
    protected WindowSurface mInputWindowSurface = null;
    protected TextureDrawer mTextureRenderer = null;
    VideoEncoderConfig mVideoEncoderConfig = null;

    GLManager mGlManager = null;

    //保留上一帧的输入/裁剪长宽
    protected int mInputWidth = 0;
    protected int mInputHeight = 0;
    protected int mClipWidth = 0;
    protected int mClipHeight = 0;

    private int mEncodeFrameCnt = 0;

    private RecordConfig mRecordConfig;

    //new the object in gl thread.
    public AbstractTextureMoiveEncoder(GLManager glMgr, HardEncodeListner listener) {
        mGlManager = glMgr;
        mEglCore = mGlManager.getEglCore();
        mHardEncoderListener = listener;
    }

    public long startEncode(VideoEncoderConfig config, RecordConfig recordConfig) {
        long eid = sEncodeIds.incrementAndGet();
        mVideoEncoderConfig = config;
        mRecordConfig = recordConfig;
        try {
            if (mVideoEncoderConfig.mEncodeType == VideoEncoderType.HARD_ENCODER_H264) {
                mVideoEncoderImpl = new H264SurfaceEncoder(eid);
            } else {
                mVideoEncoderImpl = new H265SurfaceEncoder(eid);
            }

            if (mVideoEncoderImpl.init(config, mHardEncoderListener)) {
                mTextureRenderer = TextureDrawer.create();
//                setEncoderFlipY();

                if (mInputWindowSurface != null) {
                    mInputWindowSurface.release();
                    mInputWindowSurface = null;
                }

                mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoderImpl.getInputSurface(), true);

//                GLErrorUtils.checkGlError("AbstractTextureMoiveEncoder.startEncode end");

//                YYLog.info(this, Constant.MEDIACODE_ENCODER + "start video encoder success, eid=" + eid);
                return eid;
            } else {
//                YYLog.error(this, Constant.MEDIACODE_ENCODER + "videocodec init fail");
                releaseEncoder();
//                GLErrorUtils.checkGlError("AbstractTextureMoiveEncoder.startEncode end");
                return -1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            releaseEncoder();

//            GLErrorUtils.checkGlError("AbstractTextureMoiveEncoder.startEncode end");

//            YYLog.error(this, Constant.MEDIACODE_ENCODER + "start video encoder fail: " + t.toString());
            return -1;
        }
    }

    /**
     * 快速丢帧特殊处理策略：2倍速每3帧丢1帧，3倍速每2帧丢1帧，4倍速每3帧丢2帧
     * 由于当前录制摄像头采集帧率默认设置成20，这样特殊处理后2-4倍速帧率基本在40帧左右
     *
     * @param recordSpeed 录制速度
     */
    private boolean needDropFrame(int recordSpeed) {
        switch (recordSpeed) {
            case 1:
                break;
            case 2:
                if ((mEncodeFrameCnt + 1) % 3 == 0) {
                    return true;
                }
                break;
            case 3:
                if (mEncodeFrameCnt % 2 != 0) {
                    return true;
                }
                break;
            case 4:
                if (mEncodeFrameCnt % 3 != 0) {
                    return true;
                }
                break;
            default:
                if (mEncodeFrameCnt % recordSpeed != 0) {
                    return true;
                }
                break;

        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void encodeFrame(EncodeMediaSample sample, int renderTexture) {
        //录制速度超过1.0时，需要均匀丢视频帧
        mEncodeFrameCnt++;
        float recordSpeed = mRecordConfig.getRecordSpeed();
        /*if(recordSpeed > 1.0f && mEncodeFrameCnt % (int)recordSpeed != 0) {
//            YYLog.info(this,"drop video frame cnt" + mEncodeFrameCnt);
            return;
        }*/
        if (recordSpeed > 1.0f && needDropFrame((int) recordSpeed)) {
//            YYLog.info(this, "drop video frame cnt" + mEncodeFrameCnt);
            return;
        }

        long ptsNs = sample.mAndoridPtsNanos;
//        YYLog.debug(this, Constant.MEDIACODE_ENCODER + "[pts]video pts before encode:" + ptsNs / 1000000);

        mInputWindowSurface.makeCurrent();
        mTextureRenderer.drawTexture(renderTexture, sample.mTransform, sample.mWidth, sample.mHeight,
                mVideoEncoderConfig.getPlaneEncodeWidth(), mVideoEncoderConfig.getPlaneEncodeHeight());
        mInputWindowSurface.setPresentationTime(ptsNs); //tickCount * 1000 * 1000);//timestampNanos);

        //如果gop size设置小于2，表示需要全I帧编码（部分机型设为0有问题，故设为1，也认为是全I帧编码的折中策略），需要request sync frame
        if (mVideoEncoderConfig.mGopSize == 0 ||
                (mVideoEncoderConfig.mGopSize != 0 && (mEncodeFrameCnt % mVideoEncoderConfig.mGopSize) == 0)) {
            requestSyncFrame();
        }

        mInputWindowSurface.swapBuffers();
        onEncodedFrameFinished(sample);
    }

    private boolean checkClipRatioChanged(int inputWidth, int inputHeight, int clipWidth, int clipHeight) {
        boolean ret = false;
        if (mInputWidth != inputWidth || mInputHeight != inputHeight || mClipWidth != clipWidth ||
                mClipHeight != clipHeight) {
//            YYLog.info(this, Constant.MEDIACODE_ENCODER + "inputWidth:" + inputWidth + " inputHeight:" + inputHeight +
//                    " clipWidth:" + clipWidth + " clipHeight:" + clipHeight);
            mInputWidth = inputWidth;
            mInputHeight = inputHeight;
            mClipWidth = clipWidth;
            mClipHeight = clipHeight;
            ret = true;
        }
        return ret;
    }

    public void switchEncoder() {

    }

    public void stopEncoder() {
        //目前只会调用releaseEncoder
    }

    public void releaseEncoder() {
        if (mVideoEncoderImpl != null) {
            mVideoEncoderImpl.drainEncoder(null, true);
            mVideoEncoderImpl.deInit();
            mVideoEncoderImpl = null;
        }

        if (mTextureRenderer != null) {
            mTextureRenderer = null;
        }

        if (mInputWindowSurface != null) {
            mGlManager.resetContext();
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }

        mInputWidth = 0;
        mInputHeight = 0;
        mClipWidth = 0;
        mClipHeight = 0;
    }

//    public void setEncoderFlipX() {
//        if (mTextureRenderer != null) {
//            YYLog.info(this, Constant.MEDIACODE_ENCODER + "encoder setEncoderFlipX");
//            mTextureRenderer.setFlipX(true);
//        }
//    }

//    public void setEncoderFlipY() {
//        if (mTextureRenderer != null) {
//            YYLog.info(this, Constant.MEDIACODE_ENCODER + "encoder setEncoderFlipY");
//            mTextureRenderer.setFlipY(true);
//        }
//    }

    public void adjustBitRate(int bitRateInKbps) {
        if (mVideoEncoderImpl != null) {
            mVideoEncoderImpl.adjustBitRate(bitRateInKbps);
        }
    }

    public void requestSyncFrame() {
        if (mVideoEncoderImpl != null) {
            mVideoEncoderImpl.requestSyncFrame();
        }
    }

    public void onEncodedFrameFinished(EncodeMediaSample sample) {
        if (mVideoEncoderImpl != null) {
            mVideoEncoderImpl.drainEncoder(sample, false); //tickCount * 1000 * 1000);//timestampNanos)
        }
    }

    public String getMediaFormat() {
        if (mVideoEncoderImpl != null) {
            return mVideoEncoderImpl.getFormat();
        }

        return "";
    }
}
