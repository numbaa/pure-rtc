package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.model.VideoFrame;

public class VideoEncoder {

    private int encodeRate;

    private int fps;

    public VideoFrame captureAndEncodeOneFrame() {
        return new VideoFrame();
    }

    // 设置码率
    public void setEncodeRate(int encodeRate) {
        this.encodeRate = encodeRate;
    }

    // 设置帧率
    public void setFPS(int fps) {
        this.fps = fps;
    }
}
