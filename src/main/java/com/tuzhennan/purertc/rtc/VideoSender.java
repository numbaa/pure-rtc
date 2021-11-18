package com.tuzhennan.purertc.rtc;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;

import java.util.ArrayList;
import java.util.List;

class VideoSender {

    private final Clock clock;

    private final NetChannel.ChannelEndPoint channel;

    private final VideoEncoder videoEncoder;

    private final VirtualThread virtualThread;

    public VideoSender(Clock clock, NetChannel.ChannelEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.videoEncoder = new VideoEncoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(new VirtualThread.Task() {
            @Override
            public void run() {
                captureAndEncodeOneFrame();
            }
        });
    }

    public void step() {
        virtualThread.step();
    }

    private void captureAndEncodeOneFrame() {
        VideoFrame encodedFrame = videoEncoder.captureAndEncodeOneFrame();
        List<RtpPacket> packets = splitToRtpPackets(encodedFrame);
        sendToNetwork(packets);

        //TODO: 根据FPS以及其他因素计算出下一次抓屏时间
        long nextCaptureTimeMS = 16;
        this.virtualThread.postDelayedTask(nextCaptureTimeMS, new VirtualThread.Task() {
            @Override
            public void run() {
                captureAndEncodeOneFrame();
            }
        });
    }

    private void sendToNetwork(List<RtpPacket> packets) {
        packets.forEach(channel::send);
    }

    private List<RtpPacket> splitToRtpPackets(VideoFrame frame) {
        return new ArrayList<>();
    }
}
