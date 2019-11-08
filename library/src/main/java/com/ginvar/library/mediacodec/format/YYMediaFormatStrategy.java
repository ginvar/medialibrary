package com.ginvar.library.mediacodec.format;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

//import com.ycloud.common.Constant;
//import com.ycloud.mediacodec.VideoEncoderConfig;
//import com.ycloud.mediacodec.VideoEncoderType;
//import com.ycloud.toolbox.log.YYLog;

import com.ginvar.library.mediacodec.VideoEncoderConfig;
import com.ginvar.library.mediacodec.VideoEncoderType;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by dzhj on 17/5/17.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class YYMediaFormatStrategy {

    private static final String TAG = YYMediaFormatStrategy.class.getSimpleName();

    public static MediaFormat getVideoFormatForEncoder(VideoEncoderConfig config, String mimeType) {
        MediaFormat mediaFormat = null;
        MediaCodec encoder = null;
        try {
//            YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "[procedure] encoder init, configure： " + config.toString());
            int width = config.getEncodeWidth();
            int height = config.getEncodeHeight();
            mediaFormat = MediaFormat.createVideoFormat(mimeType, width, height);

            encoder = MediaCodec.createEncoderByType(mimeType);
            MediaCodecInfo.CodecCapabilities caps = encoder.getCodecInfo().getCapabilitiesForType(mimeType);
            MediaCodecInfo.CodecProfileLevel[] pr = caps.profileLevels;

            int level = 0;
            int profile = 0;
            int baseLineLevel = 0;

            if (mimeType.equals("video/hevc")) {
                for (MediaCodecInfo.CodecProfileLevel aPr : pr) {
                    if (profile == aPr.profile && level <= aPr.level) {
                        profile = aPr.profile;
                        level = aPr.level;
                    }
                }
            } else if (!config.mLowDelay &&
                    config.mEncodeType == VideoEncoderType.HARD_ENCODER_H264) {
                //find baseline level
                for (MediaCodecInfo.CodecProfileLevel aPr : pr) {
                    if (aPr.profile <= MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444) {
                        if (profile < aPr.profile) {
                            profile = aPr.profile;
                            level = aPr.level;
                        } else if (profile == aPr.profile && level < aPr.level) {
                            profile = aPr.profile;
                            level = aPr.level;
                        }
                    }

                    if (aPr.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline) {
                        if (baseLineLevel < aPr.level) {
                            baseLineLevel = aPr.level;
                        }
                    }
                }
                if (config.mEnableProfile && profile > 0) {
                    // 即使能用high,兼容性也是问题,担心有的机器直接崩溃了
                    level = level > MediaCodecInfo.CodecProfileLevel.AVCLevel42 ?
                            MediaCodecInfo.CodecProfileLevel.AVCLevel42 : level;     // avoid crash
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile);
                    }

//                    YYLog.info(TAG, "mediaFormat.level:" + level);
                    mediaFormat.setInteger("level", level);
                } else {
                    baseLineLevel = baseLineLevel > MediaCodecInfo.CodecProfileLevel.AVCLevel42 ?
                            MediaCodecInfo.CodecProfileLevel.AVCLevel42 : baseLineLevel;     // avoid crash

                    mediaFormat
                            .setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//                    YYLog.info(TAG, "mediaFormat.Baseline level:" + baseLineLevel);
                    mediaFormat.setInteger("level", baseLineLevel);
                }
            }
            mediaFormat
                    .setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, config.mBitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, config.mFrameRate);

            if (config.mGopSize == 0) {
                //部分机型设为0有问题，故设为1，也认为是全I帧编码的折中策略
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                } else {
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.mGopSize);
                }
            } else {
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.mGopSize);
            }

            // 0:BITRATE_MODE_CQ, 1:BITRATE_MODE_VBR, 2:BITRATE_MODE_CBR
            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, config.mBitRateModel);

            //do config from server
//            YYLog.info(TAG,
//                    Constant.MEDIACODE_ENCODER + "MediaCodec params:" + config.mEncodeParameter);
            try {
                if (!config.encodeParameterEmpty()) {
                    String itemDelim = ":";
                    String[] tokens = config.mEncodeParameter.split(itemDelim);
                    String valueDelim = "=";
                    for (int i = 0; i < tokens.length; i++) {
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec parse:" + tokens[i]);
                        String[] keyValue = tokens[i].split(valueDelim);
                        if (keyValue.length == 2) {
//                            YYLog.info(TAG,
//                                    Constant.MEDIACODE_ENCODER + "MediaCodec param item: name " + keyValue[0] +
//                                            ", value " + keyValue[1]);
                            YYMediaFormatStrategy.setEncoderParams(mediaFormat, keyValue[0], keyValue[1]);
                        } else {
//                            YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec invalid param item:" +
//                                    Arrays.toString(keyValue));
                        }
                    }
                }
            } catch (Exception e) {
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec parse error:" + e);
            }

            String strFormat = mediaFormat.toString();
//            YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "before configure, MediaCodec format-----:" + strFormat);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (encoder != null) {
                encoder.release();
            }
        }

        return mediaFormat;
    }

    public static void setEncoderParams(MediaFormat format, String name, String value) {
        switch (name) {
            case "bitrate-mode":
                /*
                 * Constant quality mode *
                public static final int BITRATE_MODE_CQ = 0;
                 * Variable bitrate mode *
                public static final int BITRATE_MODE_VBR = 1;
                 * Constant bitrate mode *
                public static final int BITRATE_MODE_CBR = 2;
                 */
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set bitrate-mode: " + value);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    format.setInteger(MediaFormat.KEY_BITRATE_MODE, Integer.parseInt(value));
                }
                break;
            case "color-range":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set color-range: " + value);
                //format.setInteger(MediaFormat.KEY_COLOR_RANGE, Integer.parseInt(value));
                // use this line when use Android API level 24
                format.setInteger("color-range", Integer.parseInt(value));
                break;
            case "color-standard":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set color-standard: " + value);
                //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, Integer.parseInt(value));
                // use this line when use Android API level 24
                format.setInteger("color-standard", Integer.parseInt(value));
                break;
            case "color-transfer":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set color-transfer: " + value);
                //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, Integer.parseInt(value));
                // use this line when use Android API level 24
                format.setInteger("color-transfer", Integer.parseInt(value));
                break;
            case "complexity":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set complexity: " + value);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    format.setInteger(MediaFormat.KEY_COMPLEXITY, Integer.parseInt(value));
                }
                break;
            case "gop_duration":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set i-frame-interval: " + value);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Integer.parseInt(value));
                break;
            case "intra-refresh-period":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set i-frame-interval: " + value);
                //format.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, Integer.parseInt(value));
                // use this line when use Android API level 24
                format.setInteger("intra-refresh-period", Integer.parseInt(value));
                break;
            case "profile":
                switch (value.toLowerCase()) {
                    case "baseline":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: Baseline");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                        }
                        break;
                    case "main":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: Main");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
                        }
                        break;
                    case "extended":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: Extended");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileExtended);
                        }
                        break;
                    case "high":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: High");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                        }
                        break;
                    case "high10":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: High10");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10);
                        }
                        break;
                    case "high422":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: High422");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422);
                        }
                        break;
                    case "high444":
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: High444");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            format.setInteger(MediaFormat.KEY_PROFILE,
                                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444);
                        }
                        break;
                    default:
//                        YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set profile: error keyword");
                        break;
                }
                break;
            case "level":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set level: " + value);
                //format.setInteger(MediaFormat.KEY_LEVEL, Integer.parseInt(value));
                // use this line when use Android API level 23
                format.setInteger("level", Integer.parseInt(value));
                break;
            case "priority":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set priority: " + value);
                //format.setInteger(MediaFormat.KEY_PRIORITY, Integer.parseInt(value));
                // use this line when use Android API level 23
                format.setInteger("priority", Integer.parseInt(value));
                break;
            case "repeat-previous-frame-after":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set repeat-previous-frame-after: " + value);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, Integer.parseInt(value));
                }
                break;

            case "width":
//                YYLog.info(TAG, Constant.MEDIACODE_ENCODER + "MediaCodec set width: " + value);
                //format.setInteger(MediaFormat.KEY_WIDTH,Integer.parseInt(value));
                break;
            case "pack_16":
                int width = format.getInteger(MediaFormat.KEY_WIDTH);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                if (Integer.parseInt(value) == 1 && width > 0 && height > 0) {
                    int wDivide = width / 16;
                    int wRemainder = width % 16;
                    if (wRemainder > 0) {
                        format.setInteger(MediaFormat.KEY_WIDTH, wDivide * 16 + 16);
                    }
                    int hDivide = height / 16;
                    int hRemainder = height % 16;
                    if (hRemainder > 0) {
                        format.setInteger(MediaFormat.KEY_HEIGHT, hDivide * 16 + 16);
                    }
                }
                break;
            default:
//                YYLog.info(TAG, "unsupported params:" + name);
                break;
        }
    }
}