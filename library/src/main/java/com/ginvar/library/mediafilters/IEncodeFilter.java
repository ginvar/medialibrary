package com.ginvar.library.mediafilters;

import com.ginvar.library.mediacodec.VideoEncoderType;

public class IEncodeFilter extends AbstractYYMediaFilter {
    public VideoEncoderType getEncoderFilterType() {
        return VideoEncoderType.HARD_ENCODER_H264;
    }

    public boolean startEncode() {
        return false;
    }

    public void stopEncode() {
    }

    public boolean init() {
        return false;
    }

    public void deInit() {
    }

    public void setEncoderListener(IEncoderListener listener) {
    }

    public void requestSyncFrame() {
    }
}
