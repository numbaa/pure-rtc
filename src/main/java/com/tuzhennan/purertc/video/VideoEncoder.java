package com.tuzhennan.purertc.video;

import com.tuzhennan.purertc.model.VideoFrame;

// 模型：
// 0.IPPP
// 1.码率只用来计算P帧
// 2.I帧默认100KBytes
// 3.I帧只在首帧或者主动请求时发
// 4. 1Mbps = 1000Kbps（不是1024）
public class VideoEncoder {

    private int encodeRateBitPerSecond;

    private int fps;

    private boolean nextFrameIsKeyFrame = true;

    private long sequenceNumber = 1;

    private final int kDefaultIFrameBytes = 100 * 1000;

    public VideoEncoder(int bps, int fps) {
        this.encodeRateBitPerSecond = bps;
        this.fps = fps;
    }

    public VideoFrame captureAndEncodeOneFrame() {
        if (nextFrameIsKeyFrame) {
            nextFrameIsKeyFrame = false;
            VideoFrame keyFrame = new VideoFrame();
            keyFrame.isKeyFrame = true;
            keyFrame.frameID = sequenceNumber++;
            keyFrame.sizeBytes = kDefaultIFrameBytes;
            return keyFrame;
        } else {
            VideoFrame pFrame = new VideoFrame();
            pFrame.isKeyFrame = false;
            pFrame.frameID = sequenceNumber++;
            pFrame.sizeBytes = calculateFrameSize();
            return pFrame;
        }
    }

    // 设置码率
    public void setEncodeRateBitPerSecond(int encodeRate) {
        this.encodeRateBitPerSecond = encodeRate;
    }

    // 设置帧率
    public void setFPS(int fps) {
        this.fps = fps;
    }

    public long nextCaptureTime() {
        return 1000 / fps;
    }

    public void requestKeyFrame() {
        nextFrameIsKeyFrame = true;
    }

    private int calculateFrameSize() {
        return encodeRateBitPerSecond / 8 / fps;
    }
}
