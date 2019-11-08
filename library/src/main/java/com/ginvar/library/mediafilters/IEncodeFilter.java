package com.ginvar.library.mediafilters;

import com.ginvar.library.mediacodec.VideoEncoderType;

public class IEncodeFilter extends AbstractYYMediaFilter {
    public VideoEncoderType getEncoderFilterType() {
        return VideoEncoderType.HARD_ENCODER_H264;
    }
}
