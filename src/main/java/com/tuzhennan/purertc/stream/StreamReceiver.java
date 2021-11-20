package com.tuzhennan.purertc.stream;

import com.tuzhennan.purertc.model.RtpPacket;
import com.tuzhennan.purertc.utils.Clock;
import com.tuzhennan.purertc.utils.VirtualThread;
import com.tuzhennan.purertc.video.VideoDecoder;

public class StreamReceiver {

    private final Clock clock;

    private final NetChannel.RightEndPoint channel;

    private final VideoDecoder videoDecoder;

    private final VirtualThread virtualThread;

    public StreamReceiver(Clock clock, NetChannel.RightEndPoint channel) {
        this.clock = clock;
        this.channel = channel;
        this.videoDecoder = new VideoDecoder();
        this.virtualThread = new VirtualThread(clock);
        this.virtualThread.postTask(this::recvRtpPacket);
    }

    public void step() {
        this.virtualThread.step();
    }

    private void recvRtpPacket() {
        //为了简化代码，采用轮询的方式收包，收不到包sleep 1毫秒再尝试1次
        RtpPacket packet = channel.recv();
        if (packet == null) {
            this.virtualThread.postDelayedTask(1, this::recvRtpPacket);
        } else {
            onRecvRtpPacket(packet);
        }
    }

    private void onRecvRtpPacket(RtpPacket packet) {

    }
}
