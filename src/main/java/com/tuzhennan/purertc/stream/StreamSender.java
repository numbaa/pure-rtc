package com.tuzhennan.purertc.stream;

import com.tuzhennan.purertc.model.RtcpPacket;
import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.model.VideoFrame;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import com.tuzhennan.purertc.video.VideoEncoder;

import java.util.ArrayList;
import java.util.List;

public class StreamSender {

    private final int MaxPayloadLength = 1200;

    private final Clock clock;

    private final NetChannel.LeftEndPoint channel;

    private final VideoEncoder videoEncoder;

    private final VirtualThread virtualThread;

    public StreamSender(Clock clock, NetChannel.LeftEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.videoEncoder = new VideoEncoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::captureAndEncodeOneFrame);
        this.virtualThread.postTask(this::recvRtcpFeedback);
    }

    public void step() {
        virtualThread.step();
    }

    private void captureAndEncodeOneFrame() {
        VideoFrame encodedFrame = videoEncoder.captureAndEncodeOneFrame();
        List<RtpPacket> packets = packetizeToRtpPacket(encodedFrame);
        sendToNetwork(packets);

        //TODO: 根据FPS以及其他因素计算出下一次抓屏时间
        long nextCaptureTimeMS = 16;
        this.virtualThread.postDelayedTask(nextCaptureTimeMS, this::captureAndEncodeOneFrame);
    }

    private void sendToNetwork(List<RtpPacket> packets) {
        packets.forEach(channel::send);
    }

    private List<RtpPacket> packetizeToRtpPacket(VideoFrame frame) {
        return new ArrayList<>();
    }

    private void recvRtcpFeedback() {
        //为了简化代码，采用轮询的方式收包，收不到包sleep 1毫秒再尝试1次
        RtcpPacket packet = channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtcpFeedback);
        } else {
            onRecvRtcp(packet);
            this.virtualThread.postTask(this::recvRtcpFeedback);
        }
    }

    private void onRecvRtcp(RtcpPacket packet) {

    }
}
